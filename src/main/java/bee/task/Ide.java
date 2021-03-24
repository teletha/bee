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

import java.util.function.Consumer;

import bee.Task;
import bee.api.Command;
import kiss.I;

public class Ide extends Task {

    /**
     * Create idea's project file.
     */
    @Override
    @Command("Generate configuration files.")
    public void execute() {
        require(Pom::build);

        task(IDESupport::execute);

        ui.talk("Generate IDE configuration files.");
    }

    /**
     * Find supported {@link IDESupport} and apply task.
     */
    private void task(Consumer<IDESupport> task) {
        I.find(IDESupport.class).stream().filter(support -> support.exist(project)).forEach(task);
    }
}