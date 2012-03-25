/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package demo.util;

import java.io.PrintStream;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

/**
 * @version 2012/03/25 1:05:25
 */
public class ConsoleDependencyGraphDumper implements DependencyVisitor {

    private PrintStream out;

    private String currentIndent = "";

    public ConsoleDependencyGraphDumper() {
        this(null);
    }

    public ConsoleDependencyGraphDumper(PrintStream out) {
        this.out = (out != null) ? out : System.out;
    }

    public boolean visitEnter(DependencyNode node) {
        out.println(currentIndent + node);
        if (currentIndent.length() <= 0) {
            currentIndent = "+- ";
        } else {
            currentIndent = "| " + currentIndent;
        }
        return true;
    }

    public boolean visitLeave(DependencyNode node) {
        // currentIndent = currentIndent.substring(3, currentIndent.length());
        return true;
    }

}