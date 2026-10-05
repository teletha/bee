/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bee.Fail;
import psychopath.Directory;

/**
 * A thin wrapper over the command line git. Every git invocation is centralized here, so that the
 * underlying implementation can be replaced (e.g. by JGit) without changing the tasks.
 */
public class Git {

    /** The working directory of the repository. */
    private final Directory directory;

    /**
     * Hide construction.
     * 
     * @param directory A repository root.
     */
    private Git(Directory directory) {
        this.directory = directory;
    }

    /**
     * Create a git client for the specified repository.
     * 
     * @param directory A repository root.
     * @return A git client.
     */
    public static Git at(Directory directory) {
        return new Git(directory);
    }

    /**
     * Test whether the git command is available.
     * 
     * @return A result.
     */
    public static boolean isAvailable() {
        return Process.isAvailable("git");
    }

    /**
     * Execute the specified git command and return its output.
     * 
     * @param command A command line after git.
     * @return An output.
     */
    public String read(String... command) {
        return read(Arrays.asList(command));
    }

    /**
     * Execute the specified git command and return its output.
     * 
     * @param command A command line after git.
     * @return An output.
     */
    public String read(List<String> command) {
        return Process.with().workingDirectory(directory).read(full(command));
    }

    /**
     * Execute the specified git command and return its exit code.
     * 
     * @param command A command line after git.
     * @return An exit code.
     */
    public int run(String... command) {
        return Process.with().workingDirectory(directory).run(full(Arrays.asList(command)));
    }

    /**
     * Execute the specified git command silently and return its exit code.
     * 
     * @param command A command line after git.
     * @return An exit code.
     */
    private int exit(String... command) {
        return Process.with().workingDirectory(directory).ignoreOutput().run(full(Arrays.asList(command)));
    }

    /**
     * Prefix the git command to the specified arguments.
     * 
     * @param command A command line after git.
     * @return A full command line.
     */
    private List<String> full(List<String> command) {
        List<String> full = new ArrayList();
        full.add("git");
        full.addAll(command);
        return full;
    }

    /**
     * Execute the specified git command and fail when it exits abnormally.
     * 
     * @param command A command line after git.
     * @return Fluent API.
     */
    public Git exec(String... command) {
        int exit = run(command);
        if (exit != 0) {
            throw new Fail("The git command " + Arrays.toString(command) + " failed with the exit code [" + exit + "].");
        }
        return this;
    }

    /**
     * Resolve the current branch.
     * 
     * @return A branch name.
     */
    public String branch() {
        return read("rev-parse", "--abbrev-ref", "HEAD");
    }

    /**
     * Test whether the working tree has no tracked change.
     * 
     * @return A result.
     */
    public boolean isClean() {
        return read("status", "--porcelain", "--untracked-files=no").isBlank();
    }

    /**
     * Test whether the current branch is synchronized with its remote.
     * 
     * @return A result.
     */
    public boolean isSynced() {
        for (String line : read("status", "--porcelain=v1", "--branch", "--untracked-files=no").split("\\R")) {
            if (line.startsWith("## ")) {
                return !line.contains("[ahead") && !line.contains("[behind");
            }
        }
        return true;
    }

    /**
     * List the tags in the descending version order.
     * 
     * @return A list of tags.
     */
    public List<String> tags() {
        List<String> tags = new ArrayList();
        for (String tag : read("tag", "--sort=-v:refname").split("\\R")) {
            if (!tag.isBlank()) {
                tags.add(tag.trim());
            }
        }
        return tags;
    }

    /**
     * Resolve the latest version tag.
     * 
     * @return The latest version tag, or <code>null</code> when there is none.
     */
    public String latestVersionTag() {
        String latest = null;
        SemanticVersion latestVersion = null;

        for (String tag : tags()) {
            if (SemanticVersion.isVersion(tag)) {
                SemanticVersion version = new SemanticVersion(tag);
                if (latestVersion == null || latestVersion.compareTo(version) < 0) {
                    latestVersion = version;
                    latest = tag;
                }
            }
        }
        return latest;
    }

