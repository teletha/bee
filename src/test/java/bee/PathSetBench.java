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
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

import org.junit.Test;

import ezunit.AbstractMicroBenchmarkTest;

/**
 * @version 2011/02/19 13:42:58
 */
public class PathSetBench extends AbstractMicroBenchmarkTest {

    @Test
    public void pathset() {
        benchmark(new Callable<Integer>() {

            private PathSet set = new PathSet3(new File("").getAbsoluteFile().getParent());

            private Counter counter = new Counter();

            {
                set.include("*.java");
                // set.exclude("target/**");
                // set.exclude("src/**");
            }

            /**
             * @see java.util.concurrent.Callable#call()
             */
            @Override
            public Integer call() throws Exception {
                set.scan(counter);
                return counter.counter;
            }
        });
    }

    @Test
    public void pathmatcher() {
        benchmark(new Callable<Integer>() {

            private Path path = Paths.get(new File("").getAbsoluteFile().getParent());

            private Counter2 counter = new Counter2();

            /**
             * @see java.util.concurrent.Callable#call()
             */
            @Override
            public Integer call() throws Exception {
                Files.walkFileTree(path, counter);
                return counter.counter;
            }
        });
    }

    /**
     * @version 2011/02/19 13:48:02
     */
    private static class Counter extends SimpleFileVisitor {

        private int counter = 0;

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) throws IOException {
            counter++;
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * @version 2011/02/19 13:48:02
     */
    private static class Counter2 extends SimpleFileVisitor<Path> {

        private int counter = 0;

        private PathMatcher matcher1 = FileSystems.getDefault().getPathMatcher("glob:**.java");

        private PathMatcher matcher2 = FileSystems.getDefault().getPathMatcher("glob:**/target/**");

        private PathMatcher matcher3 = FileSystems.getDefault().getPathMatcher("glob:**/src/**");

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (matcher1.matches(file)) {
                counter++;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
