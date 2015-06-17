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

import bee.api.Command;
import bee.api.Task;
import bee.util.FileType;
import bee.util.HeaderType;
import bee.util.PathSet;
import bee.util.StandardHeaderType;
import kiss.I;

/**
 * @version 2015/06/14 17:50:58
 */
public class License extends Task {

    /** The file encoding. */
    protected Charset encoding = project.getEncoding();

    /** The license text. */
    protected List<String> license = project.getLicense().text();

    /**
     * <p>
     * Update license text.
     * </p>
     * 
     * @throws IOException
     */
    @Command
    public void update() throws IOException {
        update(project.getSources());
        update(project.getTestSources());
        update(project.getProjectSources());
    }

    /**
     * <p>
     * Update license text.
     * </p>
     * 
     * @param set
     * @throws IOException
     */
    private void update(PathSet set) throws IOException {
        for (Path path : set.getFiles()) {
            FileType type = FileType.of(path);

            if (type.header() == StandardHeaderType.Unknown) {
                ui.talk("Unknown Format ", project.getRoot().relativize(path));
            } else {
                ui.talk("Update ", project.getRoot().relativize(path));
                List<String> lines = Files.readAllLines(path, encoding);
                Files.write(path, convert(lines, type.header()), encoding);
            }
        }
    }

    /**
     * <p>
     * Read license file.
     * </p>
     * 
     * @param path A path to license file.
     */
    protected void license(String path) {
        license(I.locate(path));
    }

    /**
     * <p>
     * Read license file.
     * </p>
     * 
     * @param path A path to license file.
     */
    protected void license(Path path) {
        if (path != null && Files.exists(path)) {
            try {
                license.clear();
                license.addAll(Files.readAllLines(path, encoding));
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * @param sources
     * @param header
     */
    public List<String> convert(List<String> sources, HeaderType header) {
        int first = -1;
        int end = -1;

        for (int i = 0; i < sources.size(); i++) {
            String line = sources.get(i);

            if (header.isFirstHeaderLine(line)) {
                first = i;
            } else if (first != -1 && header.isEndHeaderLine(line)) {
                end = i;
                break;
            }
        }

        // remove existing header
        if (first != -1 && end != -1) {
            for (int i = end; first <= i; i--) {
                sources.remove(i);
            }
        }

        if (first == -1) {
            first = 0;
        }

        // add specified header
        sources.addAll(first, header.text(license));

        return sources;
    }
}
