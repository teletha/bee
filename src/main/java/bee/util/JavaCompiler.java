/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import static bee.util.Inputs.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import bee.BeeOption;
import bee.Isolation;
import bee.Fail;
import bee.Platform;
import bee.UserInterface;
import bee.api.Library;
import kiss.I;
import kiss.Signal;
import kiss.Variable;
import psychopath.Directory;
import psychopath.Folder;
import psychopath.Location;
import psychopath.Locator;

public class JavaCompiler {

    /** The default java compiler. */
    private static javax.tools.JavaCompiler Javac = ToolProvider.getSystemJavaCompiler();

    /** The user interface. */
    private final UserInterface ui;

    /** The source directories. */
    private final Folder sources = Locator.folder();

    /** The source codes. */
    private final List<JavaFileObject> codes = new ArrayList();

    /** The classpath list. */
    private final List<Location> classpaths = new ArrayList();

    /** The annotation processors. */
    private final List<Processor> processors = new ArrayList();

    /** The annotation processor classes. */
    private final List<String> processorClasses = new ArrayList();

    /** The annotation processor's locations. */
    private final Set<Location> processorClassPaths = new HashSet();

    /** The annotation processor's options. */
    private final Map<String, String> processorOptions = new HashMap();

    /** The output directory. */
    private Directory output;

    /** The release version. */
    private SourceVersion releaseVersion = SourceVersion.latest();

    /** The source encoding. */
    private Charset encoding = Platform.Encoding;

    /** The deprication flag. */
    private boolean deprecation = false;

    /** The verbose flag. */
    private boolean verbose = false;

    /** The nowarn flag. */
    private boolean nowarn = false;

    /** The debug info flag. */
    private boolean debug = true;

    /** The compiler flag. */
    private boolean useECJ = false;

    /** The compiler flag. */
    private boolean compileAll = BeeOption.Cacheless.value();

    /** The error listener. */
    private DiagnosticListener<JavaFileObject> listener;

    /**
     * Create new Java compiler.
     * 
     * @param ui
     * @return
     */
    public static final JavaCompiler with(UserInterface... ui) {
        return new JavaCompiler(ui == null || ui.length != 1 ? null : ui[0]);
    }

    /**
     * Hide constructor.
     * 
     * @param ui A mesage listener.
     */
    private JavaCompiler(UserInterface ui) {
        this.ui = ui != null ? ui : I.make(UserInterface.class);
    }

    /**
     * Set the user class path, overriding the user class path in the CLASSPATH environment
     * variable. If threr is no classpath, the user class path consists of the current directory.
     * 
     * @param path A classpath to add.
     */
    public JavaCompiler addClassPath(Location path) {
        if (path != null) {
            classpaths.add(path);
        }
        return this;
    }

    /**
     * Set the user class path, overriding the user class path in the CLASSPATH environment
     * variable. If threr is no classpath, the user class path consists of the current directory.
     * 
     * @param library A classpath to add.
     */
    public JavaCompiler addClassPath(Library library) {
        if (library != null) {
            classpaths.add(library.getLocalJar());
        }
        return this;
    }

    /**
     * Set the user class path, overriding the user class path in the CLASSPATH environment
     * variable. If threr is no classpath, the user class path consists of the current directory.
     * 
     * @param libraries A list of classpaths to add.
     */
    public JavaCompiler addClassPath(Set<Library> libraries) {
        if (libraries != null) {
            for (Library library : libraries) {
                addClassPath(library);
            }
        }
        return this;
    }

