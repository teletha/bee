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

        if (!git.isClean()) {
            throw new Fail("The working tree has uncommitted changes.").solve("Commit or stash them before releasing.");
        }
        git.fetch();
        if (!git.isSynced()) {
            throw new Fail("The local branch [" + branch + "] is not synchronized with the remote.")
                    .solve("Push or pull before releasing.");
        }

        // The latest tag whose GitHub Release is missing means that a previous release did not
        // finish, so a new one must not start before it is recovered.
        String unfinished = git.latestVersionTag();
        if (unfinished != null && !GithubAPI.hasRelease(vcs.owner + "/" + vcs.repo, unfinished)) {
            throw new Fail("The previous release [" + unfinished + "] has a tag but no GitHub Release.")
                    .solve("Run [release:publish " + unfinished + "] to finish it before releasing again.");
        }

        File versionFile = root.file("version.txt");
        if (versionFile.isAbsent()) {
            throw new Fail("The version file [version.txt] is not found.").solve("Run the [ci] task to generate it.");
        }
        SemanticVersion current = new SemanticVersion(versionFile.text());

        // 2. Collect the commits since the latest tag.
        String previous = git.latestVersionTag();
        List<ConventionalCommit> commits = git.conventionalCommits(previous);

        // 3. Show the context.
        ui().info("Release plan");
        ui().info("  Repository \t", vcs.uri());
        ui().info("  Branch     \t", branch);
        ui().info("  Previous   \t", previous == null ? "(none)" : previous);
        ui().info("  Current    \t", current);
        ui().info("  Commits    \t", commits.size());
        for (ConventionalCommit commit : commits) {
            ui().info("    ", commit);
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
            ui().info("Pushed the release commit for [", next, "].");
        }

        // 7. Dispatch the release workflow and watch it.
        publishVersion(next.toString());

        // 8. Verify the outcome.
        if (GithubAPI.hasRelease(vcs.owner + "/" + vcs.repo, next.toString())) {
            ui().info("Released the version [", next, "].");
        } else {
            ui().warn("The GitHub Release of [", next, "] is not created yet. Run [release:publish ", next, "] to retry.");
        }
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
        ui().info("Dispatch the release workflow for the version [", version, "].");
        GithubAPI.dispatch(repository, EVENT, new kiss.JSON().set("version", version).set("nonce", nonce));

        if (!watch) {
            ui().info("Dispatched. Watch the run on GitHub.");
            return;
        }

        GithubAPI.Run run = waitForRun(repository, nonce);
        if (run == null) {
            ui().warn("The release workflow run could not be found. Check the Actions page.");
            return;
        }
        ui().info("Watching the release workflow : ", run.html_url);

        watch(repository, run);
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
        ui().spinner("Waiting for the release workflow to start...");

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
     */
    private void watch(String repository, GithubAPI.Run run) {
        while (!"completed".equals(run.status)) {
            // The spinner overwrites the previous message in place. A multi-line message shows the
            // whole step list with the current position marked.
            ui().spinner(describe(repository, run));
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
        ui().info(describe(repository, run).replace(UserInterface.SPINNER_MARKER, "✅"));

        if (config().showLog && !"success".equals(String.valueOf(run.conclusion))) {
            showLog(repository, run);
        }

        switch (String.valueOf(run.conclusion)) {
        case "success":
            ui().info("The release workflow succeeded : ", run.html_url);
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
     * @return A description.
     */
    private String describe(String repository, GithubAPI.Run run) {
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
            builder.append(run.name)
                    .append(" [")
                    .append(run.conclusion == null ? done + "/" + job.steps.size() : run.conclusion)
                    .append("]");

            for (GithubAPI.Step step : job.steps) {
                builder.append(Platform.EOL).append("    ").append(mark(step)).append(" ").append(step.name);
            }
        }
        return builder.toString();
    }

    /**
     * Build the marker of the specified step.
     * 
     * @param step A step.
     * @return A marker.
     */
    private String mark(GithubAPI.Step step) {
        if ("completed".equals(step.status)) {
            return "success".equals(step.conclusion) ? "✅" : "❌";
        }
        return "in_progress".equals(step.status) ? UserInterface.SPINNER_MARKER : "⬜";
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
