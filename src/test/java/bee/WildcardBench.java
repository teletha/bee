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

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.junit.Test;

import ezunit.AbstractMicroBenchmarkTest;

/**
 * @version 2011/02/19 14:15:17
 */
public class WildcardBench extends AbstractMicroBenchmarkTest {

    Path path = Paths.get(new File("").getAbsolutePath());

    @Test
    public void wild() throws Exception {
        benchmark(new Callable<Boolean>() {

            private Wildcard wildcard = new Wildcard("**.java");

            /**
             * @see java.util.concurrent.Callable#call()
             */
            @Override
            public Boolean call() throws Exception {
                return wildcard.match(path.toString());
            }
        });
    }

    @Test
    public void system() throws Exception {
        benchmark(new Callable<Boolean>() {

            private PathMatcher wildcard = FileSystems.getDefault().getPathMatcher("glob:*.java");

            /**
             * @see java.util.concurrent.Callable#call()
             */
            @Override
            public Boolean call() throws Exception {
                return wildcard.matches(path);
            }
        });
    }
}
