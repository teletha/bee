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
package bee.project;

import static org.junit.Assert.*;

import org.junit.Test;

import bee.project.Version;

/**
 * @version 2010/05/21 10:37:04
 */
public class VersionTest {

    @Test
    public void noParse() {
        Version version = new Version(1, 4, 2, "test");
        assertEquals(1, version.major);
        assertEquals(4, version.minor);
        assertEquals(2, version.increment);
        assertEquals("test", version.identifier);
        assertEquals("1.4.2-test", version.toString());
    }

    @Test
    public void noParseNoIncrement() {
        Version version = new Version(1, 0, -1, "test");
        assertEquals(1, version.major);
        assertEquals(0, version.minor);
        assertEquals(0, version.increment);
        assertEquals("test", version.identifier);
        assertEquals("1.0-test", version.toString());
    }

    @Test
    public void noParseNoIdentifier() {
        Version version = new Version(1, 0, 0, null);
        assertEquals(1, version.major);
        assertEquals(0, version.minor);
        assertEquals(0, version.increment);
        assertEquals(null, version.identifier);
        assertEquals("1.0", version.toString());
    }

    @Test
    public void parse() throws Exception {
        Version version = new Version("1.2.3-test");
        assertEquals(1, version.major);
        assertEquals(2, version.minor);
        assertEquals(3, version.increment);
        assertEquals("test", version.identifier);
        assertEquals("1.2.3-test", version.toString());
    }

    @Test
    public void parseNoIncrement() throws Exception {
        Version version = new Version("1.2-test");
        assertEquals(1, version.major);
        assertEquals(2, version.minor);
        assertEquals(0, version.increment);
        assertEquals("test", version.identifier);
        assertEquals("1.2-test", version.toString());
    }

    @Test
    public void parseNoIdentifier() {
        Version version = new Version("1.0.0");
        assertEquals(1, version.major);
        assertEquals(0, version.minor);
        assertEquals(0, version.increment);
        assertEquals(null, version.identifier);
        assertEquals("1.0", version.toString());
    }
}
