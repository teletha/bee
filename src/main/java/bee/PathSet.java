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

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * @version 2011/03/13 23:52:56
 */
public class PathSet {

    /** The base directory. */
    private final Path base;

    /** The patterns. */
    private final ArrayList<String> patterns = new ArrayList();

    /**
     * @param base
     */
    public PathSet(Path base, String... patterns) {
        this.base = base;

        for (String pattern : patterns) {
            this.patterns.add(pattern);
        }
    }
}
