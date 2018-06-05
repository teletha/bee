/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import bee.api.Command;
import bee.api.Task;
import bee.coder.FileType;
import bee.coder.StandardHeaderStyle;
import kiss.I;
import kiss.Signal;
import psychopath.Directory;

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
    @Command("Write license header comment.")
    public void update() {
        update(project.getSourceSet());
        update(project.getTestSourceSet());
        update(project.getProjectSourceSet());
    }

    /**
     * <p>
     * Update license text.
     * </p>
     * 
     * @param set
     * @throws IOException
     */
    private void update(Signal<Directory> set) {
        set.flatMap(Directory::walkFiles).to(file -> {
            try {
                Path path = file.asPath();
                FileType type = FileType.of(path);

                if (type.header() == StandardHeaderStyle.Unknown) {
                    ui.talk("Unknown Format ", project.getRoot().relativize(path));
                } else {
                    List<String> source = Files.readAllLines(path, project.getEncoding());
                    List<String> converted = type.header().convert(source, project.getLicense());

                    if (converted != null) {
                        Files.write(path, converted, project.getEncoding());
                        ui.talk("Update ", project.getRoot().relativize(path));
                    }
                }
            } catch (IOException e) {
                throw I.quiet(e);
            }
        });
    }
}
