/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bee.Fail;
import bee.Platform;
import bee.Task;
import bee.UserInterface;
import bee.api.Command;
import bee.api.Comment;
import bee.api.Project;
import bee.api.VCS;
import bee.util.ConventionalCommit;
import bee.util.Git;
import bee.util.GithubAPI;
import bee.util.SemanticVersion;
import bee.util.SemanticVersion.Bump;
import psychopath.Directory;
import psychopath.File;

/**
 * Releases the project. The next version is computed from the conventional commits since the latest
 * tag, the version file is updated and pushed, and then the release workflow is dispatched to build
 * the tagged artifacts, create the GitHub Release and publish them. The tag is created by the
 * workflow, once the build has succeeded, so a successful release always has its tag.
 */
public interface Release extends Task<Release.Config> {

    /** The option label of the arbitrary version input. */
    String CUSTOM = "Custom version ...";

    /** The option label of the abort. */
    String ABORT = "Abort";

    /** The repository dispatch event type which starts the release workflow. */
    String EVENT = "release";

    /** The GitHub Actions event name of the dispatched run. */
    String RUN_EVENT = "repository_dispatch";

    /** The GitHub Actions event name of the build run which the release commit starts. */
    String BUILD_EVENT = "push";

    /** The maximum number of the commits to show in the release plan. */
    int MAX_COMMITS = 20;

    /** The interval (ms) between the workflow run polls. */
    long POLL_INTERVAL = 1000;

    /** The maximum time (ms) to wait for the workflow run to be discovered. */
    long DISCOVER_TIMEOUT = 60 * 1000;

    /**
     * The configuration of the release task.
     */
    class Config {

        /** Preview the release plan without changing anything. */
        @Comment("Preview the release plan without changing anything.")
        public boolean dryRun;

        /** Force the release type instead of computing it from the commits. */
        @Comment("Force the release type. One of major, minor or patch.")
        public String releaseAs;

        /** Set the next version explicitly. */
        @Comment("Set the next version explicitly instead of computing it.")
        public String version;

        /** Whether the release commit is pushed to the remote. */
        @Comment("Push the release commit to the remote.")
        public boolean push = true;

        /** Whether the release workflow is watched until it completes. */
        @Comment("Watch the release workflow until it completes.")
        public boolean watch = true;

        /** Whether the workflow log is shown after the run completes. */
        @Comment("Show the workflow log after the run completes.")
        public boolean showLog = true;

        /** The branch to release from. The current branch is used when it is empty. */
        @Comment("The branch to release from. The current branch is used when it is empty.")
        public String branch;

        /** Whether untracked files are included without asking when the pending changes are committed. */
        @Comment("Include untracked files without asking when the pending changes are committed.")
        public boolean includeUntracked;
    }

