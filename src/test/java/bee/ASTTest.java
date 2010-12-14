/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee;

import java.io.File;
import java.io.IOException;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.JavacTask;

/**
 * @version 2010/05/26 19:02:43
 */
public class ASTTest {

    public static final void main(final String[] args) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            System.err.println("Java Compiler is not available.");
            return;
        }

        StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null);

        CompilationTask task = javac.getTask(null, fileManager, null, null, null, fileManager.getJavaFileObjects(new File("e:\\trix\\bee\\src\\main\\java\\bee\\Test.java")));

        JavacTask javacTask = (JavacTask) task;
        for (CompilationUnitTree unitTree : javacTask.parse()) {
            System.out.println("Tree for " + unitTree.getSourceFile());
            
            unitTree.accept()
            for (Tree root : unitTree.getTypeDecls()) {
                System.out.println("\t TypeDeclaration for " + root.getClass() + " : " + root);

                if (root.getKind() == Kind.CLASS) {
                    ClassTree tree = (ClassTree) root;

                    for (Tree member : tree.getMembers()) {
                        System.out.println(member + member.getKind().toString());

                        if (member.getKind() == Kind.METHOD) {
                            MethodTree method = (MethodTree) member;

                            System.out.println(member);

                            BlockTree block = method.getBody();

                            System.out.println(block);

                            for (StatementTree statement : block.getStatements()) {
                                System.out.println(statement + " " + statement.getKind());
                            }
                        }
                    }
                }
            }
        }
    }
}
