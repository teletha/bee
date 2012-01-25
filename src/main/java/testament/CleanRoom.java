/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import static java.nio.file.StandardCopyOption.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static testament.Ezunit.*;
import static testament.UnsafeUtility.*;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import kiss.I;

/**
 * <p>
 * The environmental rule for test that depends on file system.
 * </p>
 * 
 * @version 2011/04/06 22:47:29
 */
public class CleanRoom extends Sandbox {

    /** The counter for instances. */
    private static final AtomicInteger counter = new AtomicInteger();

    /** The root bioclean room for tests which are related with file system. */
    private static final Path clean = Paths.get("target/clean-room");

    /** The temporary bioclean room for this instance which are related with file system. */
    public final Path root = clean.resolve(String.valueOf(counter.incrementAndGet()));

    /** The host directory for test. */
    private final Path host;

    /** The clean room monitor. */
    private final Monitor monitor = new Monitor(root);

    /** The all used archives. */
    private final Set<FileSystem> archives = new HashSet();

    /**
     * Create a clean room for the current directory.
     */
    public CleanRoom() {
        this((Path) null);
    }

    /**
     * Create a clean room for the directory that the specified path indicates.
     * 
     * @param relativePath A relative location path you want to use.
     */
    public CleanRoom(String relativePath) {
        this(I.locate(relativePath));
    }

    /**
     * Create a clean room for the directory that the specified path indicates.
     * 
     * @param path A relative location path you want to use.
     */
    public CleanRoom(Path path) {
        Path directory = locatePackage(speculateInstantiator());

        if (path != null) {
            if (path.isAbsolute()) {
                directory = path;

                if (Files.notExists(directory)) {
                    try {
                        Files.createDirectories(directory);
                    } catch (IOException e) {
                        throw I.quiet(e);
                    }
                }
            } else {
                directory = directory.resolve(path);
            }
        }

        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();
        }

        this.host = directory;