    /**
     * Release the project.
     */
    @Command(defaults = true, value = "Release the project by bumping the version and dispatching the release workflow.")
    default void release() {
        Project project = project();
        Directory root = project.getRoot();
        Config conf = config();

        // 1. Verify the preconditions.
        if (!Git.isAvailable()) {
            throw new Fail("The git command is not found.").solve("Install Git and make it available on your PATH.");
        }
        VCS vcs = project.getVersionControlSystem();
        if (vcs == null || !vcs.name().equals("github")) {
            throw new Fail("The release requires a GitHub repository.");
        }

        Git git = Git.at(root);
        String branch = conf.branch == null || conf.branch.isBlank() ? git.branch() : conf.branch;

        if (git.hasChanges(true)) {
            commitPendingChanges(git, conf, branch);
        }
        git.fetch();
        if (!git.isSynced()) {
            synchronize(git, conf, branch);
        }

        File versionFile = root.file("version.txt");
        if (versionFile.isAbsent()) {
            throw new Fail("The version file [version.txt] is not found.").solve("Run the [ci] task to generate it.");
        }
        SemanticVersion current = new SemanticVersion(versionFile.text());
        String repository = vcs.owner + "/" + vcs.repo;
        String latest = git.latestVersionTag();

        // A tag without a GitHub Release, or a version which is ahead of the latest tag without a
        // tag, means that the previous release did not finish. Finish it here instead of starting a
        // new one, otherwise the version number is skipped.
        if (latest != null && !GithubAPI.hasRelease(repository, latest)) {
            ui().warn("The previous release [", latest, "] has a tag but no GitHub Release. Finishing it now.");
            publishVersion(latest);
            return;
        }
        if (latest != null && current.compareTo(new SemanticVersion(latest)) > 0
                && !git.hasTag(current.toString())
                && !GithubAPI.hasRelease(repository, current.toString())) {
            ui().warn("The version [", current, "] has not been released yet. Finishing it now.");
            publishVersion(current.toString());
            return;
        }

        // 2. Collect the commits since the latest tag.
        String previous = latest;
        List<ConventionalCommit> commits = git.conventionalCommits(previous);

        // 3. Show the context.
        ui().info("Release plan");
        ui().info("  Repository \t", vcs.uri());
        ui().info("  Branch     \t", branch);
        ui().info("  Previous   \t", previous == null ? "(none)" : previous);
        ui().info("  Current    \t", current);
        ui().info("  Commits    \t", commits.size());
        for (int i = 0; i < commits.size() && i < MAX_COMMITS; i++) {
            ui().info("    ", commits.get(i));
        }
        if (commits.size() > MAX_COMMITS) {
            ui().info("    ... and ", commits.size() - MAX_COMMITS, " more commits.");
        }

        // 4. Determine the version which the commits suggest.
        Bump bump = determineBump(current, commits);

        // 5. Resolve the version to release. The suggestion can be overridden by the user.
        SemanticVersion next;
        if (conf.version != null && !conf.version.isBlank()) {
            if (conf.version.indexOf('-') != -1 || conf.version.indexOf('+') != -1) {
                throw new Fail("A pre-release version is not supported : " + conf.version);
            }
            next = new SemanticVersion(conf.version);
        } else if (conf.releaseAs != null && !conf.releaseAs.isBlank()) {
            bump = parseBump(conf.releaseAs);
            next = current.bump(bump);
        } else if (conf.dryRun) {
            next = current.bump(bump == Bump.NONE ? Bump.PATCH : bump);
        } else {
            next = selectVersion(current, bump);
            if (next == null) {
                ui().info("Aborted.");
                return;
            }
        }

        if (next.compareTo(current) <= 0) {
            throw new Fail("The next version [" + next + "] must be greater than the current version [" + current + "].");
        }
        if (git.hasTag(next.toString())) {
            throw new Fail("The tag [" + next + "] already exists.");
        }

        if (conf.dryRun) {
            ui().info("Next       \t", next);
            ui().info("Dry run, so nothing is changed.");
            return;
        }

        // 6. Update the version and push the release commit. The tag is created by the workflow.
        makeFile(versionFile, List.of(next.toString()));
        git.add("version.txt").commit("chore(release): " + next);

        if (conf.push) {
            git.push(branch);
            ui().info("Pushed the release commit.");
        }

        // 7. Dispatch the release workflow and watch it.
        publishVersion(next.toString());

        // 8. Verify the outcome.
        if (GithubAPI.hasRelease(vcs.owner + "/" + vcs.repo, next.toString())) {
            ui().info("Released [", next, "].");
        } else {
            ui().warn("The GitHub Release of [", next, "] was not created. Run [release:publish] to retry.");
        }
    }

    /**
     * Help a release which starts from a working tree with uncommitted changes. The changes are
     * shown and, when the user approves, they are committed and pushed so that the release can
     * proceed from a clean and synchronized branch.
     * 
     * @param git The git client.
     * @param conf The release configuration.
     * @param branch The branch to push.
     */
    private void commitPendingChanges(Git git, Config conf, String branch) {
        List<Git.Status> changes = git.status();
        boolean tracked = false;
        boolean untracked = false;

        ui().warn("The working tree has uncommitted changes.");
        for (Git.Status change : changes) {
            if (change.isUntracked()) {
                untracked = true;
            } else {
                tracked = true;
            }
            ui().info("  ", markChange(change), " ", change.path);
        }

        boolean includeUntracked = false;
        if (untracked) {
            includeUntracked = conf.includeUntracked || ui().confirm("Include the untracked files in the commit?");
        }

        if (!tracked && !includeUntracked) {
            ui().info("No tracked change to commit. The untracked files are left as they are.");
            return;
        }

        if (!conf.push || !ui().confirm("Commit and push them before releasing?")) {
            throw new Fail("The working tree has uncommitted changes.").solve("Commit or stash them before releasing.");
        }

        String message = ui().ask("Commit message", "chore: commit the pending changes before release");
        git.stage(includeUntracked).commit(message).push(branch);
        ui().info("Committed and pushed the pending changes.");
    }

