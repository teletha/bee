/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.StringJoiner;

import org.apache.maven.model.Contributor;

import bee.util.Inputs;
import kiss.I;
import kiss.JSON;

public abstract class VCS {

    /** The uri. */
    protected final URI uri;

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
    protected VCS(URI uri) {
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
        return uri.toASCIIString();
    }

    /** The uri for read access. */
    public String uriForRead() {
        throw new UnsupportedOperationException(getClass() + " don't implement " + Inputs.signature(VCS::uriForRead) + ".");
    }

    /** The uri for write access. */
    public String uriForWrite() {
        throw new UnsupportedOperationException(getClass() + " don't implement " + Inputs.signature(VCS::uriForWrite) + ".");
    }

    /** The issue tracker uri. */
    public String issue() {
        throw new UnsupportedOperationException(getClass() + " don't implement " + Inputs.signature(VCS::issue) + ".");
    }

    /**
     * List of commits.
     * 
     * @return
     */
    public List<Commit> commits() {
        throw new UnsupportedOperationException(getClass() + " don't implement " + Inputs.signature(VCS::commits) + ".");
    }

    /**
     * List of contributors.
     * 
     * @return
     */
    public List<Contributor> contributors() {
        throw new UnsupportedOperationException(getClass() + " don't implement " + Inputs.signature(VCS::contributors) + ".");
    }

    /**
     * Test whether the specified file is exist or not.
     * 
     * @param filePath
     * @return
     */
    public abstract boolean exist(String filePath);

    /**
     * Test whether the specified file is exist or not.
     * 
     * @param filePath
     * @return
     */
    public abstract Content file(String filePath);

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        return uri();
    }

    /**
     * @param file
     * @return
     */
    private boolean checkSame(Path file) {
        try {
            Project project = I.make(Project.class);
            byte[] local = Files.readAllBytes(file);
            byte[] remote = Base64.getDecoder().decode(file(project.getRoot().asJavaPath().relativize(file).toString()).content);

            return Arrays.equals(local, remote);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Select the suitable VCS.
     * 
     * @param uri
     * @return
     */
    public static VCS of(URI uri) {
        switch (uri.getHost()) {
        case "github.com":
            return new GitHub(uri);

        default:
            return null;
        }
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
        GitHub(URI uri) {
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
                    .skipError()
                    .waitForTerminate()
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
                    .skipError()
                    .waitForTerminate()
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