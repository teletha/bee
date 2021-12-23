/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.util.List;

import bee.Task;
import bee.api.Command;
import bee.coder.FileType;
import bee.coder.StandardHeaderStyle;
import kiss.Signal;
import psychopath.Directory;

public class License extends Task {

    /**
     * Update license text.
     */
    @Command("Write license header comment.")
    public void update() {
        update(project.getSourceSet());
        update(project.getTestSourceSet());
        update(project.getProjectSourceSet());
    }

    /**
     * Update license text.
     * 
     * @param set
     */
    private void update(Signal<Directory> set) {
        set.flatMap(dir -> dir.walkFile()).to(file -> {
            FileType type = FileType.of(file);

            if (type.header() == StandardHeaderStyle.Unknown) {
                ui.info("Unknown Format ", project.getRoot().relativize(file));
            } else {
                List<String> source = file.lines(project.getEncoding()).toList();
                List<String> converted = type.header().convert(source, project.license());

                if (converted != null) {
                    file.text(project.getEncoding(), converted);
                    ui.info("Update ", project.getRoot().relativize(file));
                }
            }
        });
    }
}