    /**
     * Build the colored one-letter marker of a change. An untracked file is marked with U, and a
     * tracked change with the kind of the change (M, A, D, R, ...).
     * 
     * @param change A change.
     * @return A colored marker.
     */
    private String markChange(Git.Status change) {
        if (change.isUntracked()) {
            return ui().color("U", "36");
        }

        char kind = change.code.charAt(1) != ' ' ? change.code.charAt(1) : change.code.charAt(0);
        return switch (kind) {
        case 'M' -> ui().color("M", "33");
        case 'A' -> ui().color("A", "32");
        case 'D' -> ui().color("D", "31");
        case 'R' -> ui().color("R", "35");
        case 'C' -> ui().color("C", "36");
        case 'T' -> ui().color("T", "33");
        default -> ui().color(String.valueOf(kind), "37");
        };
    }

    /**
     * Help a release which starts from a branch that is not synchronized with its remote. A branch
     * which is only ahead can be pushed, but a branch which is behind must be pulled by the user
     * before releasing.
     * 
     * @param git The git client.
     * @param conf The release configuration.
     * @param branch The branch.
     */
    private void synchronize(Git git, Config conf, String branch) {
        Git.Tracking tracking = git.tracking();

        if (0 < tracking.behind) {
            throw new Fail("The local branch [" + branch + "] is behind the remote by " + tracking.behind + " commit(s).")
                    .solve("Pull the remote changes before releasing.");
        }

        if (tracking.ahead == 0) {
            throw new Fail("The local branch [" + branch + "] is not synchronized with the remote.")
                    .solve("Push or pull before releasing.");
        }

        ui().warn("The local branch [" + branch + "] is ahead of the remote by " + tracking.ahead + " commit(s).");
        if (!conf.push || !ui().confirm("Push them before releasing?")) {
            throw new Fail("The local branch [" + branch + "] is not synchronized with the remote.")
                    .solve("Push or pull before releasing.");
        }
        git.push(branch);
        ui().info("Pushed the pending commits.");
    }

    /**
     * Dispatch the release workflow for the current version and watch the run. This does not change
     * the repository, so it can be used to finish a release whose workflow failed. The version can
     * be overridden with a task setting such as {@code bee release:publish @version=0.80.0}.
     */
    @Command("Dispatch the release workflow for the current version and watch it.")
    default void publish() {
        publishVersion(config().version);
    }

    /**
     * Dispatch the release workflow for the specified version and watch it.
     * 
     * @param version A released version.
     */
    private void publishVersion(String version) {
        boolean watch = config().watch;
        VCS vcs = project().getVersionControlSystem();
        if (vcs == null || !vcs.name().equals("github")) {
            throw new Fail("The release requires a GitHub repository.");
        }
        if (version == null || version.isBlank()) {
            version = project().getVersion();
        }
        String repository = vcs.owner + "/" + vcs.repo;

        String nonce = String.valueOf(System.currentTimeMillis());
        GithubAPI.dispatch(repository, EVENT, new kiss.JSON().set("version", version).set("nonce", nonce));

        if (!watch) {
            ui().info("Dispatched the release workflow [", version, "]. Watch the run on GitHub.");
            return;
        }

        GithubAPI.Run run = waitForRun(repository, nonce);
        if (run == null) {
            ui().warn("The release workflow run could not be found. Check the Actions page.");
            return;
        }
        watch(repository, run, nonce);
    }

    /**
     * Wait until the release workflow run which carries the specified nonce appears.
     * 
     * @param repository The owner/name of the repository.
     * @param nonce A dispatch nonce.
     * @return The run, or <code>null</code> when it is not found in time.
     */
    private GithubAPI.Run waitForRun(String repository, String nonce) {
        long start = System.currentTimeMillis();
        ui().spinner("Waiting for the workflow to start...");

        while (System.currentTimeMillis() - start < DISCOVER_TIMEOUT) {
            for (GithubAPI.Run run : GithubAPI.runs(repository, RUN_EVENT)) {
                if (run.name != null && run.name.contains(nonce)) {
                    return run;
                }
            }
            sleep(POLL_INTERVAL);
        }
        return null;
    }

