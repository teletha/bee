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
import java.nio.file.Path;
import java.util.List;

import bee.api.Command;
import bee.api.Task;
import bee.coder.FileType;
import bee.coder.StandardHeaderStyle;
import psychopath.Temporary;

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
        update(project.getSourceSet().asTemporary());
        update(project.getTestSourceSet().asTemporary());
        update(project.getProjectSourceSet().asTemporary());
    }

    /**
     * <p>
     * Update license text.
     * </p>
     * 
     * @param set
     * @throws IOException
     */
    private void update(Temporary set) throws IOException {
        set.walkFiles().to(file -> {
            Path path = file.asJavaPath();

            FileType type = FileType.of(path);

            if (type.header() == StandardHeaderStyle.Unknown) {
                ui.talk("Unknown Format ", project.getRoot().relativize(path));
            } else {
                List<String> source = file.lines(project.getEncoding()).toList();
                List<String> converted = type.header().convert(source, project.getLicense());

                if (converted != null) {
                    file.text(project.getEncoding(), converted);
                    ui.talk("Update ", project.getRoot().relativize(path));
                }
            }
        });
    }
}
