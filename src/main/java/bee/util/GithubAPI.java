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

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.HttpRetryException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import bee.Fail;
import bee.TaskOperations;
import bee.UserInterface;
import bee.api.Comment;
import kiss.I;
import kiss.JSON;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * A minimal client for the <a href="https://docs.github.com/rest">GitHub REST API</a>.
 * <p>
 * Authentication is performed with the
 * <a
 * href="https://docs.github.com/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow">OAuth
 * 2.0 Device Flow</a>, so no client secret is required and the user only needs a browser to
 * authorize this tool. The issued access token is cached in the user's home directory and reused on
 * the following invocations. Set the {@code GH_TOKEN} or {@code GITHUB_TOKEN} environment variable
 * to bypass the interactive authorization (which is convenient in CI).
 * </p>
 */
public class GithubAPI {

    /**
     * The OAuth App client id used by the Device Flow. Register your own OAuth App at
     * https://github.com/settings/developers, enable "Device Flow".
     */
    private static final String CLIENT_ID = "Ov23ligQPbmLMpfxYjUI";

    /** The scope required to create public and private repositories. */
    private static final String SCOPE = "repo";

    /** The endpoint used to start the Device Flow. */
    private static final String DEVICE_CODE_ENDPOINT = "https://github.com/login/device/code";

    /** The endpoint used to exchange the device code for an access token. */
    private static final String ACCESS_TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token";

    /** The endpoint used to create a repository for the authenticated user. */
    private static final String REPOSITORY_ENDPOINT = "https://api.github.com/user/repos";

    /** The endpoint used to verify the authenticated user. */
    private static final String USER_ENDPOINT = "https://api.github.com/user";

    /** The GitHub API version. */
    private static final String API_VERSION = "2022-11-28";

    /** The local access token file. */
    private static final File TOKEN_FILE = Locator.directory(System.getProperty("user.home")).directory(".bee").file("github.token");

    /** Hide construction. */
    private GithubAPI() {
    }

    /**
     * The repository settings which can be applied by {@link #configure(String, Settings)}. Add new
     * settings as fields and handle them in {@link #configure(String, Settings)}.
     */
    public static class Settings {

        /** Whether GitHub Actions can submit approving pull request reviews. */
        @Comment("Allow GitHub Actions to approve pull requests.")
        public boolean canApprovePullRequestReviews = true;

        /** Whether merge commits are allowed on pull requests. */
        @Comment("Allow merge commits.")
        public boolean allowMergeCommit = false;

        /** Whether head branches are deleted automatically after a pull request is merged. */
        @Comment("Automatically delete head branches.")
        public boolean deleteBranchOnMerge = true;

        /** Whether GitHub Discussions is enabled. */
        @Comment("Enable GitHub Discussions.")
        public boolean hasDiscussions = true;
    }

    /**
     * Create a repository with the specified name for the authenticated user.
     *
     * @param name A name of the repository to create.
     * @param privateRepository Whether the repository is private or public.
     * @return A URL to clone the created repository.
     */
    public static String createRepository(String name, boolean privateRepository) {
        if (name == null || !name.matches("[A-Za-z0-9._-]+")) {
            throw new Fail("An invalid repository name : " + name)
                    .solve("A repository name may only contain alphanumeric characters, hyphens, underscores and periods.");
        }

        UserInterface ui = TaskOperations.ui();
        String token = token(ui);
        String payload = "{\"name\":\"" + name + "\",\"private\":" + privateRepository + "}";

        try {
            JSON repository = invoke(HttpRequest.newBuilder(URI.create(REPOSITORY_ENDPOINT))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + token)
                    .header("X-GitHub-Api-Version", API_VERSION)
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(payload, StandardCharsets.UTF_8)));

