/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import bee.api.Command;
import bee.api.Task;
import kiss.Disposable;
import kiss.Events;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;

/**
 * @version 2017/01/06 11:29:35
 */
public class Bintray extends Task {

    @Command("Test Bintray")
    public void rest() {
        Client client = new Client();
        Events<Repository> repository = client.get("npc", "maven-non");
        repository.to(r -> {
            System.out.println(r);
        }, e -> {
            e.printStackTrace();
        });
    }

    /**
     * @version 2017/01/06 13:59:11
     */
    @Manageable(lifestyle = Singleton.class)
    private static class Client {

        private String name = "teletha";

        private String key = "7d639e65a03d3714524a719f63ce818af7f7114a";

        /** The actual http client. */
        private final HttpClient client;

        /**
         * 
         */
        private Client() {
            Credentials credentials = new UsernamePasswordCredentials(name, key);
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY, credentials);

            this.client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        }

        /**
         * <p>
         * Create GET request.
         * </p>
         * 
         * @param api
         * @return
         */
        private <T> Events<T> get(Class<T> type, String api) {
            return request(type, new HttpGet(fromPath(api)));
        }

        /**
         * <p>
         * Create POST request.
         * </p>
         * 
         * @param api
         * @return
         */
        private <T> Events<T> post(Class<T> type, String api, Object value) {
            StringBuilder builder = new StringBuilder();
            I.write(value, builder);

            HttpPost post = new HttpPost(fromPath(api));
            post.setEntity(new StringEntity(builder.toString(), StandardCharsets.UTF_8));
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            return request(type, post);
        }

        /**
         * <p>
         * Create REST API.
         * </p>
         * 
         * @param api
         * @return
         */
        private String fromPath(String api) {
            return "https://api.bintray.com/" + api;
        }

        /**
         * <p>
         * Create request.
         * </p>
         * 
         * @param type
         * @param request
         * @return
         */
        private <T> Events<T> request(Class<T> type, HttpUriRequest request) {
            return new Events<T>(observer -> {
                try {
                    HttpResponse response = client.execute(request);
                    int status = response.getStatusLine().getStatusCode();

                    switch (status) {
                    case HttpStatus.SC_OK:
                    case HttpStatus.SC_CREATED:
                        T returned = I.read(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), I.make(type));
                        observer.accept(returned);
                        break;

                    default:
                        observer.error(new HttpException(response.toString()));
                        break;
                    }
                } catch (Exception e) {
                    throw I.quiet(e);
                }
                return Disposable.Φ;
            });
        }

        /**
         * <p>
         * Get the specified name repository
         * </p>
         * 
         * @param name
         * @return
         */
        public Events<Repository> get(String orgnaization, String repositoryName) {
            Events<Repository> repository = get(Repository.class, "repos/" + orgnaization + "/" + repositoryName)
                    .errorResume(post(Repository.class, "repos/" + orgnaization + "/" + repositoryName, null));

            return repository;
        }
    }

    /**
     * @version 2017/01/06 13:10:36
     */
    private static class Repository {

        /** The repository name. */
        public String name;

        /** The repository owner. */
        public String owner;

        /** The repository type. (default is maven) */
        public String type = "maven";

        /** The repository description. */
        public String desc;

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Repository [name=" + name + ", owner=" + owner + ", type=" + type + ", desc=" + desc + "]";
        }
    }

    /**
     * @version 2017/01/06 14:13:58
     */
    private static class GetPackage extends Client {

        private Repository repository;

        /**
         * {@inheritDoc}
         */
        @Override
        public String api(String orgnaization) {
            return "repos/" + orgnaization + "/" + repository.name + "/packages";
        }
    }
}