    /**
     * Use all current classpath.
     */
    public JavaCompiler addCurrentClassPath() {
        for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
            addClassPath(Locator.locate(path));
        }
        return this;
    }

    /**
     * Add source code
     * 
     * @param name A class name.
     * @param code A source code to compile.
     */
    public JavaCompiler addSource(String name, String code) {
        codes.add(new Source(name, code));

        return this;
    }

    /**
     * Add the source code directory.
     * 
     * @param directory Your source code directory.
     */
    public JavaCompiler addSourceDirectory(Directory directory) {
        sources.add(directory, "**.java");

        return this;
    }

    /**
     * Add the source code directory.
     * 
     * @param directories Your source code directory.
     */
    public JavaCompiler addSourceDirectory(Signal<Directory> directories) {
        if (directories != null) {
            sources.add(directories);
        }
        return this;
    }

    /**
     * Add the specified annotation processor to compile process. This method bybpasses the default
     * discovery process.
     * <p>
     * Names of the annotation processors to run. This bypasses the default discovery process.
     * 
     * @param processor
     */
    public JavaCompiler addProcessor(Class<? extends Processor> processor) {
        if (processor != null && !processorClasses.contains(processor.getName())) {
            processorClasses.add(processor.getName());
            processorClassPaths.add(Locator.locate(processor).absolutize());
        }
        return this;
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
    public JavaCompiler addProcessor(Processor processor) {
        if (processor != null && !processors.contains(processor)) {
            processors.add(processor);
        }
        return this;
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
    public JavaCompiler addProcessorOption(String key, String value) {
        if (key != null && value != null && key.length() != 0 && value.length() != 0) {
            processorOptions.put(key, value);
        }
        return this;
    }

    /**
     * Options to pass to annotation processors. These are not interpreted by javac directly, but
     * are made available for use by individual processors.
     * 
     * @param option A key-value pair.
     */
    public JavaCompiler addProcessorOption(Entry<String, String> option) {
        if (option != null) {
            addProcessorOption(option.getKey(), option.getValue());
        }
        return this;
    }

    /**
     * Options to pass to annotation processors. These are not interpreted by javac directly, but
     * are made available for use by individual processors.
     * 
     * @param options A key-value set.
     */
    public JavaCompiler addProcessorOption(Map<String, String> options) {
        if (options != null) {
            for (Entry<String, String> entry : options.entrySet()) {
                addProcessorOption(entry.getKey(), entry.getValue());
            }
        }
        return this;
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
    public JavaCompiler setOutput(Path directory) {
        this.output = Locator.directory(directory);

        return this;
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
    public JavaCompiler setOutput(Directory directory) {
        this.output = directory;

        return this;
    }

    /**
     * Show a description of each use or override of a deprecated member or class. Without
     * deprecation, java compiler shows a summary of the source files that use or override
     * deprecated members or classes.
     * 
     * @param show
     */
    public JavaCompiler setDeprecation(boolean show) {
        this.deprecation = show;

        return this;
    }

    /**
     * Set whether you want to use the Eclipse compiler (ECJ) instead of the JDK compiler (Javac).
     * 
     * @param useECJ True to use ECJ, False to use Javac.
     * @return
     */
    public JavaCompiler setEclipseCompiler(boolean useECJ) {
        this.useECJ = useECJ;
        return this;
    }

    /**
     * Set the source file encoding name, such as EUC-JP and UTF-8. If encoding is not specified or
     * <code>null</code> is specified, the platform default converter is used.
     * 
     * @param encoding A charset to set.
     */
    public JavaCompiler setEncoding(Charset encoding) {
        if (encoding == null) {
            this.encoding = Platform.Encoding;
        } else {
            this.encoding = encoding;
        }
        return this;
    }

    /**
     * Generate all debugging information, including local variables. By default, only line number
     * and source file information is generated.
     * 
     * @param debug
     */
    public JavaCompiler setGenerateDebugInfo(boolean debug) {
        this.debug = debug;

        return this;
    }

    /**
     * Verbose output. This includes information about each class loaded and each source file
     * compiled.
     * 
     * @param verbose
     */
    public JavaCompiler setVerbose(boolean verbose) {
        this.verbose = verbose;

        return this;
    }

    /**
     * Override the location of installed extensions.
     * 
     * @param directory
     */
    public JavaCompiler setExtensionDirectory(Path directory) {
        return this;
    }

    /**
     * Disable warning messages. This has the same meaning as -Xlint:none.
     */
    public JavaCompiler setNoWarn() {
        this.nowarn = true;

        return this;
    }

    /**
     * Compile all source files.
     */
    public JavaCompiler setCompileAll() {
        this.compileAll = true;

        return this;
    }

    /**
     * Set release version.
     * 
     * @param releaseVersion
     */
    public JavaCompiler setVersion(SourceVersion releaseVersion) {
        if (releaseVersion != null) {
            this.releaseVersion = releaseVersion;
        }
        return this;
    }

    /**
     * Set compiler listener.
     * 
     * @param listener
     */
    public JavaCompiler setListener(DiagnosticListener<JavaFileObject> listener) {
        this.listener = listener;

        return this;
    }

    /**
     * Specify the directory where to place generated source files. The directory must already
     * exist; javac will not create it. If a class is part of a package, the compiler puts the
     * source file in a subdirectory reflecting the package name, creating directories as needed.
     * For example, if you specify -s C:\mysrc and the class is called com.mypackage.MyClass, then
     * the source file will be placed in C:\mysrc\com\mypackage\MyClass.java.
     * 
     * @param directory
     */
    public JavaCompiler setGeneratedSourceOutput(Directory directory) {
        return this;
    }

    /**
     * Invoke compiler with specified options.
     */
    public ClassLoader compile() {
        // Build options
        ArrayList<String> options = new ArrayList();

        // =============================================
        // Option
        // =============================================
        if (verbose) {
            options.add("-verbose");
            options.add("-XprintProcessorInfo");
            options.add("-XprintRounds");
        }

        // =============================================
        // Lint
        // =============================================
        if (deprecation) {
            options.add("-deprecation");
        }

        if (nowarn) {
            options.add("-nowarn");
        }

        // =============================================
        // Debug info in class file
        // =============================================
        if (debug) {
            options.add("-g");
        }

        // =============================================
        // Output Directory
        // =============================================
        // Create direcotry if needed.
        if (output == null) {
            output = Locator.temporaryDirectory();
        }
        output.create();

        options.add("-d");
        options.add(output.toString());

        // =============================================
        // Annotation Processing Tools
        // =============================================
        if (processors.size() == 0 && processorClasses.size() == 0) {
            options.add("-proc:none");
        } else {
            options.add("-processor");
            options.add(String.join(",", processorClasses));
            options.add("-processorpath");
            options.add(processorClassPaths.stream().map(Location::toString).collect(Collectors.joining(",")));

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
        // Release Version
        // =============================================
        options.add("--release");
        options.add(normalize(releaseVersion));

        // =============================================
        // Java Class Paths
        // =============================================
        if (classpaths.size() != 0) {
            options.add("-cp");
            options.add(classpaths.stream().map(Location::toString).collect(Collectors.joining(File.pathSeparator)));
        }

        // =============================================
        // Java Source Files
        // =============================================
        List<JavaFileObject> sources = new ArrayList(codes);

        this.sources.walkFileWithBase("**.java").to(e -> {
            if (output == null) {
                sources.add(new Source(e.ⅱ));
            } else {
                psychopath.File classFile = output.directory(e.ⅰ.relativize(e.ⅱ.parent())).file(e.ⅱ.base() + ".class");

                if (compileAll || classFile.lastModifiedMilli() < e.ⅱ.lastModifiedMilli()) {
                    sources.add(new Source(e.ⅱ));
                }
            }
        });

        options.add("-sourcepath");
        options.add(String.join(File.pathSeparator, this.sources.walkFile().map(Location::toString).toList()));

        // =============================================
        // Start Compiling
        // =============================================
        // check target source size
        if (sources.isEmpty()) {
            ui.info("Nothing to compile - all classes are up to date");
            return Thread.currentThread().getContextClassLoader();
        }

        // =============================================
        // Select Compiler
        // =============================================
        Variable<javax.tools.JavaCompiler> compiler = Variable.of(Javac);
        if (useECJ) {
            Isolation depend = Isolation.with("org.eclipse.jdt : ecj");
            compiler.set(depend.create(javax.tools.JavaCompiler.class, "org.eclipse.jdt.internal.compiler.tool.EclipseCompiler"));

            // All local variable names (including unused ones) are kept in the class file
            // for compatibility with Eclipse default setting.
            options.add("-preserveAllLocals");
        }

        // =============================================
        // Setup Compiler and Annotation Processing Tool
        // =============================================
        if (listener == null) {
            listener = new Listener();
        }

        StandardJavaFileManager manager = compiler.v.getStandardFileManager(listener, null, encoding);
        CompilationTask task = compiler.v.getTask(null, manager, listener, options, null, sources);

        if (processors.size() != 0) {
            task.setProcessors(processors);
        }

        // =============================================
        // Run Compiler
        // =============================================
        if (task.call()) {
            ui.info("Compiles " + sources.size() + " sources. (" + (useECJ ? "ECJ" : "Javac") + ")");
        } else {
            throw new Fail("Fail compiling code.");
        }

        try {
            return new InvertedClassLoader(output.absolutize().asJavaPath().toUri().toURL());
        } catch (MalformedURLException e) {
            throw I.quiet(e);
        }
    }

    /**
     * 
     */
    private class Listener implements DiagnosticListener<JavaFileObject> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            String message = diagnostic.toString();

            switch (diagnostic.getKind()) {
            case ERROR:
                ui.error(message);
                break;

            case MANDATORY_WARNING:
            case WARNING:
                ui.warn(message);
                break;

            case NOTE:
                ui.info(message);
                break;

            case OTHER:
                ui.trace(message);
                break;
            }
        }
    }

    /**
     * File or In-memory source code.
     */
    private static class Source extends SimpleJavaFileObject {

        /** The in-memory code. */
        private String code;

        /** The source file. */
        private Path file;

        /**
         * @param code
         */
        private Source(String name, String code) {
            super(URI.create("string:///" + name.replace(".", "/") + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = Objects.requireNonNull(code);
        }

        /**
         * @param file
         */
        private Source(Path file) {
            super(file.toUri(), Kind.SOURCE);
            this.file = file;
        }

        /**
         * @param file
         */
        private Source(psychopath.File file) {
            this(file.asJavaPath());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            if (file == null) {
                return code;
            } else {
                return Files.readString(file);
            }
        }
    }

    /**
     * {@link ClassLoader} with high priority.
     */
    private static class InvertedClassLoader extends URLClassLoader {

        /**
         * @param isolation
         */
        public InvertedClassLoader(URL isolation) {
            super(new URL[] {isolation});
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> clazz = findLoadedClass(name);

            if (clazz == null) {
                try {
                    clazz = findClass(name);
                } catch (ClassNotFoundException e) {
                    clazz = super.loadClass(name, resolve);
                }
            }

            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public URL getResource(String name) {
            URL url = findResource(name);
            if (url == null) {
                url = super.getResource(name);
            }
            return url;
        }
    }
}