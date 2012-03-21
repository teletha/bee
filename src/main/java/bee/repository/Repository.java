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

import static kiss.Element.*;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import kiss.I;

/**
 * @version 2010/09/05 20:52:29
 */
public class Repository {

    /** The local repository. */
    public static final Path Local;

    /** The list of repositories. */
    static final List<Repository> builtin = new ArrayList<Repository>();

    /** The bee repository directory. */
    private static File repository;

    // built-in repositories
    static {
        // setup local repository
        Local = searchLocalRepository();

        // setup built-in external repositories
        // builtin.add(new MavenRepository("http://repo1.maven.org/maven2/"));
    }

    /**
     * <p>
     * Search maven home directory.
     * </p>
     * 
     * @return
     */
    private static Path searchMavenHome() {
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String path : entry.getValue().split(File.pathSeparator)) {
                    Path mvn = I.locate(path).resolve("mvn");

                    if (Files.exists(mvn)) {
                        return mvn.getParent().getParent();
                    }
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * Search maven local respository.
     * </p>
     * 
     * @return
     */
    private static Path searchLocalRepository() {
        Path local = Paths.get(System.getProperty("user.home"), ".m2");
        Path home = searchMavenHome();

        if (home == null) {
            // maven is not found
            return local;
        } else {
            // maven is here
            Path conf = home.resolve("conf/settings.xml");

            if (Files.exists(conf)) {
                String path = $(conf).find("localRepository").text();

                if (path.length() != 0) {
                    return I.locate(path);
                }
            }

            // user custom local repository is not found
            return local;
        }
    }

    /** The identified url. */
    public final URL url;

    /** The identified path. */
    public final Path path;

    /**
     * @param url
     */
    protected Repository(URL url) {
        this.url = url;
        this.path = I.locate(url);
    }

    public void load(Artifact artifact) throws NoSuchArtifactException {
        try {
            copy(new URL(url, artifact.toPath(".pom")), artifact.locateProjectDescriptor());

            analyze(artifact.locateProjectDescriptor());

            copy(new URL(url, artifact.toPath(".jar")), artifact.locate());

        } catch (MalformedURLException e) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error(e);
        }
    }

    private void analyze(File projectFile) {

    }

    /**
     * <p>
     * Copy library resource.
     * </p>
     * 
     * @param from
     * @param to
     * @throws NoSuchArtifactException
     * @throws IOError
     */
    protected void copy(URL from, File to) throws NoSuchArtifactException, IOError {
        try {
            URLConnection connection = from.openConnection();

            InputStream input = null;
            OutputStream output = null;

            try {
                // try to download
                int size = 0;
                byte[] buffer = new byte[8192];

                // create parent directory
                to.getParentFile().mkdirs();

                input = connection.getInputStream();
                output = new FileOutputStream(to);

                while ((size = input.read(buffer)) != -1) {
                    output.write(buffer, 0, size);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new IOError(e);
            } finally {
                close(input);
                close(output);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new NoSuchArtifactException();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
    }

    /**
     * <p>
     * Return location of the local repository.
     * </p>
     * 
     * @return Location of the local repository.
     */
    public static final File getLocation() {
        return repository;
    }

    /**
     * <p>
     * Set location of the local repository.
     * </p>
     * 
     * @param directory Location of the local repository.
     */
    public static final void setLocation(File directory) {
        if (directory != null) {
            if (directory.isFile()) {
                directory = directory.getParentFile();
            }

            repository = directory;
        }
    }
}
