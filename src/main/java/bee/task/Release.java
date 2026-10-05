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

import java.util.List;

import bee.Fail;
import bee.Task;
import bee.api.Command;
import bee.api.Comment;
import bee.api.Project;
import bee.api.VCS;
import bee.util.ConventionalCommit;
import bee.util.Git;
import bee.util.SemanticVersion;
import bee.util.SemanticVersion.Bump;
import psychopath.Directory;
import psychopath.File;

/**
 * Releases the project. The next version is computed from the conventional commits since the latest
 * tag, the version file is updated and committed, and the version is tagged and pushed. The tag push
 * starts the release workflow, which creates the GitHub Release and publishes the artifacts.
 */
public interface Release extends Task<Release.Config> {

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

        /** Whether the release commit and the tag are pushed to the remote. */
        @Comment("Push the release commit and the tag to the remote.")
        public boolean push = true;

        /** The branch to release from. The current branch is used when it is empty. */
        @Comment("The branch to release from. The current branch is used when it is empty.")
        public String branch;
    }

    /**
     * Release the project.
     */
    @Command(defaults = true, value = "Release the project by bumping the version and creating the tag.")
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
            throw new Fail("The local branch [" + branch + "] is not synchronized with the remote.").solve("Push or pull before releasing.");
        }

        File versionFile = root.file("version.txt");
        if (versionFile.isAbsent()) {
            throw new Fail("The version file [version.txt] is not found.").solve("Run the [ci] task to generate it.");
        }
        SemanticVersion current = new SemanticVersion(versionFile.text());

        // 2. Collect the commits since the latest tag.
        String previous = git.latestVersionTag();
        List<ConventionalCommit> commits = git.conventionalCommits(previous);

        // 3. Compute the next version.
        Bump bump = determineBump(current, commits);

        SemanticVersion next;
        if (conf.version != null && !conf.version.isBlank()) {
            if (conf.version.indexOf('-') != -1 || conf.version.indexOf('+') != -1) {
                throw new Fail("A pre-release version is not supported : " + conf.version);
            }
            next = new SemanticVersion(conf.version);
        } else if (conf.releaseAs != null && !conf.releaseAs.isBlank()) {
            bump = parseBump(conf.releaseAs);
            next = current.bump(bump);
        } else if (bump == Bump.NONE) {
            String choice = ui().ask("No releasable commit was found. Select the release type.", List.of("major", "minor", "patch", "abort"));
            if (choice.equals("abort")) {
                ui().info("Aborted.");
                return;
            }
            bump = parseBump(choice);
            next = current.bump(bump);
        } else {
            next = current.bump(bump);
        }

        if (next.compareTo(current) <= 0) {
            throw new Fail("The next version [" + next + "] must be greater than the current version [" + current + "].");
        }
        if (git.hasTag(next.toString())) {
            throw new Fail("The tag [" + next + "] already exists.");
        }

        // 4. Show the plan and confirm.
        ui().info("Release plan");
        ui().info("  Repository \t", vcs.uri());
        ui().info("  Branch     \t", branch);
        ui().info("  Previous   \t", previous == null ? "(none)" : previous);
        ui().info("  Current    \t", current);
        ui().info("  Next       \t", next, " (", bump.name().toLowerCase(), ")");
        ui().info("  Commits    \t", commits.size());
        for (ConventionalCommit commit : commits) {
            ui().info("    ", commit);
        }

        if (conf.dryRun) {
            ui().info("Dry run, so nothing is changed.");
            return;
        }
        if (!ui().confirm("Release the version [" + next + "] ?")) {
            ui().info("Aborted.");
            return;
        }

        // 5. Update the version, commit and tag.
        makeFile(versionFile, List.of(next.toString()));
        git.add("version.txt").commit("chore(release): " + next).tag(next.toString());

        // 6. Push the branch and the tag.
        if (conf.push) {
            git.push(branch).push(next.toString());
            ui().info("Pushed the release commit and the tag [", next, "].");
        }

        ui().info("Released the version [", next, "].");
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
