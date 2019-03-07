/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
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
import bee.api.Github;
import bee.api.Library;
import bee.api.Project;
import bee.api.Task;
import bee.util.Config;
import bee.util.Config.Description;
import bee.util.RESTClient;
import kiss.I;
import psychopath.File;

/**
 * @version 2017/01/16 14:47:15
 */
public class Bintray extends Task {

    /** The bintray domain. */
    private static final String uri = "https://api.bintray.com/";

    @Command("Deploy products to Bintray repository.")
    public void deploy() {
        require(Install::project);

        Account account = Config.user(Account.class);

        RESTClient client = new RESTClient(account.name(), account.key());
        Library library = project.getLibrary();
        Repository repo = Repository.of(project.exactVersionControlSystem());
        Package pack = Package.of(repo, project);

        I.signal(repo)
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
                .to(I.NoOP);
    }

    /**
     * @version 2017/01/10 10:02:07
     */
    @Description("Bintray Account Setting")
    public static interface Account {

        @Description("Bintray account name")
        public String name();

        @Description("Bintray API key (See https://bintray.com/profile/edit)")
        public String key();
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
         * If set to true then the repo’s metadata will be automatically signed with Bintray GPG
         * key.
         */
        public boolean gpg_sign_metadata = false;

        /**
         * If set to true then the repo’s files will be automatically signed with Bintray GPG key.
         */
        public boolean gpg_sign_files = false;

        /**
         * If set to true then the repo’s metadata and files will be signed automatically with the
         * owner’s GPG key. this flag cannot be set true simultaneously with either of the bintray
         * key falgs (files or metadata). this flag can be set true only if the repo’s owner
         * supplied a private (and public) GPG key on his bintray profile.
         */
        public boolean gpg_use_owner_key = false;

        /**
         * <p>
         * With owner name.
         * </p>
         * 
         * @param owner
         */
        private static Repository of(Github project) {
            Repository repo = new Repository();
            repo.owner = project.owner;
            repo.desc = "The " + project.owner + "'s Maven Repository.";

            return repo;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return owner.replaceAll("\\.", "-").toLowerCase() + "/" + name.toLowerCase();
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

        // /** The github repository. */
        // public String github_repo;
        //
        // /** The github release note. */
        // public String github_release_notes_file = "RELEASE.txt";

        /** The state. */
        public boolean public_download_numbers = false;

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
            p.website_url = project.exactVersionControlSystem().uri();
            p.vcs_url = project.exactVersionControlSystem().uri();
            p.issue_tracker_url = project.exactVersionControlSystem().issue();

            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return repository + "/" + name.toLowerCase();
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

                return new TransferResource(null, repository, path, localFile.toFile(), null).setContentLength(Files.size(localFile));
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(sha1);
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
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return path + "[" + localFile + "]";
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
        public static RepositoryFile of(String path, File filePath) {
            RepositoryFile file = new RepositoryFile();
            file.path = path;
            file.localFile = filePath.asJavaPath();
            file.sha1 = DigestUtils.sha1Hex(filePath.bytes());
            return file;
        }
    }
}
