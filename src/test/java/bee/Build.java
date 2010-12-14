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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import ezbean.io.FileSystem;
import ezbean.xml.XMLWriter;

/**
 * @version 2010/05/14 20:28:54
 */
public class Build {

    public static void main(String[] args) throws Exception {
        // create jar
        File directory = new File("target");
        File classes = new File(directory, "classes");
        File jar1 = new File(directory, "bee1.jar");
        File jar2 = new File(directory, "bee2.jar");
        File jar3 = new File(directory, "bee3.jar");
        File jar = null;

        if (jar1.exists()) {
            jar = jar2;
            FileSystem.delete(jar1);
            FileSystem.delete(jar3);
        } else if (jar2.exists()) {
            jar = jar3;
            FileSystem.delete(jar2);
            FileSystem.delete(jar1);
        } else {
            jar = jar1;
            FileSystem.delete(jar2);
            FileSystem.delete(jar3);
        }

        Output stream = new Output(new BufferedOutputStream(new FileOutputStream(jar)));

        dig(classes, stream);
        stream.finish();
        stream.close2();

        // create factorypath
        XMLWriter out = new XMLWriter(new FileWriter(new File(".factorypath")));
        out.startDocument();
        out.start("factorypath");
        out.start("factorypathentry", "kind", "WKSPJAR", "id", "/Bee/target/" + jar.getName(), "enabled", "true", "runInBatchMode", "false");
        out.end();

        out.start("factorypathentry", "kind", "WKSPJAR", "id", "/Ezbean/target/ezbean-0.7.jar", "enabled", "true", "runInBatchMode", "false");
        out.end();

        out.start("factorypathentry", "kind", "EXTJAR", "id", "E:\\Java\\Repository\\asm\\asm\\3.2\\asm-3.2.jar", "enabled", "true", "runInBatchMode", "false");
        out.end();

        out.start("factorypathentry", "kind", "EXTJAR", "id", "E:\\Java\\Java\\lib\\tools.jar", "enabled", "true", "runInBatchMode", "false");
        out.end();
        out.end();
        out.endDocument();
    }

    private static void dig(File directory, JarOutputStream stream) throws Exception {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                JarEntry entry = new JarEntry(file.getPath().substring(15).replace('\\', '/').concat("/"));
                entry.setMethod(JarEntry.STORED);
                entry.setSize(0);
                entry.setCrc(0);
                stream.putNextEntry(entry);
                stream.closeEntry();

                dig(file, stream);
            } else {
                JarEntry entry = new JarEntry(file.getPath().substring(15).replace('\\', '/'));
                stream.putNextEntry(entry);
                FileSystem.copy(new FileInputStream(file), stream);
                stream.closeEntry();
            }
        }
    }

    private static class Output extends JarOutputStream {

        /**
         * @param out
         * @throws IOException
         */
        public Output(OutputStream out) throws IOException {
            super(out);
        }

        /**
         * @see java.util.zip.ZipOutputStream#close()
         */
        @Override
        public void close() throws IOException {

        }

        public void close2() throws IOException {
            super.close();
        }
    }
}