        // access control
        writable(false, host);
    }

    /**
     * <p>
     * Assume platform encoding.
     * </p>
     * 
     * @param charset Your exepcted charcter encoding.
     */
    public void assume(Charset charset) {
        assumeThat(Charset.defaultCharset(), is(charset));
    }

    /**
     * <p>
     * Assume platform encoding.
     * </p>
     * 
     * @param charset Your exepcted charcter encoding.
     */
    public void assume(String charset) {
        assumeThat(Charset.defaultCharset(), is(Charset.forName(charset)));
    }

    /**
     * <p>
     * Locate a present resource file which is assured that the spcified file exists.
     * </p>
     * 
     * @param name A file name.
     * @return A located present file.
     */
    public Path locateFile(String name) {
        return locate(name, true, true);
    }

    /**
     * <p>
     * Locate a present resource file which is assured that the spcified file exists as archive.
     * </p>
     * 
     * @param name A file name.
     * @return A located present archive file.
     */
    public Path locateArchive(String name) {
        return locateArchive(locateFile(name));
    }

    /**
     * <p>
     * Locate a present resource file which is assured that the spcified file exists as archive.
     * </p>
     * 
     * @param name A file name.
     * @return A located present archive file.
     */
    public Path locateArchive(Path path) {
        try {
            FileSystem system = FileSystems.newFileSystem(path, null);

            // register archive to dispose in cleanup phase
            archives.add(system);

            // API definition
            return system.getPath("/");
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Locate a present resource directory which is assured that the specified directory exists.
     * </p>
     * 
     * @param name A directory name.
     * @return A located present directory.
     */
    public Path locateDirectory(String name) {
        return locate(name, true, false);
    }

    /**
     * <p>
     * Locate an absent resource which is assured that the specified resource doesn't exists.
     * </p>
     * 
     * @param name A resource name.
     * @return A located absent file system resource.
     */
    public Path locateAbsent(String name) {
        return locate(name, false, false);
    }

    /**
     * Helper method to locate file in clean room.
     * 
     * @param path
     * @return
     */
    private Path locate(String path, boolean isPresent, boolean isFile) {
        // null check
        if (path == null) {
            path = "";
        }

        // locate virtual file in the clean room
        Path virtual = root.resolve(path);

        // create virtual file if needed
        if (isPresent) {
            if (isFile) {
                // create parent directory
                try {
                    Files.createDirectories(virtual.getParent());
                } catch (IOException e) {
                    throw I.quiet(e);
                }

                // create requested file
                try {
                    Files.createFile(virtual);
                } catch (FileAlreadyExistsException e) {
                    // ignore
                } catch (IOException e) {
                    throw I.quiet(e);
                }
            } else {
                // create requested directory
                try {
                    Files.createDirectories(virtual);
                } catch (IOException e) {
                    throw I.quiet(e);
                }
            }
        }

        // validate file state
        assertEquals(Files.exists(virtual), isPresent);
        assertEquals(Files.isRegularFile(virtual), isFile);

        // API definition
        return virtual;
    }

    /**
     * @see testament.ReusableRule#before(java.lang.reflect.Method)
     */
    @Override
    protected void before(Method method) throws Exception {
        super.before(method);

        // start monitoring clean room
        use(monitor);

        // renew clean room for this test if needed
        if (monitor.modified) {
            // clean up all resources
            sweep(root);

            // copy all resources newly
            copyDirectory(host, root);

            // reset
            monitor.modified = false;
        }
    }

    /**
     * @see testament.Sandbox#after(java.lang.reflect.Method)
     */
    @Override
    protected void after(Method method) {
        for (FileSystem system : archives) {
            try {
                system.close();
            } catch (IOException e) {
                catchError(e);
            }
        }
        super.after(method);
    }

    /**
     * @see testament.ReusableRule#afterClass()
     */
    @Override
    protected void afterClass() {
        // Dispose clean room actually.
        sweep(root);

        // Delete root directory of clean room.
        try {
            Files.delete(clean);
        } catch (DirectoryNotEmptyException e) {
            // CleanRoom is used by other testcase, So we can't delete.
        } catch (IOException e) {
            catchError(e);
        }

        // delegate
        super.afterClass();
    }

    /**
     * <p>
     * Helper method to copy all resource in the specified directory.
     * </p>
     * 
     * @param input A input directory.
     * @param output An output directory.
     * @throws IOException I/O error.
     */
    private void copyDirectory(Path input, Path output) throws IOException {
        Files.createDirectories(output);

        for (Path path : Files.newDirectoryStream(input, monitor)) {
            if (Files.isDirectory(path)) {
                copyDirectory(path, output.resolve(path.getFileName()));
            } else {
                copyFile(path, output.resolve(path.getFileName()));
            }
        }
    }

    /**
     * <p>
     * Copy a input file to an output file. You can override this method to change file copy
     * behavior.
     * </p>
     * 
     * @param input A input file. (not directory)
     * @param output An output file. (not directory)
     * @throws IOException I/O error.
     */
    protected void copyFile(Path input, Path output) throws IOException {
        Files.copy(input, output, COPY_ATTRIBUTES);
    }

    /**
     * Helper method to delete files.
     * 
     * @param path
     */
    private void sweep(Path path) {
        if (Files.exists(path)) {
            try {
                Files.walkFileTree(path, new Sweeper());
            } catch (IOException e) {
                catchError(e);
            }
        }
    }

    /**
     * @version 2011/03/10 9:35:05
     */
    private static final class Sweeper extends SimpleFileVisitor<Path> {

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#postVisitDirectory(java.lang.Object,
         *      java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * @version 2010/02/13 13:23:22
     */
    private class Monitor extends Security implements Filter<Path> {

        /** The path prefix. */
        private final String prefix;

        /** The flag for file resource modification. */
        private boolean modified = true;

        /**
         * @param root
         */
        public Monitor(Path root) {
            this.prefix = root.toString();
        }

        /**
         * @see testament.Sandbox.Security#checkDelete(java.lang.String)
         */
        @Override
        public void checkDelete(String file) {
            if (!modified && file.startsWith(prefix)) {
                modified = true;
            }
        }

        /**
         * @see testament.Sandbox.Security#checkWrite(java.io.FileDescriptor)
         */
        @Override
        public void checkWrite(FileDescriptor fd) {
            if (!modified && fd.toString().startsWith(prefix)) {
                modified = true;
            }
        }

        /**
         * @see testament.Sandbox.Security#checkWrite(java.lang.String)
         */
        @Override
        public void checkWrite(String file) {
            if (!modified && file.startsWith(prefix)) {
                modified = true;
            }
        }

        /**
         * @see java.nio.file.DirectoryStream.Filter#accept(java.lang.Object)
         */
        public boolean accept(Path path) throws IOException {
            String name = path.getFileName().toString();

            return !name.equals("package-info.html") && !name.endsWith(".class");
        }
    }
}
