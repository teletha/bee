/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.StringJoiner;

import org.apache.maven.model.Contributor;

import kiss.I;
import kiss.JSON;
import psychopath.Directory;

public abstract class VCS {

    /** The uri. */
    protected final URL uri;

    /** The owner name. */
    public final String owner;

    /** The repository name. */
    public final String repo;

    /**
     * <p>
     * Create Github repository.
     * </p>
     * 
     * @param uri
     */
    protected VCS(URL uri) {
        this.uri = uri;

        String path = uri.getPath();
        this.owner = path.substring(1, path.lastIndexOf("/"));
        this.repo = path.substring(path.lastIndexOf("/") + 1);
    }

    /**
     * Return the identical name.
     * 
     * @return
     */
    public String name() {
        return getClass().getSimpleName().toLowerCase();
    }

    /**
     * Return the uri of repository.
     * 
     * @return
     */
    public String uri() {
        try {
            return uri.toURI().toASCIIString();
        } catch (URISyntaxException e) {
            throw I.quiet(e);
        }
    }

    /** The uri for read access. */
    public String uriForRead() {
        throw unsupport("uriForRead");
    }

    /** The uri for write access. */
    public String uriForWrite() {
        throw unsupport("uriForWrite");
    }

    /** The issue tracker uri. */
    public String issue() {
        throw unsupport("issue");
    }

    /**
     * List of commits.
     * 
     * @return
     */
    public List<Commit> commits() {
        throw unsupport("commits");
    }

    /**
     * List of contributors.
     * 
     * @return
     */
    public List<Contributor> contributors() {
        throw unsupport("contributors");
    }

    /**
     * Test whether the specified file is exist or not.
     * 
     * @param filePath
     * @return
     */
    public boolean exist(String filePath) {
        throw unsupport("exist(String)");
    }

    /**
     * Test whether the specified file is exist or not.
     * 
     * @param filePath
     * @return
     */
    public Content file(String filePath) {
        throw unsupport("file(String)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        return uri();
    }

    /**
     * Throw implementation error.
     * 
     * @param signature
     * @return
     */
    RuntimeException unsupport(String signature) {
        return new UnsupportedOperationException("Class [" + getClass().getName() + "] don't implement " + signature + ".");
    }

    /**
     * Select the suitable VCS.
     * 
     * @param uri
     * @return
     */
    public static VCS of(String uri) {
        if (uri == null || uri.isBlank()) {
            return null;
        }

        try {
            URL u = new URL(uri.replaceAll("\\.git$", ""));

            switch (u.getHost()) {
            case "github.com":
                return new GitHub(u);

            default:
                return null;
            }
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Detect the user specified version control system automatically from local repository or
     * clipboard data.
     * 
     * @return
     */
    public static VCS detect(Directory root) {
        return VCS.of(root.file(".git/config")
                .lines()
                .map(v -> v.replaceAll("\\s", ""))
                .take(v -> v.startsWith("url="))
                .map(v -> v.substring(4))
                .to()
                .or(() -> {
                    try {
                        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                        return ((String) clip.getData(DataFlavor.stringFlavor)).strip();
                    } catch (Exception e) {
                        return "";
                    }
                }));
    }

    /**
     * Repository related model.
     */
    public static interface Account {
        String name();

        String password();
    }

    /**
     * Repository related model.
     */
    public static class Content {
        public String type;

        public String name;

        public String path;

        public String sha;

        public int size;

        public String content;
    }

    /**
     * Repository related model.
     */
    public static class Commit {

        public String message;
    }

    /**
     * Repository related model.
     */
    public static class Release {

        public int id;

        public String tag_name;

        public String html_url;

        public String target_commitish;

        public String name;

        public String body;

        public boolean draft;

        public boolean prerelease;

        @Override
        public String toString() {
            return "Release [id=" + id + ", tag_name=" + tag_name + ", html_url=" + html_url + ", target_commitish=" + target_commitish + ", name=" + name + ", body=" + body + ", draft=" + draft + ", prerelease=" + prerelease + "]";
        }
    }

    /**
     * GitHub implementation.
     */
    static class GitHub extends VCS {

        /**
         * @param uri
         */
        GitHub(URL uri) {
            super(uri);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean exist(String filePath) {
            return I.http(uri("repos", owner, repo, "contents", filePath), JSON.class).mapTo(true).take(1).to().next();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Content file(String filePath) {
            return I.http(uri("repos", owner, repo, "contents", filePath), Content.class).take(1).to().next();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String issue() {
            return uri() + "/issues";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String uriForRead() {
            return "scm:git:" + uri() + ".git";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String uriForWrite() {
            return "scm:git:" + uri() + ".git";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Commit> commits() {
            return I.http("https://api.github.com/repos/" + owner + "/" + repo + "/commits?per_page=100", JSON.class)
                    .flatIterable(o -> o.find("*"))
                    .map(o -> {
                        Commit commit = new Commit();
                        commit.message = o.get("commit").text("message");
                        return commit;
                    })
                    .waitForTerminate()
                    .skipError()
                    .toList();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Contributor> contributors() {
            return I.http("https://api.github.com/repos/" + owner + "/" + repo + "/contributors", JSON.class)
                    .flatIterable(c -> c.find("*"))
                    .flatMap(c -> I.http(c.text("url"), JSON.class))
                    .map(u -> {
                        Contributor contributor = new Contributor();
                        contributor.setEmail(u.text("email"));
                        contributor.setName(u.text("name"));
                        contributor.setUrl(u.text("html_url"));
                        return contributor;
                    })
                    .waitForTerminate()
                    .skipError()
                    .toList();
        }

        /**
         * Helper method to create URI.
         * 
         * @param paths
         * @return
         */
        private String uri(Object... paths) {
            StringJoiner builder = new StringJoiner("/", "https://api.github.com/", "");

            for (Object path : paths) {
                builder.add(String.valueOf(path));
            }
            return builder.toString();
        }
    }
}