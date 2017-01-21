/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.maven.model.Contributor;

import bee.util.RESTClient;
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
     * Retrieve all releases.
     * </p>
     * 
     * @return
     */
    public Variable<Releases> releases() {
        return new RESTClient().get(repo("releases"), new Releases()).to();
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
     * @version 2017/01/21 10:25:45
     */
    public static class Releases extends ArrayList<Release> {
    }

    /**
     * @version 2017/01/21 10:25:55
     */
    public static class Release {

        public String tag_name;

        public String html_url;

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Release [name=" + tag_name + ", url=" + html_url + "]";
        }
    }
}
