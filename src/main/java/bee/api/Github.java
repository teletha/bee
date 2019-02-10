/*
 * Copyright (C) 2019 Nameless Production Committee
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.message.BasicHeader;
import org.apache.maven.model.Contributor;

import bee.util.Config;
import bee.util.RESTClient;
import kiss.I;
import kiss.Signal;
import kiss.Variable;

/**
 * @version 2017/01/16 16:27:13
 */
public abstract class Github {

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
    protected Github(URI uri) {
        this.uri = uri;

        String path = uri.getPath();
        this.owner = path.substring(1, path.lastIndexOf("/"));
        this.repo = path.substring(path.lastIndexOf("/") + 1);
    }

    /** The name. */
    public abstract String name();

    /** The uri. */
    public abstract String uri();

    /** The uri for read access. */
    public abstract String uriForRead();

    /** The uri for write access. */
    public abstract String uriForWrite();

    /** The issue tracker uri. */
    public abstract String issue();

    /**
     * <p>
     * List of contributors.
     * </p>
     * 
     * @return
     */
    public abstract List<Contributor> contributors();

    /**
     * <p>
     * Test whether the specified file is exist or not.
     * </p>
     * 
     * @param filePath
     * @return
     */
    public boolean exist(String filePath) {
        return new RESTClient().get(uri("repos", owner, repo, "contents", filePath)).take(1).toBinary().get();
    }

    /**
     * <p>
     * Test whether the specified file is exist or not.
     * </p>
     * 
     * @param filePath
     * @return
     */
    public Content file(String filePath) {
        return new RESTClient().get(uri("repos", owner, repo, "contents", filePath), new Content()).take(1).to().get();
    }

    /**
     * @param pom
     */
    public boolean checkSame(Path file) {
        try {
            Project project = I.make(Project.class);
            byte[] local = Files.readAllBytes(file);
            byte[] remote = Base64.decodeBase64(file(project.getRoot().asJavaPath().relativize(file).toString()).content);

            return Arrays.equals(local, remote);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Retrieve all releases.
     * </p>
     * 
     * @return
     */
    public Variable<Releases> releases() {
        return new RESTClient().get(repo("releases"), new Releases()).to();
    }

    /**
     * @return
     */
    public Signal<Release> release() {
        Project project = I.make(Project.class);
        Account account = Config.project(Account.class);
        RESTClient client = new RESTClient(builder -> {
            builder.setDefaultHeaders(Arrays.asList(new BasicHeader("Authorization", "token " + account.password())));
        });

        Release release = new Release();
        release.tag_name = project.getVersion();
        return client.delete(repo("releases/" + project.getVersion())).flatMap(v -> client.post(repo("releases"), release));
    }

    /**
     * @return
     */
    public Signal<Release> release(String tag) {
        return new RESTClient().get(repo("releases/tags/" + tag), new Release());
    }

    /**
     * <p>
     * Helper method to create URI.
     * </p>
     * 
     * @param paths
     * @return
     */
    private String repo(String path) {
        return uri("repos", owner, repo, path);
    }

    /**
     * <p>
     * Helper method to create URI.
     * </p>
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

    /**
     * @version 2017/01/25 15:50:53
     */
    public static interface Account {
        String name();

        String password();
    }

    /**
     * @version 2017/01/25 11:14:54
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
     * @version 2017/01/21 10:25:45
     */
    public static class Releases extends ArrayList<Release> {
    }

    /**
     * @version 2017/01/21 10:25:55
     */
    public class Release {

        public int id;

        public String tag_name;

        public String html_url;

        public String target_commitish;

        public String name;

        public String body;

        public boolean draft;

        public boolean prerelease;

        public Signal<Release> update() {
            Account account = Config.project(Account.class);
            RESTClient client = new RESTClient(builder -> {
                builder.setDefaultHeaders(Arrays.asList(new BasicHeader("Authorization", "token " + account.password())));
            });

            return client.delete(repo("releases/" + id)).flatMap(v -> client.post(repo("releases"), new Release()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Release [id=" + id + ", tag_name=" + tag_name + ", html_url=" + html_url + ", target_commitish=" + target_commitish + ", name=" + name + ", body=" + body + ", draft=" + draft + ", prerelease=" + prerelease + "]";
        }
    }
}
