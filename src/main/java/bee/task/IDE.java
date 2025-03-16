/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.util.List;
import java.util.function.Consumer;

import bee.Task;
import bee.api.Command;
import kiss.I;

public class IDE extends Task {

    /**
     * Create idea's project file.
     */
    @Override
    @Command(value = "Generate configuration files.", defaults = true)
    public void execute() {
        task(IDESupport::execute);

        ui.info("Generate IDE configuration files.");
    }

    /**
     * Delete idea's project file.
     */
    @Command("Delete configuration files.")
    public void delete() {
        task(IDESupport::delete);

        ui.info("Delete IDE configuration files.");
    }

    /**
     * Find supported {@link IDESupport} and apply task.
     */
    private void task(Consumer<IDESupport> task) {
        List<IDESupport> supports = I.find(IDESupport.class);

        // search existing environment
        for (IDESupport support : supports) {
            if (support.exist(project)) {
                task.accept(support);
                return;
            }
        }

        // initialize develop environemnt
        ui.info("Project develop environment is not found.");
        task.accept(ui.ask("Bee supports the following IDEs.", supports));
    }
}