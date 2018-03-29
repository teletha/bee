/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import static bee.util.Inputs.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import bee.Platform;
import bee.UserInterface;
import bee.api.Library;
import filer.Filer;
import kiss.I;

/**
 * @version 2018/03/29 9:26:33
 */
public class JavaCompiler {

    /** The actual java compiler. */
    private static javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /** The user interface. */
    private final UserInterface ui;

    /** The source directories. */
    private final List<PathPattern> sources = new ArrayList();

    /** The classpath list. */
    private final List<Path> classpaths = new ArrayList();

    /** The annotation processors. */
    private final List<Processor> processors = new ArrayList();

    /** The annotation processor classes. */
    private final List<String> processorClasses = new ArrayList();

    /** The annotation processor's locations. */
    private final Set<Path> processorClassPaths = new HashSet();

    /** The annotation processor's options. */
    private final Map<String, String> processorOptions = new HashMap();

    /** The output directory. */
    private Path output;

    /** The source version. */
    private SourceVersion sourceVersion = SourceVersion.latest();

    /** The target version. */
    private SourceVersion targetVersion = SourceVersion.latest();

    /** The source encoding. */
    private Charset encoding = Platform.Encoding;

    /** The deprication flag. */
    private boolean deprication = false;

    /** The verbose flag. */
    private boolean verbose = false;

    /** The nowarn flag. */
    private boolean nowarn = false;

    /**
     * <p>
     * Exposable constructor.
     * </p>
     */
    public JavaCompiler() {
        this.ui = I.make(UserInterface.class);
    }

    /**
     * <p>
     * Set the user class path, overriding the user class path in the CLASSPATH environment
     * variable. If threr is no classpath, the user class path consists of the current directory.
     * </p>
     * 
     * @param path A classpath to add.
     */
    public void addClassPath(Path path) {
        if (path != null) {
            classpaths.add(path);
        }
    }

    /**
     * <p>
     * Set the user class path, overriding the user class path in the CLASSPATH environment
     * variable. If threr is no classpath, the user class path consists of the current directory.
     * </p>
     * 
     * @param path A classpath to add.
     */
    public void addClassPath(Library library) {
        if (library != null) {
            classpaths.add(library.getLocalJar());
        }
    }

    /**
     * <p>
     * Set the user class path, overriding the user class path in the CLASSPATH environment
     * variable. If threr is no classpath, the user class path consists of the current directory.
     * </p>
     * 
     * @param path A classpath to add.
     */
    public void addClassPath(Set<Library> libraries) {
        if (libraries != null) {
            for (Library library : libraries) {
                addClassPath(library);
            }
        }
    }

    /**
     * <p>
     * Add the source code directory.
     * </p>
     * 
     * @param path A path to your source code directory.
     */
    public void addSourceDirectory(String path) {
        if (path != null) {
            addSourceDirectory(Filer.locate(path));
        }
    }

    /**
     * <p>
     * Add the source code directory.
     * </p>
     * 
     * @param directory Your source code directory.
     */
    public void addSourceDirectory(Path directory) {
        if (directory != null) {
            if (!Files.isDirectory(directory)) {
                directory = directory.getParent();
            }
            addSourceDirectory(new PathPattern(directory, "**.java"));
        }
    }

    /**
     * <p>
     * Add the source code directory.
     * </p>
     * 
     * @param outputDirectory Your source code directory.
     */
    public void addSourceDirectory(PathSet directories) {
        if (directories != null) {
            for (PathPattern pattern : directories) {
                addSourceDirectory(new PathPattern(pattern.base, pattern.mix("**.java")));
            }
        }
    }

    /**
     * <p>
     * Add the source code directory.
     * </p>
     * 
     * @param outputDirectory Your source code directory.
     */
    public void addSourceDirectory(PathPattern directories) {
        if (directories != null) {
            sources.add(directories);
        }
    }