    /**
     * Poll the specified run and report its progress and outcome.
     * 
     * @param repository The owner/name of the repository.
     * @param run A workflow run.
     * @param nonce The dispatch nonce which the run name carries for discovery. It is removed from
     *        the displayed name, because it is an internal marker and not part of the version.
     */
    private void watch(String repository, GithubAPI.Run run, String nonce) {
        ui().info("Watching the release workflow : ", run.html_url);

        while (!"completed".equals(run.status)) {
            // The spinner overwrites the previous message in place. A multi-line message shows the
            // whole step list with the current position marked.
            ui().spinner(progress(repository, run, nonce));
            sleep(POLL_INTERVAL);

            for (GithubAPI.Run current : GithubAPI.runs(repository, RUN_EVENT)) {
                if (current.id == run.id) {
                    run = current;
                    break;
                }
            }
        }

        // Show the final states, then the outcome. The spinner marker is replaced with a check mark
        // because no animation runs here.
        ui().info(describe(repository, run, nonce).replace(UserInterface.SPINNER_MARKER, ui().color("\u2713", "76")));

        if (config().showLog && !"success".equals(String.valueOf(run.conclusion))) {
            showLog(repository, run);
        }

        switch (String.valueOf(run.conclusion)) {
        case "success":
            return;

        case "cancelled":
            throw new Fail("The release workflow was cancelled : " + run.html_url);

        default:
            throw new Fail("The release workflow failed : " + run.html_url).solve("Run [release:publish] to retry.");
        }
    }

    /**
     * Describe the jobs and the steps of the specified run. The step which is currently running is
     * marked, so the whole list shows the progress at a glance.
     * 
     * @param repository The owner/name of the repository.
     * @param run A workflow run.
     * @param nonce The dispatch nonce which the run name carries for discovery. It is removed from
     *        the displayed name, because it is an internal marker and not part of the version.
     * @return A description.
     */
    private String describe(String repository, GithubAPI.Run run, String nonce) {
        // The run name is "Release <version><nonce>" for a dispatch, so dropping the nonce shows the
        // version alone without changing how the run is discovered.
        String name = run.name == null || nonce == null ? run.name : run.name.replace(nonce, "");
        StringBuilder builder = new StringBuilder();

        for (GithubAPI.Job job : GithubAPI.jobs(repository, run.id)) {
            if (job.steps == null || job.steps.isEmpty()) {
                builder.append("  ").append(job.name).append(" [").append(job.conclusion == null ? job.status : job.conclusion).append("]");
                continue;
            }

            int done = 0;
            for (GithubAPI.Step step : job.steps) {
                if ("completed".equals(step.status)) {
                    done++;
                }
            }
            builder.append(name)
                    .append(" [")
                    .append(run.conclusion == null ? done + "/" + job.steps.size() : run.conclusion)
                    .append("]");

            for (GithubAPI.Step step : job.steps) {
                builder.append(Platform.EOL).append("  ").append(mark(step)).append(" ").append(step.name);
            }
        }
        return builder.toString();
    }

    /**
     * Describe the progress to show while the release run is watched. The release run is queued
     * while the build workflow which the release commit started holds the shared concurrency, and
     * such a queued run has no job yet, so the build run is shown to keep its progress visible
     * instead of an empty spinner.
     * 
     * @param repository The owner/name of the repository.
     * @param run A workflow run.
     * @param nonce The dispatch nonce.
     * @return A description.
     */
    private String progress(String repository, GithubAPI.Run run, String nonce) {
        if (!"in_progress".equals(run.status)) {
            // The run is queued because the build workflow which the release commit started holds
            // the shared concurrency. Show that run so its progress stays visible.
            GithubAPI.Run blocker = blockingRun(repository, run);
            if (blocker != null) {
                return "Waiting for the build workflow : " + blocker.html_url + Platform.EOL + describe(repository, blocker, null);
            }
        }

        String description = describe(repository, run, nonce);
        return description.isBlank() ? "Waiting for the release workflow to start..." : description;
    }

    /**
     * Resolve the run which holds the shared concurrency and blocks the specified run. The build
     * workflow is started by the push of the release commit, so the runs of the push event are
     * searched on the same branch.
     * 
     * @param repository The owner/name of the repository.
     * @param run A workflow run.
     * @return A blocking run, or <code>null</code> when none is found.
     */
    private GithubAPI.Run blockingRun(String repository, GithubAPI.Run run) {
        GithubAPI.Run queued = null;

        for (GithubAPI.Run candidate : GithubAPI.runs(repository, BUILD_EVENT)) {
            if (candidate.id == run.id || "completed".equals(candidate.status)) {
                continue;
            }
            if (run.head_branch != null && !run.head_branch.equals(candidate.head_branch)) {
                continue;
            }

            // The run which is in progress holds the concurrency, so prefer it over a queued one.
            if ("in_progress".equals(candidate.status)) {
                return candidate;
            }
            if (queued == null) {
                queued = candidate;
            }
        }
        return queued;
    }

