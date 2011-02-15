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
 * @version 2011/02/15 15:48:47
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
    public void include() throws Exception {
        set1.set.include("**.txt");
        set1.assertMatching(6);
    }

    @Test
    public void includes() throws Exception {
        set1.set.include("**.txt", "**.file");
        set1.assertMatching(9);
    }

    @Test
    public void includeDuplicatedFiles() throws Exception {
        set1.set.include("**.txt", "02.*");
        set1.assertMatching(6);
    }

    @Test
    public void excludeDirectory() throws Exception {
        set1.set.exclude("use/**");
        set1.assertMatching(6);
    }

    @Test
    public void excludeDirectoryWildcard() throws Exception {
        set1.set.exclude("use*/**");
        set1.assertMatching(3);
    }

    @Test
    public void excludeFile() throws Exception {
        set1.set.exclude("**01.file");
        set1.assertMatching(6);
    }

    /**
     * @version 2011/02/15 15:48:53
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

        /**
         * <p>
         * Assert the count of the matching files.
         * </p>
         * 
         * @param expected
         */
        private void assertMatching(int expected) {
            try {
                set.scan(this);

                assertEquals(expected, numbers.size());
            } finally {
                numbers.clear();
            }
        }

        /**
         * <p>
         * Assert the count of the matching files.
         * </p>
         * 
         * @param expected
         */
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
