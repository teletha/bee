/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import bee.Platform;
import bee.api.Command;
import bee.api.Task;

/**
 * @version 2015/06/14 17:50:58
 */
public class License extends Task {

    /** The file encoding. */
    protected static Charset encoding = Platform.Encoding;

    /**
     * <p>
     * Update license text.
     * </p>
     * 
     * @throws IOException
     */
    @Command
    public void update() throws IOException {
        Path license = project.getRoot().resolve("license.txt");
        Path path = project.getSources("java").getFiles().get(0);

        Java java = new Java();
        java.analyze(path);

        List<String> readAllLines = Files.readAllLines(path, encoding);

        for (String string : readAllLines) {
            ui.talk(string);
        }
    }

    /**
     * @version 2015/06/14 18:05:40
     */
    public static abstract class Analyzer {

        public abstract boolean isStart(String line);

        public abstract String prefix();

        public abstract boolean isEnd(String line);

        public abstract String[] extension();

        void analyze(Path file) throws IOException {
            int lisencePosition = -1;
            List<String> lines = Files.readAllLines(file, encoding);

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                if (isStart(line)) {
                    lisencePosition = i;
                } else if (isEnd(line)) {
                    lisencePosition = -1;
                } else if (lisencePosition != -1) {
                    // skip
                } else {

                }
            }
            for (String line : lines) {
                line = line.trim();

                if (isStart(line)) {

                }
            }
        }
    }

    /**
     * @version 2015/06/14 18:06:21
     */
    private static class Java extends Analyzer {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isStart(String line) {
            return line.startsWith("/*");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String prefix() {
            return " * ";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEnd(String line) {
            return line.endsWith(" */");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] extension() {
            return new String[] {"java"};
        }
    }
}