    /**
     * <p>
     * Add the specified annotation processor to compile process. This method bybpasses the default
     * discovery process.
     * </p>
     * <p>
     * Names of the annotation processors to run. This bypasses the default discovery process.
     * </p>
     * 
     * @param processor
     */
    public void addProcessor(Class<? extends Processor> processor) {
        if (processor != null && !processorClasses.contains(processor)) {
            processorClasses.add(processor.getName());
            processorClassPaths.add(Filer.locate(processor).toAbsolutePath());
        }
    }

    /**
     * <p>
     * Add the specified annotation processor to compile process. This method bybpasses the default
     * discovery process.
     * </p>
     * <p>
     * Names of the annotation processors to run. This bypasses the default discovery process.
     * </p>
     * 
     * @param processor
     */
    public void addProcessor(Processor processor) {
        if (processor != null && !processors.contains(processor)) {
            processors.add(processor);
        }
    }

    /**
     * <p>
     * Options to pass to annotation processors. These are not interpreted by javac directly, but
     * are made available for use by individual processors.
     * </p>
     * 
     * @param key A key name.
     * @param value A passing value.
     */
    public void addProcessorOption(String key, String value) {
        if (key != null && value != null && key.length() != 0 && value.length() != 0) {
            processorOptions.put(key, value);
        }
    }

    /**
     * <p>
     * Options to pass to annotation processors. These are not interpreted by javac directly, but
     * are made available for use by individual processors.
     * </p>
     * 
     * @param key A key name.
     * @param value A passing value.
     */
    public void addProcessorOption(Entry<String, String> option) {
        if (option != null) {
            addProcessorOption(option.getKey(), option.getValue());
        }
    }

