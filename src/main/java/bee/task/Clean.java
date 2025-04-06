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

import bee.Task;
import bee.api.Command;
import bee.util.Inputs;

public interface Clean extends Task {

    @Command("Clean output directory.")
    default void all() {
        project().getOutput()
                .trackDeleting("!*.jar")
                .to(Inputs.progress(project().getOutput(), "Deleting output directory", "Deleted output directory"));
    }
}