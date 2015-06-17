/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.util.Locale;

import bee.Bee;
import bee.Platform;
import bee.api.Command;
import bee.api.Task;

/**
 * @version 2012/05/18 11:11:39
 */
public class Help extends Task {

    @Command("Display environment.")
    public void version() {
        ui.talk("Bee version: ", Bee.API.getVersion());
        ui.talk("Java version: ", System.getProperty("java.version"), " by ", System.getProperty("java.vendor"));
        ui.talk("Java home: ", Platform.JavaHome);
        ui.talk("Locale: ", Locale.getDefault().getDisplayName());
        ui.talk("Encoding: ", Platform.Encoding.name());
    }
}