    /**
     * <p>
     * Options to pass to annotation processors. These are not interpreted by javac directly, but
     * are made available for use by individual processors.
     * </p>
     * 
     * @param options A key-value set.
     */
    public void addProcessorOption(Map<String, String> options) {
        if (options != null) {
            for (Entry<String, String> entry : options.entrySet()) {
                addProcessorOption(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * <p>
     * Set the destination directory for class files. If a class is part of a package, javac puts
     * the class file in a subdirectory reflecting the package name, creating directories as needed.
     * For example, if you specify -d c:\myclasses and the class is called com.mypackage.MyClass,
     * then the class file is called c:\myclasses\com\mypackage\MyClass.class.
     * </p>
     * <p>
     * If you don't use this method, this compiler use in-memory storage for compiled classes.
     * </p>
     * <p>
     * Note that the directory specified by this method is not automatically added to your user
     * class path.
     * </p>
     * 
     * @param directory Your output directory. <code>null</code> value will reset to default.
     */
    public void setOutput(Path directory) {
        this.output = directory;
    }

    /**
     * <p>
     * Show a description of each use or override of a deprecated member or class. Without
     * deprecation, java compiler shows a summary of the source files that use or override
     * deprecated members or classes.
     * </p>
     * 
     * @param show
     */
    public void setDeprecation(boolean show) {
        this.deprication = show;
    }

    /**
     * <p>
     * Set the source file encoding name, such as EUC-JP and UTF-8. If encoding is not specified or
     * <code>null</code> is specified, the platform default converter is used.
     * </p>
     * 
     * @param encoding A charset to set.
     */
    public void setEncoding(Charset encoding) {
        if (encoding == null) {
            this.encoding = Platform.Encoding;
        } else {
            this.encoding = encoding;
        }
    }

    /**
     * <p>
     * Generate all debugging information, including local variables. By default, only line number
     * and source file information is generated.
     * </p>
     * 
     * @param generate
     */
    public void setGenerateDebugInfo(boolean generate) {

    }

    /**
     * <p>
     * Verbose output. This includes information about each class loaded and each source file
     * compiled.
     * </p>
     * 
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * <p>
     * Override the location of installed extensions.
     * </p>
     * 
     * @param directory
     */
    public void setExtensionDirectory(Path directory) {

    }

    /**
     * <p>
     * Disable warning messages. This has the same meaning as -Xlint:none.
     * </p>
     */
    public void setNoWarn() {
        this.nowarn = true;
    }

    /**
     * <p>
     * Set source version and target version for compiling.
     * </p>
     * 
     * @param sourceVersion
     * @param targetVersion
     */
    public void setVersion(SourceVersion sourceVersion, SourceVersion targetVersion) {
        if (sourceVersion != null) {
            this.sourceVersion = sourceVersion;
        }

        if (targetVersion != null) {
            this.targetVersion = targetVersion;
        }
    }

    /**
     * <p>
     * Specify the directory where to place generated source files. The directory must already
     * exist; javac will not create it. If a class is part of a package, the compiler puts the
     * source file in a subdirectory reflecting the package name, creating directories as needed.
     * For example, if you specify -s C:\mysrc and the class is called com.mypackage.MyClass, then
     * the source file will be placed in C:\mysrc\com\mypackage\MyClass.java.
     * </p>
     * 
     * @param directory
     */
    public void setGeneratedSourceDirectory(File directory) {

    }

    /**
     * <p>
     * Invoke compiler with specified options.
     * </p>
     */
    public ClassLoader compile() {
        // Build options
        ArrayList<String> options = new ArrayList();

        try {
            // =============================================
            // Option
            // =============================================
            if (verbose) {
                options.add("-verbose");
            }

            // =============================================
            // Lint
            // =============================================
            if (deprication) {
                options.add("-Xlint:deprecation");
            }

            if (nowarn) {
                options.add("-Xlint:none");
            }

            // =============================================
            // Output Directory
            // =============================================
            if (output != null) {
                // Create direcotry if needed.
                if (Files.notExists(output)) {
                    Files.createDirectories(output);
                }

                // Output must be not file but directory.
                if (!Files.isDirectory(output)) {
                    output = output.getParent();
                }

                options.add("-d");
                options.add(output.toAbsolutePath().toString());
            }

            // =============================================
            // Annotation Processing Tools
            // =============================================
            if (processors.size() == 0 && processorClasses.size() == 0) {
                options.add("-proc:none");
            } else {
                options.add("-processor");
                options.add(I.join(",", processorClasses));
                options.add("-processorpath");
                options.add(I.join(",", processorClassPaths));

                for (Entry<String, String> entry : processorOptions.entrySet()) {
                    options.add("-A" + entry.getKey() + '=' + entry.getValue());
                }
            }

            // =============================================
            // Source encoding
            // =============================================
            options.add("-encoding");
            options.add(encoding.displayName());

            // =============================================
            // Source and Target Version
            // =============================================
            options.add("-source");
            options.add(normalize(sourceVersion));
            options.add("-target");
            options.add(normalize(targetVersion));

            // =============================================
            // Java Class Paths
            // =============================================
            if (classpaths.size() != 0) {
                options.add("-cp");
                options.add(I.join(File.pathSeparator, classpaths));
            }

            // =============================================
            // Java Source Files
            // =============================================
            List<File> sources = new ArrayList();

            for (PathPattern directory : this.sources) {
                for (Path sourceFile : directory.list()) {
                    Path classsFile = output.resolve(directory.base.relativize(sourceFile.getParent()))
                            .resolve(Paths.getName(sourceFile) + ".class");

                    if (Paths.getLastModified(classsFile) < Paths.getLastModified(sourceFile)) {
                        sources.add(sourceFile.toFile());
                    }
                }
            }

            options.add("-sourcepath");
            options.add(I.join(File.pathSeparator, this.sources));

            // check compiling source size
            if (sources.isEmpty()) {
                ui.talk("Nothing to compile - all classes are up to date");

                return Thread.currentThread().getContextClassLoader();
            }

            // Invocation
            ErrorListener listener = new ErrorListener();
            Manager manager = new Manager(compiler.getStandardFileManager(listener, Locale.getDefault(), StandardCharsets.UTF_8));

            CompilationTask task = compiler.getTask(null, manager, listener, options, null, manager.getJavaFileObjectsFromFiles(sources));

            // =============================================
            // Annotation Processing Tools
            // =============================================
            if (processors.size() != 0) {
                task.setProcessors(processors);
            }

            boolean result = task.call();

            if (result) {
                ui.talk("Compile " + sources.size() + " sources.");
            } else {
                throw new Error("Compile is fail.");
            }

            return manager;
        } catch (IOException e) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw I.quiet(e);
        }
    }

    /**
     * @version 2010/12/18 11:51:46
     */
    private class ErrorListener implements DiagnosticListener<JavaFileObject> {

        /**
         * @see javax.tools.DiagnosticListener#report(javax.tools.Diagnostic)
         */
        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            switch (diagnostic.getKind()) {
            case ERROR:
                ui.error(diagnostic.toString());
                break;

            case MANDATORY_WARNING:
            case WARNING:
                ui.warn(diagnostic.toString());
                break;

            case NOTE:
            case OTHER:
                ui.talk(diagnostic.toString());
                break;
            }
        }
    }

    /**
     * @version 2010/12/19 10:20:30
     */
    private class Manager extends SecureClassLoader implements StandardJavaFileManager {

        /** The actual file manager. */
        private final StandardJavaFileManager manager;

        /** The mapping for defined classes. */
        private final Map<String, Class> classes = new HashMap();

        /**
         * @param fileManager
         */
        public Manager(StandardJavaFileManager manager) {
            this.manager = manager;
        }

        /**
         * @see javax.tools.StandardJavaFileManager#getJavaFileObjectsFromFiles(java.lang.Iterable)
         */
        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
            return manager.getJavaFileObjectsFromFiles(files);
        }

        /**
         * @see javax.tools.StandardJavaFileManager#getJavaFileObjects(java.io.File[])
         */
        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
            return manager.getJavaFileObjects(files);
        }

        /**
         * @see javax.tools.StandardJavaFileManager#getJavaFileObjectsFromStrings(java.lang.Iterable)
         */
        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
            return manager.getJavaFileObjectsFromStrings(names);
        }