            ui.info("Created repository : ", repository.text("html_url"));
            return repository.text("clone_url");
        } catch (Fail e) {
            if (alreadyExists(e)) {
                ui.info("The repository [", name, "] already exists. Skip creation.");
                return repositoryUrl(name);
            }
            throw e;
        }
    }

    /**
     * Apply the specified settings to the repository for the authenticated user.
     *
     * @param name A repository name.
     * @param settings Repository settings to apply.
     */
    public static void configure(String name, Settings settings) {
        UserInterface ui = TaskOperations.ui();
        String token = token(ui);
        String endpoint = "https://api.github.com/repos/" + owner(token) + "/" + name;

        // ---------------------------------------------------------------------
        // GitHub Actions default workflow permissions
        // ---------------------------------------------------------------------
        // Preserve the current default workflow permissions while updating the setting.
        JSON current = invoke(HttpRequest.newBuilder(URI.create(endpoint + "/actions/permissions/workflow"))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", API_VERSION)
                .GET());

        StringBuilder permissions = new StringBuilder("{");
        String defaultPermissions = current.text("default_workflow_permissions");
        if ("read".equals(defaultPermissions) || "write".equals(defaultPermissions)) {
            permissions.append("\"default_workflow_permissions\":\"").append(defaultPermissions).append("\",");
        }
        permissions.append("\"can_approve_pull_request_reviews\":").append(settings.canApprovePullRequestReviews).append("}");

        request(HttpRequest.newBuilder(URI.create(endpoint + "/actions/permissions/workflow"))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("Content-Type", "application/json")
                .method("PUT", BodyPublishers.ofString(permissions.toString(), StandardCharsets.UTF_8)));

        // ---------------------------------------------------------------------
        // Repository merge settings
        // ---------------------------------------------------------------------
        String repository = "{\"allow_merge_commit\":" + settings.allowMergeCommit + ",\"delete_branch_on_merge\":" + settings.deleteBranchOnMerge + "}";

        JSON updated = invoke(HttpRequest.newBuilder(URI.create(endpoint))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("Content-Type", "application/json")
                .method("PATCH", BodyPublishers.ofString(repository, StandardCharsets.UTF_8)));

        // ---------------------------------------------------------------------
        // GitHub Discussions
        // ---------------------------------------------------------------------
        // The REST API can enable discussions only while creating a repository, so the GraphQL API
        // is used to change the setting on an existing repository.
        if (settings.hasDiscussions != Boolean.parseBoolean(updated.text("has_discussions"))) {
            String query = "mutation { updateRepository(input: {repositoryId: \"" + updated
                    .text("node_id") + "\", hasDiscussionsEnabled: " + settings.hasDiscussions + "}) { repository { hasDiscussionsEnabled } } }";

            JSON response = invoke(HttpRequest.newBuilder(URI.create("https://api.github.com/graphql"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(new JSON().set("query", query).toString(), StandardCharsets.UTF_8)));

            JSON errors = response.get("errors");
            if (errors != null && errors.has("0")) {
                throw new Fail("Failed to configure the discussions : " + errors.get("0").text("message"));
            }
        }

        ui.info("Configured the repository [", name, "] : can_approve_pull_request_reviews=", settings.canApprovePullRequestReviews, ", allow_merge_commit=", settings.allowMergeCommit, ", delete_branch_on_merge=", settings.deleteBranchOnMerge, ", has_discussions=", settings.hasDiscussions);
    }

    /**
     * Resolve the clone URL of the specified repository for the authenticated user.
     *
     * @param name A repository name.
     * @return A URL to clone the repository.
     */
    public static String repositoryUrl(String name) {
        return "https://github.com/" + owner(token(TaskOperations.ui())) + "/" + name + ".git";
    }

    /**
     * Resolve the login name of the authenticated user.
     *
     * @param token An access token.
     * @return A login name.
     */
    private static String owner(String token) {
        JSON user = invoke(HttpRequest.newBuilder(URI.create(USER_ENDPOINT))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", API_VERSION)
                .GET());

        String login = user.text("login");
        if (login == null || login.isBlank()) {
            throw new Fail("Failed to resolve the authenticated GitHub user.");
        }
        return login;
    }

    /**
     * Connect the specified local directory with the remote repository. When the remote has commits,
     * this checks out its default branch. Existing local files are kept, so a project which already
     * contains Bee files can be attached to the remote safely.
     *
     * @param url A remote repository URL.
     * @param directory A local directory to connect.
     */
    public static void connect(String url, Directory directory) {
        UserInterface ui = TaskOperations.ui();

        if (!Git.isAvailable()) {
            throw new Fail("The git command is not found.").solve("Install Git and make it available on your PATH.");
        }

        Git git = Git.at(directory);

        // initialize the local repository
        if (!directory.directory(".git").isPresent()) {
            git.init();
        }

        // configure the origin remote
        if (git.hasRemote("origin")) {
            if (!url.equals(git.remoteUrl("origin"))) {
                git.setRemote(url);
            }
        } else {
            git.addRemote(url);
        }

        // track the remote default branch if it already has commits
        git.fetch("origin");

        String branch = git.remoteDefaultBranch("origin");
        if (branch != null && !git.hasCommit()) {
            if (git.run("checkout", branch) != 0) {
                ui.warn("Failed to check out the branch [", branch, "]. Local files may conflict with the remote.");
            }
        }

        ui.info("Configured the repository : ", url);
    }

    /**
     * Test whether the specified error means that the repository already exists.
     *
     * @param error A request error.
     * @return A result.
     */
    private static boolean alreadyExists(Throwable error) {
        while (error != null) {
            if (error instanceof HttpRetryException http && http.responseCode() == 422 && String.valueOf(error.getMessage())
                    .contains("already exists")) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    /**
     * Resolve the access token used to access the GitHub API. An environment variable has the
     * highest priority, then the cached token, and finally the interactive Device Flow.
     *
     * @param ui A user interface.
     * @return An access token.
     */
    private static String token(UserInterface ui) {
        // 1. well-known environment variables (useful in CI)
        for (String key : List.of("GH_TOKEN", "GITHUB_TOKEN")) {
            String token = I.env(key);
            if (token != null && !token.isBlank()) {
                ui.debug("Use the GitHub token from the environment variable [", key, "].");
                return token.trim();
            }
        }

        // 2. the cached token
        if (TOKEN_FILE.isPresent()) {
            String token = TOKEN_FILE.text().trim();
            if (!token.isEmpty() && verify(token)) {
                ui.debug("Use the cached GitHub token.");
                return token;
            }
        }

        // 3. authorize interactively and cache the result
        String token = authorize(ui);
        TOKEN_FILE.parent().create();
        TOKEN_FILE.text(token);
        ui.debug("The GitHub token is saved at ", TOKEN_FILE);
        return token;
    }

    /**
     * Verify that the specified token is still valid and grants the required scope.
     *
     * @param token An access token.
     * @return A result.
     */
    private static boolean verify(String token) {
        try {
            HttpResponse response = I.http(HttpRequest.newBuilder(URI.create(USER_ENDPOINT))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + token)
                    .header("X-GitHub-Api-Version", API_VERSION)
                    .GET(), HttpResponse.class).waitForTerminate().to().acquire();

            try {
                String scopes = response.headers().firstValue("x-oauth-scopes").orElse("");
                for (String required : SCOPE.split(" ")) {
                    if (!List.of(scopes.split("\\s*,\\s*")).contains(required)) {
                        return false;
                    }
                }
                return true;
            } finally {
                I.quiet(response.body());
            }
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Authorize this tool by the OAuth 2.0 Device Flow.
     *
     * @param ui A user interface.
     * @return An access token.
     */
    private static String authorize(UserInterface ui) {
        JSON device = invoke(HttpRequest.newBuilder(URI.create(DEVICE_CODE_ENDPOINT))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(form("client_id", CLIENT_ID, "scope", SCOPE), StandardCharsets.UTF_8)));

        String error = device.text("error");
        if (error != null && !error.isEmpty()) {
            throw new Fail("Failed to start the GitHub Device Flow : " + device.text("error_description"));
        }

        String deviceCode = device.text("device_code");
        String userCode = device.text("user_code");
        String verification = device.text("verification_uri");
        String complete = device.text("verification_uri_complete");
        int interval = device.has("interval") ? Integer.parseInt(device.text("interval")) : 5;
        int expires = device.has("expires_in") ? Integer.parseInt(device.text("expires_in")) : 900;

        ui.info("To authorize Bee, open the following URL in your browser.");
        ui.info("    ", verification);
        ui.info("Then enter the following code.");
        ui.info("    ", userCode);
        if (copyToClipboard(userCode)) {
            ui.info("The code has been copied to your clipboard.");
        }
        openBrowser(complete != null && !complete.isEmpty() ? complete : verification);

        long deadline = System.currentTimeMillis() + expires * 1000L;
        while (System.currentTimeMillis() < deadline) {
            sleep(interval);

            JSON result = invoke(HttpRequest.newBuilder(URI.create(ACCESS_TOKEN_ENDPOINT))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(BodyPublishers
                            .ofString(form("client_id", CLIENT_ID, "device_code", deviceCode, "grant_type", "urn:ietf:params:oauth:grant-type:device_code"), StandardCharsets.UTF_8)));

            String token = result.text("access_token");
            if (token != null && !token.isEmpty()) {
                return token;
            }

            switch (String.valueOf(result.text("error"))) {
            case "authorization_pending":
                break;

            case "slow_down":
                interval += 5;
                break;

            case "expired_token":
                throw new Fail("The GitHub device code expired.").solve("Run the task again to restart the authorization.");

            case "access_denied":
                throw new Fail("The GitHub authorization was denied by the user.");

            default:
                throw new Fail("The GitHub authorization failed : " + result.text("error_description"));
            }
        }

        throw new Fail("The GitHub authorization timed out.").solve("Run the task again to restart the authorization.");
    }

    /**
     * Copy the specified text to the system clipboard if possible.
     *
     * @param text A text to copy.
     * @return Whether the text has been copied or not.
     */
    private static boolean copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            return true;
        } catch (Throwable e) {
            // Clipboard isn't available on a headless environment, so any error is ignored.
            return false;
        }
    }

    /**
     * Open the specified URL in the default browser if possible.
     *
     * @param url A URL to open.
     */
    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Throwable e) {
            // The user can open the URL manually, so any error is ignored.
        }
    }

    /**
     * Sleep the current thread for the specified seconds.
     *
     * @param seconds A sleep time.
     */
    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Fail("The GitHub authorization was interrupted.").reason(e);
        }
    }

    /**
     * Send the specified request and wait for the JSON response.
     *
     * @param request A request builder.
     * @return A response body.
     */
    private static JSON invoke(HttpRequest.Builder request) {
        try {
            return I.http(request, JSON.class).waitForTerminate().to().acquire();
        } catch (Throwable e) {
            throw new Fail("Failed to access the GitHub API.").reason(Fail.strip(e));
        }
    }

    /**
     * Send the specified request and wait for the response. This is used for requests whose response
     * body is empty (e.g. 204 No Content).
     *
     * @param request A request builder.
     */
    private static void request(HttpRequest.Builder request) {
        try {
            I.http(request, String.class).waitForTerminate().to().acquire();
        } catch (Throwable e) {
            throw new Fail("Failed to access the GitHub API.").reason(Fail.strip(e));
        }
    }

    /**
     * Build an application/x-www-form-urlencoded body.
     *
     * @param pairs A sequence of key-value pairs.
     * @return An encoded body.
     */
    private static String form(Object... pairs) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i != 0) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(String.valueOf(pairs[i]), StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(String.valueOf(pairs[i + 1]), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }
}
