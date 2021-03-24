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

import bee.api.Command;
import bee.api.Project;
import kiss.Extensible;

public interface IDESupport extends Extensible {
    /**
     * Generate project configuration file.
     */
    @Command("Generate configuration files.")
    void execute();

    /**
     * Check whether the configuration file is already existed or not.
     * 
     * @return
     */
    boolean exist(Project project);
}