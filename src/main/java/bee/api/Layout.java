/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.nio.file.Path;

import kiss.I;

/**
 * @version 2012/04/17 13:02:56
 */
public enum Layout {

    /** The source directory. */
    Source("src"),

    /** The output directory. */
    Output("target"),

    /** The project directory. */
    ProjectDirectory(Source, "project/java"),

    /** The project difinition file. */
    ProjectSource(ProjectDirectory, "Project.java"),

    /** The compiled project definition file. */
    ProjectClass(Output, "project-classes/Project.class");

    /** The relative path. */
    private final Path path;

    /**
     * <p>
     * Build location path.
     * </p>
     * 
     * @param path A relative path.
     */
    private Layout(String path) {
        this.path = I.locate(path);
    }

    /**
     * <p>
     * Build location path.
     * </p>
     * 
     * @param base A base path.
     * @param path A relative path.
     */
    private Layout(Layout base, String path) {
        this.path = base.path.resolve(path);
    }

    /**
     * <p>
     * Compute location in the specified project.
     * </p>
     * 
     * @param project A base project.
     * @return A resource location.
     */
    public Path in(Project project) {
        return from(project.getRoot());
    }

    /**
     * <p>
     * Compute location in the specified base directory.
     * </p>
     * 
     * @param base A base directory.
     * @return A resource location.
     */
    public Path from(Path base) {
        return base.relativize(path);
    }
}