        /**
         * @see javax.tools.StandardJavaFileManager#getJavaFileObjects(java.lang.String[])
         */
        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
            return manager.getJavaFileObjects(names);
        }

        /**
         * @see javax.tools.StandardJavaFileManager#setLocation(javax.tools.JavaFileManager.Location,
         *      java.lang.Iterable)
         */
        @Override
        public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
            manager.setLocation(location, path);
        }

        /**
         * @see javax.tools.StandardJavaFileManager#getLocation(javax.tools.JavaFileManager.Location)
         */
        @Override
        public Iterable<? extends File> getLocation(Location location) {
            return manager.getLocation(location);
        }

        /**
         * @see javax.tools.OptionChecker#isSupportedOption(java.lang.String)
         */
        @Override
        public int isSupportedOption(String option) {
            return manager.isSupportedOption(option);
        }

        /**
         * @see javax.tools.JavaFileManager#list(javax.tools.JavaFileManager.Location,
         *      java.lang.String, java.util.Set, boolean)
         */
        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
            return manager.list(location, packageName, kinds, recurse);
        }

        /**
         * @see javax.tools.JavaFileManager#inferBinaryName(javax.tools.JavaFileManager.Location,
         *      javax.tools.JavaFileObject)
         */
        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            return manager.inferBinaryName(location, file);
        }

        /**
         * @see javax.tools.StandardJavaFileManager#isSameFile(javax.tools.FileObject,
         *      javax.tools.FileObject)
         */
        @Override
        public boolean isSameFile(FileObject a, FileObject b) {
            return manager.isSameFile(a, b);
        }

        /**
         * @see javax.tools.JavaFileManager#handleOption(java.lang.String, java.util.Iterator)
         */
        @Override
        public boolean handleOption(String current, Iterator<String> remaining) {
            return manager.handleOption(current, remaining);
        }

        /**
         * @see javax.tools.JavaFileManager#hasLocation(javax.tools.JavaFileManager.Location)
         */
        @Override
        public boolean hasLocation(Location location) {
            return manager.hasLocation(location);
        }

        /**
         * @see javax.tools.JavaFileManager#getClassLoader(javax.tools.JavaFileManager.Location)
         */
        @Override
        public ClassLoader getClassLoader(Location location) {
            return this;
        }

        /**
         * @see javax.tools.JavaFileManager#getJavaFileForInput(javax.tools.JavaFileManager.Location,
         *      java.lang.String, javax.tools.JavaFileObject.Kind)
         */
        @Override
        public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
            return manager.getJavaFileForInput(location, className, kind);
        }

        /**
         * @see javax.tools.JavaFileManager#getJavaFileForOutput(javax.tools.JavaFileManager.Location,
         *      java.lang.String, javax.tools.JavaFileObject.Kind, javax.tools.FileObject)
         */
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
            if (output == null) {
                return new Bytecode(className);
            } else {
                return manager.getJavaFileForOutput(location, className, kind, sibling);
            }
        }

        /**
         * @see javax.tools.JavaFileManager#getFileForInput(javax.tools.JavaFileManager.Location,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
            return manager.getFileForInput(location, packageName, relativeName);
        }

        /**
         * @see javax.tools.JavaFileManager#getFileForOutput(javax.tools.JavaFileManager.Location,
         *      java.lang.String, java.lang.String, javax.tools.FileObject)
         */
        @Override
        public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling)
                throws IOException {
            return manager.getFileForOutput(location, packageName, relativeName, sibling);
        }

        /**
         * @see javax.tools.JavaFileManager#flush()
         */
        @Override
        public void flush() throws IOException {
            manager.flush();

            // clean up
            classes.clear();
        }

        /**
         * @see javax.tools.JavaFileManager#close()
         */
        @Override
        public void close() throws IOException {
            manager.close();

            // clean up
            classes.clear();
        }

        /**
         * @see java.lang.ClassLoader#findClass(java.lang.String)
         */
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Class clazz = classes.get(name);

            return clazz != null ? clazz : super.findClass(name);
        }

        /**
         * @version 2010/12/18 22:45:13
         */
        private class Bytecode extends ByteArrayOutputStream implements JavaFileObject {

            /** The class name. */
            private final String name;

            /**
             * @param name
             */
            public Bytecode(String name) {
                this.name = name;
            }

            /**
             * @see javax.tools.FileObject#toUri()
             */
            @Override
            public URI toUri() {
                return null;
            }

            /**
             * @see javax.tools.FileObject#getName()
             */
            @Override
            public String getName() {
                return null;
            }

            /**
             * @see javax.tools.FileObject#openInputStream()
             */
            @Override
            public InputStream openInputStream() throws IOException {
                return null;
            }

            /**
             * @see javax.tools.FileObject#openReader(boolean)
             */
            @Override
            public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
                return null;
            }

            /**
             * @see javax.tools.FileObject#getCharContent(boolean)
             */
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return null;
            }

            /**
             * @see javax.tools.FileObject#openWriter()
             */
            @Override
            public Writer openWriter() throws IOException {
                return null;
            }

            /**
             * @see javax.tools.FileObject#getLastModified()
             */
            @Override
            public long getLastModified() {
                return 0;
            }

            /**
             * @see javax.tools.FileObject#delete()
             */
            @Override
            public boolean delete() {
                return false;
            }

            /**
             * @see javax.tools.JavaFileObject#getKind()
             */
            @Override
            public Kind getKind() {
                return null;
            }

            /**
             * @see javax.tools.JavaFileObject#isNameCompatible(java.lang.String,
             *      javax.tools.JavaFileObject.Kind)
             */
            @Override
            public boolean isNameCompatible(String simpleName, Kind kind) {
                return false;
            }

            /**
             * @see javax.tools.JavaFileObject#getNestingKind()
             */
            @Override
            public NestingKind getNestingKind() {
                return null;
            }

            /**
             * @see javax.tools.JavaFileObject#getAccessLevel()
             */
            @Override
            public Modifier getAccessLevel() {
                return null;
            }

            /**
             * @see javax.tools.SimpleJavaFileObject#openOutputStream()
             */
            @Override
            public OutputStream openOutputStream() throws IOException {
                return this;
            }

            /**
             * @see java.io.OutputStream#close()
             */
            @Override
            public void close() throws IOException {
                // close stream
                super.close();

                // define class
                byte[] bytes = toByteArray();

                classes.put(name, defineClass(name, bytes, 0, bytes.length));
            }
        }
    }
}
