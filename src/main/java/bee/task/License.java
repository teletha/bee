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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import bee.api.Command;
import bee.api.Task;
import bee.util.FileType;
import bee.util.PathSet;
import bee.util.StandardHeaderStyle;

/**
 * @version 2015/06/14 17:50:58
 */
public class License extends Task {

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

            if (type.header() == StandardHeaderStyle.Unknown) {
                ui.talk("Unknown Format ", project.getRoot().relativize(path));
            } else {
                ui.talk("Update ", project.getRoot().relativize(path));
                List<String> lines = Files.readAllLines(path, project.getEncoding());
                Files.write(path, type.header().convert(lines, project.getLicense()), project.getEncoding());
            }
        }
    }
}
