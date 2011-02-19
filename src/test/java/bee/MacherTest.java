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
package bee;

import java.nio.file.FileSystems;
import java.nio.file.Paths;

/**
 * @version 2011/02/19 14:29:18
 */
public class MacherTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(Paths.get("F:\\Development\\Bee\\target\\clean-room\\1\\use\\01.file"));
        System.out.println(FileSystems.getDefault()
                .getPathMatcher("glob:**/use/*")
                .matches(Paths.get("F:\\Development\\Bee\\target\\clean-room\\1\\use\\01.file")));
    }
}
