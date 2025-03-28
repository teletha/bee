/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.util.List;
import java.util.function.Predicate;

import bee.Task;
import bee.api.Command;
import bee.coder.FileType;
import bee.coder.StandardHeaderStyle;
import kiss.I;
import kiss.Signal;
import psychopath.Directory;
import psychopath.File;

public interface License extends Task<License.Config> {

    /**
     * Update license text.
     */
    @Command("Write license header comment.")
    default void update() {
        int[] counter = {0, 0};
        update(project().getSourceSet(), counter);
        update(project().getTestSourceSet(), counter);
        update(project().getProjectSourceSet(), counter);

        ui().info("The license statement has been updated.");
        ui().info("Updated : ", counter[0], "\tUnknown : ", counter[1]);
    }

    /**
     * Update license text.
     * 
     * @param set
     */
    private void update(Signal<Directory> set, int[] counter) {
        Config conf = config();

        set.flatMap(Directory::walkFile).skip(conf.exclude).to(file -> {
            FileType type = FileType.of(file);

            if (type.header() == StandardHeaderStyle.Unknown) {
                ui().trace("Unknown Format ", project().getRoot().relativize(file));
                counter[1]++;
            } else {
                List<String> source = file.lines(project().getEncoding()).toList();
                List<String> converted = type.header().convert(source, project().license());

                if (converted != null) {
                    file.text(project().getEncoding(), converted);
                    ui().trace("Update ", project().getRoot().relativize(file));
                    counter[0]++;
                }
            }
        });
    }

    class Config {
        /** Specify files to be excluded from the update process. */
        public Predicate<File> exclude = I::reject;
    }
}