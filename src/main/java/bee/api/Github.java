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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.codec.binary.Base64;
import org.apache.maven.model.Contributor;

import kiss.I;
import kiss.JSON;

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
     * List of contributors.
     * 
     * @return
     */
    public abstract List<Contributor> contributors();

    /**
     * Test whether the specified file is exist or not.
     * 
     * @param filePath
     * @return
     */
    public boolean exist(String filePath) {
        return I.http(uri("repos", owner, repo, "contents", filePath), JSON.class).take(1).toBinary().next();
    }

    /**
     * Test whether the specified file is exist or not.
     * 
     * @param filePath
     * @return
     */
    public Content file(String filePath) {
        return I.http(uri("repos", owner, repo, "contents", filePath), Content.class).take(1).to().next();
    }

    /**
     * @param file
     * @return
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
    @SuppressWarnings("serial")
    public static class Releases extends ArrayList<Release> {
    }

    /**
     * Repository related model.
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

        @Override
        public String toString() {
            return "Release [id=" + id + ", tag_name=" + tag_name + ", html_url=" + html_url + ", target_commitish=" + target_commitish + ", name=" + name + ", body=" + body + ", draft=" + draft + ", prerelease=" + prerelease + "]";
        }
    }
}