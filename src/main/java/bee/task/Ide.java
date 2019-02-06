/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.util.function.Consumer;

import bee.api.Command;
import bee.api.Task;
import kiss.I;
import psychopath.Locator;
import psychopath.PathOperatable;

/**
 * @version 2016/11/30 12:22:21
 */
public class Ide extends Task {

    /**
     * <p>
     * Create idea's project file.
     * </p>
     */
    @Override
    @Command("Generate configuration files.")
    public void execute() {
        task(IDESupport::execute);

        ui.talk("Generate IDE configuration files.");

        PathOperatable o = Locator.folder();
        o.walkFile(op -> op);
        o.walkFile("");
        o.walkFileWithBase(oo -> oo);
        o.walkFileWithBase("");

        o.walkDirectory(op -> op);
        o.walkDirectory("");
        o.walkDirectoryWithBase(oo -> oo);
        o.walkDirectoryWithBase("");
    }

    /**
     * <p>
     * Find supported {@link IDESupport} and apply task.
     * </p>
     */
    private void task(Consumer<IDESupport> task) {
        I.find(IDESupport.class).stream().filter(support -> support.exist(project)).forEach(task);
    }
}
