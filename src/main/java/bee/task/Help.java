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

import java.util.Locale;

import bee.Bee;
import bee.Platform;
import bee.Task;
import bee.api.Command;

/**
 * @version 2015/06/22 16:48:35
 */
public class Help extends Task {

    @Command("Display Bee runtime environment.")
    public void version() {
        ui.info("Bee version: ", Bee.API.getVersion());
        ui.info("Java version: ", System.getProperty("java.version"), " by ", System.getProperty("java.vendor"));
        ui.info("Java home: ", Platform.JavaHome);
        ui.info("Locale: ", Locale.getDefault().getDisplayName());
        ui.info("Encoding: ", Platform.Encoding.name());
    }
}