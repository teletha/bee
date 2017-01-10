/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import bee.api.Command;
import bee.api.Project;
import kiss.Extensible;

/**
 * @version 2017/01/10 13:15:45
 */
public interface IDESupport extends Extensible {
    /**
     * <p>
     * Generate project configuration file.
     * </p>
     */
    @Command("Generate configuration files.")
    void execute();

    /**
     * <p>
     * Check whether the configuration file is already existed or not.
     * </p>
     * 
     * @return
     */
    boolean exist(Project project);
}
