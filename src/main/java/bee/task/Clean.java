/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import bee.Task;
import bee.api.Command;

public class Clean extends Task {

    @Command("Clean output directory.")
    public void all() {
        project.getOutput().delete();

        ui.info("Deleted ", project.getOutput());
    }
}
