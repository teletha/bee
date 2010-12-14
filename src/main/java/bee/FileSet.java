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
import java.io.FilenameFilter;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ezbean.I;

/**
 * @version 2010/05/20 2:11:23
 */
public class FileSet implements Iterable<File> {

    /** The base file. */
    private final File base;

    /** The actual filter set. */
    private Set<FilenameFilter> filters = new CopyOnWriteArraySet();

    private FilenameFilter filter;

    public FileSet(String base) {
        this(I.locate(base));
    }

    public FileSet(File base) {
        this.base = base;
    }

    public FileSet include(String... patterns) {
        return this;
    }

    public FileSet exclude(String... patterns) {
        return this;
    }

    public FileSet excludeDefault(boolean exclusion) {
        return this;
    }

    public void copyTo(File dist) {

    }

    public void moveTo(File dist) {
        copyTo(dist);
        delete();
    }

    public void delete() {

    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<File> iterator() {
        return new FileIterator();
    }

    /**
     * @version 2010/06/10 16:45:21
     */
    private final class FileIterator implements Iterator<File> {

        /** The directory queue. */
        private final ArrayDeque<File> directories = new ArrayDeque();

        /** The file collection. */
        private File[] files = new File[0];

        private int position = -1;

        private FileIterator() {
            directories.add(base);
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            if (0 <= position) {
                File file = files[position];

                if (file.isFile()) {
                    return true;
                } else {
                    directories.add(file);

                    position--;

                    return hasNext();
                }
            } else {
                File directory = directories.pollLast();

                if (directory != null) {
                    files = directory.listFiles();

                    if (files == null) {
                        return false;
                    }
                    position = files.length - 1;

                    return hasNext();
                } else {
                    return false;
                }
            }
        }

        /**
         * @see java.util.Iterator#next()
         */
        @Override
        public File next() {
            return files[position--];
        }

        /**
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @version 2010/05/20 2:16:54
     */
    private static final class Wildcard implements FilenameFilter {

        /** The actual pattern. */
        private final String pattern;

        /**
         * @param pattern
         */
        private Wildcard(String pattern) {
            this.pattern = pattern;
        }

        /**
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        @Override
        public boolean accept(File dir, String name) {
            return false;
        }
    }

    /**
     * @version 2010/05/20 2:15:39
     */
    private static final class Not implements FilenameFilter {

        /** The delegated filter. */
        private final FilenameFilter delegate;

        /**
         * @param delegate
         */
        private Not(FilenameFilter delegate) {
            this.delegate = delegate;
        }

        /**
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        @Override
        public boolean accept(File dir, String name) {
            return !delegate.accept(dir, name);
        }
    }
}
