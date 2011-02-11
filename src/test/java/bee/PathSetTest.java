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

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import ezunit.ReusableRule;

/**
 * @version 2010/06/14 17:43:01
 */
public class PathSetTest {

    @Rule
    public static final MatchSet set1 = new MatchSet("01");

    @Rule
    public static final MatchSet set2 = new MatchSet("02");

    @Test
    public void all() throws Exception {
        set1.assertMatching(9);
    }

    @Test
    public void extension() throws Exception {
        set1.set.include("**.txt");
        set1.assertMatching(6);
    }

    @Test
    public void extensions() throws Exception {
        set1.set.include("**.txt", "**.file");
        set1.assertMatching(9);
    }

    @Test
    public void duplicatedFiles() throws Exception {
        set1.set.include("**.txt", "**/02.*");
        set1.assertMatching(6);
    }

    @Test
    public void directory1() throws Exception {
        set1.set.include("use**");
        set1.assertMatching(6);
    }

    @Test
    public void directory2() throws Exception {
        set1.set.include("use/**");
        set1.assertMatching(3);
    }

    @Test
    public void directoryWildcard() throws Exception {
        set2.set.include("**/a/**/a/**");
        set2.assertMatching(8, 1, 2, 3, 4, 5, 6, 9, 10);
    }

    @Test
    public void directoryDirectWithWildcard1() throws Exception {
        set2.set.include("**/b/a/**");
        set2.assertMatching(8, 5, 6, 9, 10, 11, 12, 13, 14);
    }

    @Test
    public void directoryDirectWithWildcard2() throws Exception {
        set2.set.include("**/b/b/**");
        set2.assertMatching(6, 7, 8, 13, 14, 15, 16);
    }

    @Test
    public void directoryRoot() throws Exception {
        set2.set.include("b/b/**");
        set2.assertMatching(4, 13, 14, 15, 16);
    }

    @Test
    public void directoryDouble() throws Exception {
        set2.set.include("**/b/a/**");
        set2.set.include("**/a/b/**");
        set2.assertMatching(12, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
    }

    @Test
    public void directoryA() throws Exception {
        set2.set.include("*/a/**");
        set2.assertMatching(8);
    }

    @Test
    public void both() throws Exception {
        set2.set.include("**/a/**.txt");
        set2.assertMatching(7);
    }

    /**
     * @version 2011/01/19 11:14:21
     */
    private static final class MatchSet extends ReusableRule implements FileVisitor<Path> {

        /** The target file set. */
        private PathSet set;

        private String path;

        /** The matching file counter. */
        private List<Integer> numbers = new ArrayList();

        /**
         * @see ezunit.ReusableRule#before(java.lang.reflect.Method)
         */
        @Override
        protected void before(Method method) throws Exception {
            numbers.clear();
            set = new PathSet(path);
        }

        /**
         * 
         */
        private MatchSet(String path) {
            this.path = testcaseDirectory.getAbsolutePath() + "/match/" + path;
        }

        private void assertMatching(int expected) {
            try {
                set.scan(this);

                assertEquals(expected, numbers.size());
            } finally {
                numbers.clear();
            }
        }

        private void assertMatching(int expected, int... list) {
            try {
                set.scan(this);

                assertEquals(expected, numbers.size());

                for (int i : list) {
                    assertTrue(numbers.contains(i));
                }
            } finally {
                numbers.clear();
            }
        }

        /**
         * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path path, IOException attributes) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
            System.out.println(path);

            String name = path.getName().toString();
            int index = name.lastIndexOf('.');
            Integer number = Integer.parseInt(name.substring(0, index));
            numbers.add(number);

            return FileVisitResult.CONTINUE;
        }

        /**
         * @see java.nio.file.FileVisitor#visitFileFailed(java.lang.Object, java.io.IOException)
         */
        @Override
        public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }
}
