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

import static bee.TaskOperations.*;

import bee.Task;
import bee.api.Command;
import bee.util.Inputs;

public class Clean extends Task {

    @Command("Clean output directory.")
    public void all() {
        project().getOutput()
                .trackDeleting("!*.jar")
                .to(Inputs.observerFor(ui(), project().getOutput(), "Deleting output directory", "Deleted output directory"));
    }
}