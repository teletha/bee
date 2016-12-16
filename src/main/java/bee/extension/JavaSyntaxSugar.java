/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.extension;

import java.nio.file.Path;
import java.util.Set;

import bee.api.Command;
import bee.api.Library;
import bee.api.Scope;
import bee.api.Task;

/**
 * @version 2016/12/16 14:08:35
 */
public class JavaSyntaxSugar extends Task {

    Set<Class> includes;

    @Command(value = "List up extension methods to use in this project.")
    public void extensionMethod() {
        fromProject();
    }

    /**
     * <p>
     * Search extensions from this project's (main and test) sources and its dependencies.
     * </p>
     */
    protected final void fromProject() {
        fromSource();
        fromTestSource();
    }

    /**
     * <p>
     * Search extensions from this project's sources.
     * </p>
     */
    protected final void fromSource() {
        from(project.getClasses());
    }

    /**
     * <p>
     * Search extensions from this project's test sources.
     * </p>
     */
    protected final void fromTestSource() {
        from(project.getTestClasses());
    }

    /**
     * <p>
     * Search extensions from this project's runtime dependencies.
     * </p>
     */
    protected final void fromDependencies() {
        for (Library library : project.getDependency(Scope.Runtime)) {
            from(library);
        }
    }

    /**
     * <p>
     * Search extension method from the specified library.
     * </p>
     * 
     * @param library
     */
    protected final void from(Library library) {
        from(library.getJar());
    }

    /**
     * <p>
     * Search extension method from the specified classes.
     * </p>
     * 
     * @param extensionClases
     */
    protected final void from(Class... extensionClases) {
    }

    /**
     * <p>
     * Search extensions from the specified path.
     * </p>
     */
    protected final void from(Path directory) {

    }
}
