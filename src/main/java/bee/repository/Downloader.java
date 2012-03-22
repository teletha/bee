/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee.repository;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bee.definition.Library;
import bee.definition.Repository;

import kiss.I;

/**
 * @version 2010/09/05 20:42:30
 */
public class Downloader {

    /** The list of artifacts to download. */
    private final List<Artifact> artifacts;

    /** The list of repository. */
    private final List<Repository> repositories;

    /** The thread pool for parallel download. */
    private final ExecutorService threads;

    /** The counter for donwloader. */
    private CountDownLatch doneSignal;

    private final List<Future<File>> results = new ArrayList<Future<File>>();

    public static final void donwload(List<Artifact> artifacts, List<Repository> repositories) {
        new Downloader(artifacts, repositories);
    }

    /**
     * Avoid construction.
     */
    private Downloader(List<Artifact> artifacts, List<Repository> repositories) {
        this.artifacts = artifacts;
        this.repositories = repositories;
        this.threads = Executors.newCachedThreadPool();
        this.doneSignal = new CountDownLatch(artifacts.size());

        for (Artifact artifact : artifacts) {
            results.add(threads.submit(new ArtifactDownloader(artifact, repositories)));
        }

        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            // TODO
        }
    }

    /**
     * @version 2010/09/13 11:42:14
     */
    private final class ArtifactDownloader implements Callable<File> {

        /** The library to download. */
        private final Artifact artifact;

        /** The list of repository. */
        private final List<Repository> repositories;

        private Library library;

        /**
         * @param artifact
         * @param repositories
         */
        private ArtifactDownloader(Artifact artifact, List<Repository> repositories) {
            this.artifact = artifact;
            this.repositories = repositories;
        }

        /**
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public File call() throws Exception {
            for (Repository repository : repositories) {
                try {
                    repository.load(artifact);
                    break;
                } catch (NoSuchArtifactException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Error e) {
                    e.printStackTrace();
                }
            }
            doneSignal.countDown();
            return null;
        }

        private ProjectDescriptor findDescriptor(Repository repository) {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) library.toExternal("-project.jar", repository).openConnection();

                if (connection.getResponseCode() == 200) {
                    Path file = library.toInternal("-project.jar");

                    // download(connection.getInputStream(), file);
                } else {

                }
            } catch (IOException e) {
                // We can't connect to the repository, try to next one.
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        private void download(InputStream input, File out) {
            OutputStream output = null;

            try {
                // try to download
                int size = 0;
                byte[] buffer = new byte[8192];

                output = new FileOutputStream(out);

                while ((size = input.read(buffer)) != -1) {
                    output.write(buffer, 0, size);
                }
            } catch (IOException e) {
                // TODO: handle exception
            } finally {
                close(input);
                close(output);
            }
        }

        /**
         * <p>
         * Transfer data from external repository to internal reopsitory.
         * </p>
         * 
         * @param external
         * @param internal
         * @throws FileNotFoundException
         * @throws IOException
         */
        private Path download(String suffix, Repository repository) {
            Path file = library.toInternal(suffix);

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {
                // connect
                connection = (HttpURLConnection) library.toExternal(suffix, repository).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    // try to download
                    int size = 0;
                    byte[] buffer = new byte[8192];

                    input = connection.getInputStream();
                    output = Files.newOutputStream(file);

                    while ((size = input.read(buffer)) != -1) {
                        output.write(buffer, 0, size);
                    }
                }
            } catch (IOException e) {
                close(output);

                // clean up
                I.delete(file);
            } finally {
                close(input);
                close(output);

                if (connection != null) {
                    connection.disconnect();
                }
            }

            // API definition
            if (Files.exists(file)) {
                return file;
            } else {
                throw new Error(library + " can't download from the following repositories: " + repositories);
            }
        }

        /**
         * <p>
         * Helper method to close resource silently.
         * </p>
         * 
         * @param closeable
         */
        private void close(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    private static final class ProjectDescriptor {

    }
}
