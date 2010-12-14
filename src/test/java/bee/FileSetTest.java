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

import org.junit.Test;

/**
 * @version 2010/06/14 17:43:01
 */
public class FileSetTest {

    private File base = new File("src");

    @Test
    public void iterate() throws Exception {
        int count = 0;
        FileSet set = new FileSet(base);

        for (File file : set) {
            System.out.println(file);
        }
    }
}
