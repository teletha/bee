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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.aether.transfer.TransferResource;

import bee.api.Command;
import bee.api.Library;
import bee.api.Project;
import bee.api.Task;
import bee.util.RESTClient;
import kiss.Events;
import kiss.I;

/**
 * @version 2017/01/10 1:08:40
 */
public class Bintray extends Task {

    /** The bintray domain. */
    private static final String uri = "https://api.bintray.com/";

    private String name = "teletha";

    private String key = "7d639e65a03d3714524a719f63ce818af7f7114a";

    @Command("Deploy products to Bintray repository.")
    public void deploy() {
        require(Install.class).project();

        RESTClient client = new RESTClient(name, key);
        Library library = project.getLibrary();
        Repository repo = Repository.of(project.getGroup());
        Package pack = Package.of(repo, project);

        Events.from(repo)
                .flatMap(r -> client.patch(uri + "repos/" + repo, repo))
                .errorResume(client.post(uri + "repos/" + repo, repo))
                .flatMap(r -> client.patch(uri + "packages/" + pack, pack))
                .errorResume(client.post(uri + "packages/" + repo, pack))
                .flatMap(p -> client.get(uri + "packages/" + pack + "/files", new RepositoryFiles()))
                .flatIterable(files -> {
                    RepositoryFiles completes = new RepositoryFiles();
                    completes.add(RepositoryFile.of(library.getPOM(), library.getLocalPOM()));
                    completes.add(RepositoryFile.of(library.getJar(), library.getLocalJar()));
                    completes.add(RepositoryFile.of(library.getSourceJar(), library.getLocalSourceJar()));
                    completes.add(RepositoryFile.of(library.getJavadocJar(), library.getLocalJavadocJar()));
                    completes.removeAll(files);
                    return completes;
                })
                .take(file -> Files.exists(file.localFile))
                .flatMap(file -> client.put(uri + "maven/" + pack + "/" + file.path + ";publish=1;override=1", file, file.resource()))
                .to(file -> {
                    ui.talk("Upload " + file.localFile + " to https://dl.bintray.com/" + project.getGroup() + "/maven/" + file.path + ".");
                });
    }

    /**
     * @version 2017/01/06 13:10:36
     */
    @SuppressWarnings("unused")
    private static class Repository {

        /** The repository owner. */
        public String owner;

        /** The name. (fixed) */
        public String name = "maven";

        /** The repository type. (fixed) */
        public String type = "maven";

        /** The repository description. */
        public String desc;

        /**
         * <p>
         * With owner name.
         * </p>
         * 
         * @param owner
         */
        private static Repository of(String owner) {
            Repository repo = new Repository();
            repo.owner = owner;
            repo.desc = "The " + owner + "'s Maven Repository.";
            return repo;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return owner + "/" + name;
        }
    }

    /**
     * @version 2017/01/08 15:59:24
     */
    @SuppressWarnings("unused")
    private static class Package {

        /** The owner repository. */
        private Repository repository;

        /** The name. */
        public String name;

        /** The description. */
        public String desc;

        /** The web site. */
        public String website_url;

        /** The version control system. */
        public String vcs_url;

        /** The issure traker. */
        public String issue_tracker_url;

        /** The license list. */
        public List<String> licenses = new ArrayList();

        /**
         * @param name
         * @param desc
         */
        private static Package of(Repository repo, Project project) {
            Package p = new Package();
            p.repository = repo;
            p.name = project.getLibrary().name;
            p.desc = project.getDescription();
            p.licenses.add(project.getLicense().name());
            p.website_url = project.getVersionControlSystem().uri();
            p.vcs_url = project.getVersionControlSystem().uri();
            p.issue_tracker_url = project.getVersionControlSystem().issue();
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return repository + "/" + name;
        }
    }

    /**
     * @version 2017/01/08 16:29:13
     */
    @SuppressWarnings("serial")
    private static class RepositoryFiles extends ArrayList<RepositoryFile> {
    }

    /**
     * @version 2017/01/08 16:29:23
     */
    private static class RepositoryFile {

        /** The file path. */
        public String path;

        /** The local file path. */
        public Path localFile;

        /** The file checksum. */
        public String sha1;

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(sha1);
        }

        /**
         * <p>
         * Create the resource to transfer.
         * </p>
         * 
         * @return
         */
        private TransferResource resource() {
            try {
                Project project = I.make(Project.class);
                String repository = "http://" + project.getGroup() + ".bintray.com/maven";

                return new TransferResource(repository, path, localFile.toFile(), null).setContentLength(Files.size(localFile));
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RepositoryFile == false) {
                return false;
            }
            return Objects.equals(sha1, ((RepositoryFile) obj).sha1);
        }

        /**
         * <p>
         * Create repository file.
         * </p>
         * 
         * @param path
         * @param filePath
         * @return
         */
        public static RepositoryFile of(String path, Path filePath) {
            try {
                RepositoryFile file = new RepositoryFile();
                file.path = path;
                file.localFile = filePath;
                file.sha1 = DigestUtils.shaHex(Files.readAllBytes(filePath));

                return file;
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }
}
