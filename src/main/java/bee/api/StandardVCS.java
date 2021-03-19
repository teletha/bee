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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Contributor;

import bee.Bee;
import kiss.I;

abstract class StandardVCS extends Github {

    public static void main(String[] args) throws InterruptedException {
        I.load(Bee.class);

        GitHub hub = new GitHub(URI.create("https://github.com/Teletha/Sinobu"));

        Thread.sleep(1000 * 5);
    }

    /**
     * @param uri
     */
    protected StandardVCS(URI uri) {
        super(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String uri() {
        return uri.toASCIIString();
    }

    /**
     * <p>
     * Select standard VCS.
     * </p>
     * 
     * @param uri
     * @return
     */
    static Github of(URI uri) {
        switch (uri.getHost()) {
        case "github.com":
            return new GitHub(uri);

        default:
            return null;
        }
    }

    /**
     * Repository model.
     */
    static class GitHub extends StandardVCS {

        /**
         * @param uri
         */
        private GitHub(URI uri) {
            super(uri);
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
        public List<Contributor> contributors() {
            return I.http("https://api.github.com/repos/teletha/bee/contributors", GithubContributors.class)
                    .flatIterable(c -> c)
                    .flatMap(c -> I.http(c.url, GitHubUser.class))
                    .map(u -> {
                        Contributor contributor = new Contributor();
                        contributor.setEmail(u.email);
                        contributor.setName(u.name);
                        contributor.setUrl(u.html_url);
                        return contributor;
                    })
                    .skipError()
                    .toList();
        }

        /**
         * Repository related model.
         */
        @SuppressWarnings("serial")
        private static class GithubContributors extends ArrayList<GitHubContributor> {
        }

        /**
         * Repository related model.
         */
        private static class GitHubContributor {

            public String url;
        }

        /**
         * Repository related model.
         */
        private static class GitHubUser {

            public String name;

            public String email;

            public String html_url;
        }
    }
}