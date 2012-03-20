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
package bee.task;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import kiss.I;

/**
 * <p>
 * Jar file is unwraped for fat style jar.
 * </p>
 * 
 * @version 2012/01/26 16:26:58
 */
public class JarArchiver extends ZipArchiver {

    /**
     * @see bee.task.ZipArchiver#add(java.nio.file.Path, java.lang.String[])
     */
    @Override
    public void add(Path base, String... patterns) {
        if (Files.isRegularFile(base) && base.getFileName().toString().endsWith(".jar")) {
            try {
                // use ZipFS
                base = FileSystems.newFileSystem(base, ClassLoader.getSystemClassLoader()).getPath("/");
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
        super.add(base, patterns);
    }
}
