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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import bee.Task;
import bee.TaskInfo;
import bee.api.Command;
import kiss.I;

public interface IDE extends Task {

    /**
     * Create idea's project file.
     */
    @Command(value = "Generate configuration files.", defaults = true)
    default void create() {
        task(IDESupport::create);

        ui().info("Generate IDE configuration files.");
    }

    /**
     * Delete idea's project file.
     */
    @Command("Delete configuration files.")
    default void delete() {
        task(IDESupport::delete);

        ui().info("Delete IDE configuration files.");
    }

    /**
     * Find supported {@link IDESupport} and apply task.
     */
    private void task(Consumer<IDESupport> task) {
        List<IDESupport> supports = new ArrayList();

        // search existing environment
        for (Class type : I.findAs(IDESupport.class)) {
            if (type != IDESupport.class) {
                IDESupport support = (IDESupport) TaskInfo.find(type);
                if (support.exist(project())) {
                    task.accept(support);
                    return;
                }
                supports.add(support);
            }
        }

        // initialize develop environemnt
        ui().info("Project develop environment is not found.");
        task.accept(ui().ask("Bee supports the following IDEs.", supports, IDESupport::name));
    }
}