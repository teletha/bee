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

import java.nio.file.Files;
import java.nio.file.Path;

import kiss.I;
import kiss.model.ClassUtil;
import kiss.xml.XMLWriter;
import bee.task.JarArchiver;

import com.sun.source.util.JavacTask;

/**
 * @version 2012/01/26 2:55:56
 */
public class EclipseConfigurationBuilder {

    public static void main(String[] args) throws Exception {
        Path factory = I.locate(".factorypath");

        Path jar = I.locate("bee2.jar").toAbsolutePath();
        Path classes = I.locate("target/classes");

        Path tools = ClassUtil.getArchive(JavacTask.class);

        JarArchiver archiver = new JarArchiver();
        archiver.add(classes);
        archiver.pack(jar);

        XMLWriter writer = new XMLWriter(Files.newBufferedWriter(factory, I.$encoding));
        writer.startDocument();
        writer.start("factorypath");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", tools.toAbsolutePath().toString(), "enabled", "true", "runInBatchMode", "false");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", jar.toAbsolutePath().toString(), "enabled", "true", "runInBatchMode", "false");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", "F:\\Development\\Sinobu\\target\\sinobu-0.9.1.jar", "enabled", "true", "runInBatchMode", "false");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", "F:\\Application\\Maven Repository\\asm\\asm\\4.0\\asm-4.0.jar", "enabled", "true", "runInBatchMode", "false");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", "F:\\Application\\Maven Repository\\junit\\junit\\4.10\\junit-4.10.jar", "enabled", "true", "runInBatchMode", "false");
        writer.end();
        writer.endDocument();
    }
}
