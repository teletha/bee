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
import java.io.FileReader;

import kiss.I;

import org.junit.Test;
import org.xml.sax.InputSource;

/**
 * @version 2010/05/20 18:09:51
 */
public class POMTest {

    @Test
    public void read() throws Exception {
        POM pom = new POM();

        I.parse(new InputSource(new FileReader(new File("pom.xml"))), pom);

        System.out.println(pom.getGroupId() + "   " + pom.getArtifactId() + "   " + pom.getVersion() + "   " + pom.getDescription());
    }
}
