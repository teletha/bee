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
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;

import ezbean.I;
import ezunit.ReusableRule;

/**
 * @version 2010/05/26 21:50:25
 */
public class AST extends ReusableRule {

    /** The java compiler. */
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /** The file manager. */
    private final StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null);

    /** The source file. */
    private final CompilationUnitTree source;

    /**
     * @param clazz
     */
    public AST(Class clazz) {
        String path = "src/main/java/" + clazz.getName().replace('.', '/') + ".java";
        JavacTask task = (JavacTask) compiler.getTask(null, manager, null, null, null, manager.getJavaFileObjects(new File(path)));

        try {
            for (CompilationUnitTree tree : task.parse()) {

            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }
}
