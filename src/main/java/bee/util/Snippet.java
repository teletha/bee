/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.javadoc.Javadoc;

import bee.api.Repository;

public class Snippet {

    public final String name;

    public final String code;

    public final String comment;

    /**
     * @param code
     */
    private Snippet(String[] lines, MethodDeclaration method) {
        this.name = method.getNameAsString();
        this.code = formatCode(lines, method.getBody().get());
        this.comment = method.getJavadoc().map(Javadoc::toText).orElse("").strip();
    }

    /**
     * @param block
     * @return
     */
    private String formatCode(String[] lines, BlockStmt block) {
        Position begin = block.getBegin().get();
        Position end = block.getEnd().get();
        StringJoiner join = new StringJoiner("\n");
        for (int i = begin.line; i < end.line - 1; i++) {
            join.add(lines[i]);
        }
        return join.toString().stripIndent();
    }

    /**
     * Parse source code.
     * 
     * @param source
     * @param annotationFQCN
     * @return
     */
    public static List<Snippet> parse(String source, String annotationFQCN) {
        return parse(false, source, annotationFQCN);
    }

    /**
     * Parse source code.
     * 
     * @param source
     * @param annotationFQCN
     * @return
     */
    static List<Snippet> parse(boolean test, String source, String annotationFQCN) {
        if (!test) Repository.require("com.github.javaparser", "javaparser-core");

        List<Snippet> snippets = new ArrayList();

        String[] lines = source.split("\\r?\\n");
        StaticJavaParser.getConfiguration().setLanguageLevel(LanguageLevel.JAVA_17);
        StaticJavaParser.getConfiguration().setLexicalPreservationEnabled(true);

        CompilationUnit root = StaticJavaParser.parse(source);
        List<MethodDeclaration> methods = root.findAll(MethodDeclaration.class);

        for (MethodDeclaration method : methods) {
            if (method.isAnnotationPresent(annotationFQCN)) {
                Snippet snippet = new Snippet(lines, method);

                snippets.add(snippet);
            }
        }

        return snippets;
    }
}
