/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import bee.compiler.JavaCompiler;
import bee.definition.Scope;

/**
 * @version 2012/03/28 9:58:39
 */
public class Test extends Task {

    @Command(defaults = true)
    public void execute() {
        // compile test codes
        JavaCompiler compiler = new JavaCompiler();
        compiler.addClassPath(project.getClasses());
        compiler.addClassPath(project.getDependency(Scope.Test));
        compiler.addSourceDirectory(project.getTestSources());
        compiler.setOutput(project.getTestClasses().resolveSibling("aaa"));
        compiler.compile();
    }
}
