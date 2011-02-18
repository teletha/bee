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

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import ezunit.CleanRoom;
import ezunit.Ezunit;

/**
 * @version 2010/12/19 10:21:32
 */
public class JavaCompilerTest {

    @Rule
    public static final CleanRoom clean = new CleanRoom(Ezunit.locatePackage(JavaCompilerTest.class) + "/source1");

    @Test
    public void compile() throws Exception {
        File output = clean.locateDirectory("out");
        File sourceFile = clean.locateFile("Main.jawa");
        File classFile = new File(output, "source1/Main.class");

        assertTrue(sourceFile.exists());
        assertFalse(classFile.exists());

        JavaCompiler compiler = new JavaCompiler();
        compiler.addSourceDirectory(clean.root);
        compiler.setOutput(output);
        compiler.compile();

        assertTrue(classFile.exists());
    }
}