    /**
     * Test whether the specified tag exists.
     * 
     * @param tag A tag name.
     * @return A result.
     */
    public boolean hasTag(String tag) {
        return !read("tag", "--list", tag).isBlank();
    }

    /**
     * Collect the conventional commits since the specified revision.
     * 
     * @param fromExclusive A revision which is excluded, or <code>null</code> to read all.
     * @return A list of commits.
     */
    public List<ConventionalCommit> conventionalCommits(String fromExclusive) {
        List<String> command = List.of("log", fromExclusive == null ? "HEAD" : fromExclusive + "..HEAD", "--no-merges", "--pretty=format:%H%x1f%s%x1f%b%x1e");

        List<ConventionalCommit> commits = new ArrayList();
        for (String record : read(command).split("\u001e")) {
            String[] fields = record.strip().split("\u001f", -1);
            if (fields.length >= 2 && !fields[0].isBlank()) {
                commits.add(new ConventionalCommit(fields[0].trim(), fields[1], fields.length > 2 ? fields[2] : ""));
            }
        }
        return commits;
    }

    /**
     * Test whether the specified remote is configured.
     * 
     * @param name A remote name.
     * @return A result.
     */
    public boolean hasRemote(String name) {
        return exit("remote", "get-url", name) == 0;
    }

    /**
     * Resolve the URL of the specified remote.
     * 
     * @param name A remote name.
     * @return A remote URL.
     */
    public String remoteUrl(String name) {
        return read("remote", "get-url", name);
    }

    /**
     * Resolve the default branch of the specified remote.
     * 
     * @param name A remote name.
     * @return A default branch name, or <code>null</code> when the remote is empty.
     */
    public String remoteDefaultBranch(String name) {
        String prefix = "ref: refs/heads/";

        for (String line : read("ls-remote", "--symref", name, "HEAD").split("\\R")) {
            if (line.startsWith(prefix)) {
                String rest = line.substring(prefix.length());
                int tab = rest.indexOf('\t');
                return (tab == -1 ? rest : rest.substring(0, tab)).trim();
            }
        }
        return null;
    }

    /**
     * Test whether the repository has at least one commit.
     * 
     * @return A result.
     */
    public boolean hasCommit() {
        return exit("rev-parse", "--verify", "HEAD") == 0;
    }

    /**
     * Initialize the repository.
     * 
     * @return Fluent API.
     */
    public Git init() {
        return exec("init");
    }

    /**
     * Stage the specified path.
     * 
     * @param path A path.
     * @return Fluent API.
     */
    public Git add(String path) {
        return exec("add", path);
    }

    /**
     * Commit the staged changes.
     * 
     * @param message A commit message.
     * @return Fluent API.
     */
    public Git commit(String message) {
        return exec("commit", "-m", message);
    }

    /**
     * Create a tag at the current commit.
     * 
     * @param tag A tag name.
     * @return Fluent API.
     */
    public Git tag(String tag) {
        return exec("tag", tag);
    }

    /**
     * Add the origin remote.
     * 
     * @param url A remote URL.
     * @return Fluent API.
     */
    public Git addRemote(String url) {
        return exec("remote", "add", "origin", url);
    }

    /**
     * Update the origin remote.
     * 
     * @param url A remote URL.
     * @return Fluent API.
     */
    public Git setRemote(String url) {
        return exec("remote", "set-url", "origin", url);
    }

    /**
     * Fetch the tags and prune the deleted branches from the origin.
     * 
     * @return Fluent API.
     */
    public Git fetch() {
        return exec("fetch", "--tags", "--prune");
    }

    /**
     * Fetch the specified remote.
     * 
     * @param remote A remote name.
     * @return Fluent API.
     */
    public Git fetch(String remote) {
        return exec("fetch", remote);
    }

    /**
     * Push the specified ref to the origin.
     * 
     * @param ref A ref name.
     * @return Fluent API.
     */
    public Git push(String ref) {
        return exec("push", "origin", ref);
    }
}
