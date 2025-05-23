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

import bee.Platform;
import bee.Task;
import bee.api.Command;
import bee.util.Process;

public interface Bun extends Task {

    @Command("Install bun.")
    default void install() {
        if (Process.isAvailable("bun")) {
            ui().info("Bun [", Process.with().read("bun -v"), "] is already installed.");
        } else {
            Process.with()
                    .inheritIO()
                    .when(Platform::isWindows, "powershell -c \"irm bun.sh/install.ps1|iex\"")
                    .run("curl -fsSL https://bun.sh/install | bash");
        }
    }

    @Command("Launch development server.")
    default void dev() {
        Process.with().inheritIO().run("bun run dev");
    }
}