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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * @version 2010/11/23 22:20:42
 */
public class InternalCompiler {

    /**
     * @version 2010/11/23 22:25:17
     */
    private static class ClassFile extends SimpleJavaFileObject {

        private ByteArrayOutputStream output = new ByteArrayOutputStream(8092);

        /**
         * @param name
         */
        private ClassFile(String name) {
            super(URI.create("string:///" + name.replace('.', '/') + ".class"), Kind.CLASS);
        }

        /**
         * @see javax.tools.SimpleJavaFileObject#openOutputStream()
         */
        @Override
        public OutputStream openOutputStream() throws IOException {
            return output;
        }
    }

}
