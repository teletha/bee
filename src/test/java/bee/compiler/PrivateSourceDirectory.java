/*
 * Copyright (C) 2011 Nameless Production Committee.
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

import static testament.Ezunit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import kiss.I;

import org.junit.Rule;

import testament.CleanRoom;
import testament.PrivateModule;

/**
 * @version 2011/03/13 15:53:47
 */
public class PrivateSourceDirectory extends CleanRoom {

    @Rule
    public static final PrivateModule module = new PrivateModule();

    /** The source directory. */
    final Path input;

    /** The bytecode directory. */
    final Path output;

    /** The package name to remove. */
    private final String packageName;

    /**
     * @param clazz
     */
    public PrivateSourceDirectory(String type) {
        super(I.locate("src/test/java/" + getCaller().getPackage().getName().replace('.', '/') + "/" + type)
                .toAbsolutePath());
        this.input = root;
        this.output = locateDirectory("out");
        this.packageName = getCaller().getPackage().getName().replace('.', '/') + '/' + type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyFile(Path input, Path output) throws IOException {
        output = root.resolve(packageName).resolve(root.relativize(output));

        Files.createDirectories(output.getParent());
        Files.copy(input, output);
    }

    /**
     * <p>
     * Locate a source file of the specified class.
     * </p>
     * 
     * @param clazz A target class.
     * @return A source code location.
     */
    public Path locateSourceCode(Class clazz) {
        return input.resolve(clazz.getName().replace('.', '/').concat(".java"));
    }

    /**
     * <p>
     * Locate a source file of the specified class.
     * </p>
     * 
     * @param clazz A target class.
     * @return A source code location.
     */
    public Path locateByteCode(Class clazz) {
        return output.resolve(clazz.getName().replace('.', '/').concat(".class"));
    }
}
