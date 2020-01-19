/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc;

import static javax.tools.StandardLocation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public abstract class DocTool<Self extends DocTool> implements DiagnosticListener<JavaFileObject> {

    /** Guilty Accessor. */
    public static DocTrees DocUtils;

    /** Guilty Accessor. */
    public static Elements ElementUtils;

    /** Guilty Accessor. */
    public static Types TypeUtils;

    /** The input directories. */
    private final List<Path> sources = new ArrayList();

    /** The output directory. */
    private Path output = Path.of("docs");

    /**
     * Hide constructor.
     */
    protected DocTool() {
    }

    /**
     * Exact the source directories.
     * 
     * @return
     */
    public final List<Path> sources() {
        return sources;
    }

    /**
     * Set source directory.
     * 
     * @param sourceDirectories A list of paths to source directory.
     * @return Chainable API.
     */
    public final Self sources(String... sourceDirectories) {
        return sources(Arrays.stream(sourceDirectories).map(Path::of).collect(Collectors.toList()));
    }

    /**
     * Set source directory.
     * 
     * @param sourceDirectories A list of paths to source directory.
     * @return Chainable API.
     */
    public final Self sources(Path... sourceDirectories) {
        return sources(List.of(sourceDirectories));
    }

    /**
     * Set source directory.
     * 
     * @param sourceDirectories A list of paths to source directory.
     * @return Chainable API.
     */
    public final Self sources(List<Path> sourceDirectories) {
        if (sourceDirectories != null) {
            for (Path source : sourceDirectories) {
                if (source != null) {
                    this.sources.add(source);
                }
            }
        }
        return (Self) this;
    }

    /**
     * Exact the output directory.
     * 
     * @return
     */
    public final Path output() {
        if (Files.notExists(output)) {
            try {
                Files.createDirectories(output);
            } catch (IOException e) {
                throw new IllegalStateException("Can't create output directory. [" + output + "]");
            }
        } else if (Files.isDirectory(output) == false) {
            throw new IllegalArgumentException("The output directory is NOT directory. [" + output + "]");
        }
        return output;
    }

    /**
     * Set output directory.
     * 
     * @param outputDirectory A path to output directory.
     * @return Chainable API.
     */
    public final Self output(String outputDirectory) {
        return output(Path.of(outputDirectory));
    }

    /**
     * Set output directory.
     * 
     * @param outputDirectory A path to output directory.
     * @return Chainable API.
     */
    public final Self output(Path outputDirectory) {
        if (outputDirectory != null) {
            this.output = outputDirectory;
        }
        return (Self) this;
    }

    /**
     * Build documents.
     */
    public final void build() {
        synchronized (DocTool.class) {
            self = this;
            DocumentationTool tool = ToolProvider.getSystemDocumentationTool();

            try (StandardJavaFileManager manager = tool.getStandardFileManager(this, Locale.getDefault(), Charset.defaultCharset())) {
                manager.setLocationFromPaths(SOURCE_PATH, sources);
                manager.setLocationFromPaths(Location.DOCUMENTATION_OUTPUT, List.of(output()));

                Iterable<? extends JavaFileObject> units = manager.list(SOURCE_PATH, "", Set.of(Kind.SOURCE), true);

                if (tool.getTask(null, manager, this, Internal.class, List.of(), units).call()) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        System.out.println(diagnostic);
    }

    /**
     * Find all package names in the source directory.
     * 
     * @return
     */
    protected final Set<String> findSourcePackages() {
        // collect internal package names
        Set<String> packages = new HashSet();

        for (Path source : sources) {
            try (Stream<Path> paths = Files.walk(source)) {
                paths.filter(path -> Files.isDirectory(path)).forEach(path -> {
                    packages.add(source.relativize(path).toString().replace(File.separatorChar, '.'));
                });
            } catch (Exception e) {
                // though
            }
        }

        return packages;
    }

    /**
     * Initialization phase.
     */
    protected abstract void initialize();

    /**
     * Process a class or interface program element. Provides access to information about the type
     * and its members. Note that an enum type is a kind of class and an annotation type is a kind
     * of interface.
     * 
     * @param root A class or interface program element root.
     */
    protected abstract void process(TypeElement root);

    /**
     * Process a package program element. Provides access to information about the package and its
     * members.
     * 
     * @param root A package program element root.
     */
    protected abstract void process(PackageElement root);

    /**
     * Process a module program element. Provides access to information about the module, its
     * directives, and its members.
     * 
     * @param root A module program element root.
     */
    protected abstract void process(ModuleElement root);

    /**
     * Completion phase.
     */
    protected abstract void complete();

    /** Dirty Access */
    private static DocTool self;

    /**
     * <h>DONT USE THIS CLASS</h>
     * <p>
     * It is a Doclet for internal use, but it is public because it cannot be made private due to
     * the specifications of the documentation tool.
     * </p>
     */
    public static class Internal implements Doclet {

        DocTrees trees;

        /**
         * {@inheritDoc}
         */
        @Override
        public final void init(Locale locale, Reporter reporter) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final boolean run(DocletEnvironment env) {
            DocUtils = env.getDocTrees();
            ElementUtils = env.getElementUtils();
            TypeUtils = env.getTypeUtils();

            try {
                self.initialize();

                for (Element element : env.getSpecifiedElements()) {
                    switch (element.getKind()) {
                    case MODULE:
                        self.process((ModuleElement) element);
                        break;

                    case PACKAGE:
                        self.process((PackageElement) element);
                        break;

                    default:
                        self.process((TypeElement) element);
                        break;
                    }
                }
            } finally {
                self.complete();
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final String getName() {
            return getClass().getSimpleName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final Set<? extends Option> getSupportedOptions() {
            return Set.of();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }
}