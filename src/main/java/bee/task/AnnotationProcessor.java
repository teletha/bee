/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import bee.api.Project;
import kiss.I;
import psychopath.Directory;

/**
 * @version 2017/01/26 11:30:32
 */
public class AnnotationProcessor implements Processor {

    /** The message notifier. */
    private Messager notifier;

    /** The file utility. */
    private Filer filer;

    /** The utility. */
    private Elements util;

    /** The type utility. */
    private Types types;

    /** The options. */
    private Map<String, String> options;

    /** The project related data. */
    private ProjectInfo info;

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedOptions() {
        return Collections.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ProcessingEnvironment environment) {
        this.notifier = environment.getMessager();
        this.filer = environment.getFiler();
        this.util = environment.getElementUtils();
        this.types = environment.getTypeUtils();
        this.options = environment.getOptions();
        this.info = I.read(options.get(ProjectInfo.class.getName()), I.make(ProjectInfo.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        try {
            for (TypeElement annotationType : annotations) {
                for (Element element : round.getElementsAnnotatedWith(annotationType)) {
                    Class annotationClass = Class.forName(annotationType.toString());
                    AnnotationValidator validator = find(annotationClass);

                    if (validator != null) {
                        validator.initialize(element, notifier, filer, util, types, info);

                        validator.validate(element.getAnnotation(annotationClass));
                    }
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptyList();
    }

    /**
     * <p>
     * Search {@link AnnotationValidator}.
     * </p>
     * 
     * @param annotation
     * @return
     */
    protected AnnotationValidator find(Class annotation) {
        return I.find(AnnotationValidator.class, annotation);
    }

    /**
     * @version 2017/04/24 11:16:20
     */
    static class ProjectInfo implements Entry<String, String> {

        /** The project root path. */
        private Path root;

        /** The source directories. */
        private List<Path> sources;

        /**
         * @param project
         */
        ProjectInfo(Project project) {
            setRoot(project.getRoot());

            this.sources = project.getSourceSet().map(Directory::asPath).toList();
        }

        /**
         * Get the projectRoot property of this {@link AnnotationProcessor.ProjectInfo}.
         * 
         * @return The projectRoot property.
         */
        public Path getRoot() {
            return root;
        }

        /**
         * Set the projectRoot property of this {@link AnnotationProcessor.ProjectInfo}.
         * 
         * @param projectRoot The projectRoot value to set.
         */
        void setRoot(Path projectRoot) {
            this.root = projectRoot;
        }

        /**
         * Get the sources property of this {@link AnnotationProcessor.ProjectInfo}.
         * 
         * @return The sources property.
         */
        public List<Path> getSources() {
            return sources;
        }

        /**
         * Set the sources property of this {@link AnnotationProcessor.ProjectInfo}.
         * 
         * @param sources The sources value to set.
         */
        void setSources(List<Path> sources) {
            this.sources = sources;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            I.write(this, builder);

            return builder.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getKey() {
            return ProjectInfo.class.getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getValue() {
            return toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String setValue(String value) {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error();
        }
    }
}