    /**
     * Build the marker of the specified step.
     * 
     * @param step A step.
     * @return A marker.
     */
    private String mark(GithubAPI.Step step) {
        if ("completed".equals(step.status)) {
            return "success".equals(step.conclusion) ? ui().color("\u2713", "76") + " " : ui().color("\u2717", "1") + " ";
        } else if ("in_progress".equals(step.status)) {
            return UserInterface.SPINNER_MARKER + " ";
        } else {
            return ui().color("\u25e6", "240") + " ";
        }
    }

    /**
     * Show the job logs of the specified run.
     * 
     * @param repository The owner/name of the repository.
     * @param run A workflow run.
     */
    private void showLog(String repository, GithubAPI.Run run) {
        psychopath.Directory logs = GithubAPI.logs(repository, run.id);
        if (logs != null && logs.isPresent()) {
            ui().info("Release workflow log");
            // The log files are UTF-8, so read them explicitly to avoid the platform charset.
            for (psychopath.File file : logs.walkFile("*.txt").toList()) {
                for (String line : file.lines(StandardCharsets.UTF_8).toList()) {
                    ui().info(line);
                }
            }
        }
    }

    /**
     * Sleep the current thread for the specified milliseconds.
     * 
     * @param millis A sleep time.
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Fail("Interrupted while watching the release workflow.").reason(e);
        }
    }

    /**
     * Determine the version bump from the commits since the previous release.
     * 
     * @param current The current version.
     * @param commits The commits since the previous release.
     * @return A bump.
     */
    default Bump determineBump(SemanticVersion current, List<ConventionalCommit> commits) {
        Bump bump = Bump.NONE;
        for (ConventionalCommit commit : commits) {
            bump = bump.max(commit.bump());
        }
        // While the major version is zero, a breaking change bumps the minor version.
        if (current.major == 0 && bump == Bump.MAJOR) {
            bump = Bump.MINOR;
        }
        return bump;
    }

    /**
     * Ask the user to select the version to release. The patch, minor and major bumps are offered
     * together with an arbitrary input and an abort.
     * 
     * @param current The current version.
     * @param suggested The bump which the commits suggest.
     * @return The selected version, or <code>null</code> to abort.
     */
    default SemanticVersion selectVersion(SemanticVersion current, Bump suggested) {
        SemanticVersion patch = current.bump(Bump.PATCH);
        SemanticVersion minor = current.bump(Bump.MINOR);
        SemanticVersion major = current.bump(Bump.MAJOR);
        SemanticVersion recommended = current.bump(suggested);

        Map<String, SemanticVersion> options = new LinkedHashMap();
        options.put(label(patch, "patch", patch.equals(recommended)), patch);
        options.put(label(minor, "minor", minor.equals(recommended)), minor);
        options.put(label(major, "major", major.equals(recommended)), major);
        options.put(CUSTOM, null);
        options.put(ABORT, null);

        String selected = ui().ask("Select the version to release.", new ArrayList<>(options.keySet()));
        if (selected.equals(ABORT)) {
            return null;
        }
        if (selected.equals(CUSTOM)) {
            return askVersion(current, suggested == Bump.NONE ? patch : recommended);
        }
        return options.get(selected);
    }

    /**
     * Build an option label.
     * 
     * @param version A version.
     * @param type A release type.
     * @param suggested Whether the version is suggested by the commits.
     * @return A label.
     */
    private String label(SemanticVersion version, String type, boolean suggested) {
        return version + "  (" + type + ")" + (suggested ? "  [suggested]" : "");
    }

    /**
     * Ask the user to input an arbitrary version.
     * 
     * @param current The current version.
     * @param defaults A default version.
     * @return An input version.
     */
    private SemanticVersion askVersion(SemanticVersion current, SemanticVersion defaults) {
        while (true) {
            String input = ui().ask("Input the version.", defaults.toString());

            if (input.indexOf('-') != -1 || input.indexOf('+') != -1) {
                ui().warn("A pre-release version is not supported.");
                continue;
            }
            try {
                SemanticVersion version = new SemanticVersion(input);
                if (version.compareTo(current) <= 0) {
                    ui().warn("The version must be greater than the current version [", current, "].");
                    continue;
                }
                return version;
            } catch (Exception e) {
                ui().warn("Invalid version : ", input);
            }
        }
    }

    /**
     * Parse the specified release type.
     * 
     * @param type A release type.
     * @return A bump.
     */
    private Bump parseBump(String type) {
        try {
            return Bump.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new Fail("An unknown release type : " + type).solve("Use one of major, minor or patch.");
        }
    }
}
