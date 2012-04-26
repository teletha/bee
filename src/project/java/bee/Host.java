package bee;

/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
import java.util.Locale;

import bee.api.ArtifactLocator;
import bee.task.Command;
import bee.task.Jar;
import bee.task.Task;

/**
 * @version 2012/04/15 15:20:53
 */
public class Host extends Task {

    /**
     * <p>
     * Install the current Bee into your environment.
     * </p>
     */
    @Command(description = "Install the current Bee into your environment.")
    public void install() throws Exception {
        require(Jar.class).merge();

        BeeInstaller.install(ArtifactLocator.Jar.in(project));
    }

    /**
     * <p>
     * Display the processing envidonment information.
     * </p>
     */
    @Command(defaults = true, description = "Display the processing envidonment information.")
    public void version() {
        ui.talk("Bee version: 0.1");
        ui.talk("Java version: ", System.getProperty("java.version"), " by ", System.getProperty("java.vendor"));
        ui.talk("Java home: ", Platform.JavaHome);
        ui.talk("Locale: ", Locale.getDefault().getDisplayName());
        ui.talk("Encoding: ", Platform.Encoding.name());
    }
}
