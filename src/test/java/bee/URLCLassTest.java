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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @version 2010/05/20 17:35:10
 */
public class URLCLassTest {

    public static void main(String[] args) throws Exception {
        URLClassLoader classLoader = new URLClassLoader(new URL[] {new URL("http://repo1.maven.org/maven2/aislib/aislib/0.5.2/aislib-0.5.2.jar")});

        Class clazz = classLoader.loadClass("pl.aislib.util.Pair");
        System.out.println(clazz);

        clazz = Thread.currentThread().getContextClassLoader().loadClass("pl.aislib.util.Pair");
        System.out.println(clazz);
    }
}
