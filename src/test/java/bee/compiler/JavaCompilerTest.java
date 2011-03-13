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
package bee.compiler;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import ezunit.CleanRoom;

/**
 * @version 2010/12/19 10:21:32
 */
public class JavaCompilerTest {

    @Rule
    public static final CleanRoom clean = new CleanRoom("source1");

    @Test
    public void compile() throws Exception {
        Path output = clean.locateDirectory("out");
        Path sourceFile = clean.locateFile("Main.jawa");
        Path classFile = output.resolve("source1/Main.class");

        assertTrue(Files.exists(sourceFile));
        assertTrue(Files.notExists(classFile));

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(clean.root);
        compiler.setOutput(output);
        compiler.compile();

        assertTrue(Files.exists(classFile));
    }
}
