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

import javax.annotation.Resource;

import bee.task.Jar;

import com.sun.source.util.JavacTask;

import ezbean.I;
import ezbean.model.ClassUtil;
import ezbean.xml.XMLWriter;

/**
 * @version 2011/03/15 17:47:48
 */
@Resource
public class EclipseConfiguration {

    public static void main(String[] args) throws Exception {
        Path factory = I.locate(".factorypath");

        Path jar = I.locate("bee2.jar").toAbsolutePath();
        Path classes = I.locate("target/classes");

        Path tools = ClassUtil.getArchive(JavacTask.class);

        Jar archiver = new Jar();
        archiver.add(classes);
        archiver.pack(jar);

        XMLWriter writer = new XMLWriter(Files.newBufferedWriter(factory, I.getEncoding()));
        writer.startDocument();
        writer.start("factorypath");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", tools.toAbsolutePath().toString(), "enabled", "true", "runInBatchMode", "false");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", jar.toAbsolutePath().toString(), "enabled", "true", "runInBatchMode", "false");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", "F:\\Development\\Ezbean\\target\\ezbean-0.8.2.jar", "enabled", "true", "runInBatchMode", "false");
        writer.element("factorypathentry", "kind", "EXTJAR", "id", "F:\\Application\\Maven Repository\\asm\\asm\\3.3\\asm-3.3.jar", "enabled", "true", "runInBatchMode", "false");
        writer.end();
        writer.endDocument();
    }
}
