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
import java.util.List;

import bee.api.Command;
import bee.api.Task;
import bee.coder.FileType;
import bee.coder.StandardHeaderStyle;
import kiss.Signal;
import psychopath.Directory;

public class License extends Task {

    /**
     * <p>
     * Update license text.
     * </p>
     * 
     * @throws IOException
     */
    @Command("Write license header comment.")
    public void update() throws IOException {
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
    private void update(Signal<Directory> set) throws IOException {
        set.flatMap(dir -> dir.walkFiles()).to(file -> {
            FileType type = FileType.of(file);

            if (type.header() == StandardHeaderStyle.Unknown) {
                ui.talk("Unknown Format ", project.getRoot().relativize(file));
            } else {
                List<String> source = file.lines(project.getEncoding()).toList();
                List<String> converted = type.header().convert(source, project.getLicense());

                if (converted != null) {
                    file.text(project.getEncoding(), converted);
                    ui.talk("Update ", project.getRoot().relativize(file));
                }
            }
        });
    }
}
