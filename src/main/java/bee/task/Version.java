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

import bee.Bee;
import bee.Task;
import bee.api.Command;
import kiss.I;

public class Version extends Task {

    public static String version = "LATEST";

    @Command("Build bee environment using the current version.")
    public void current() {
        build(Bee.Tool.getVersion());
    }

    @Command(defaults = true, value = "Build bee environment using the stable version.")
    public void stable() {
        build(I.json("https://jitpack.io/api/builds/" + Bee.Tool.getGroup() + "/" + Bee.Tool.getProduct() + "/latestOk").text("version"));
    }

    @Command("Build bee environment using the latest version.")
    public void latest() {
        build(I.json("https://jitpack.io/api/builds/" + Bee.Tool.getGroup() + "/" + Bee.Tool.getProduct() + "/latestOk").text("version"));
    }

    @Command("Build bee environment using the selected version.")
    public void list() {

    }

    @Command("Build bee environment using the specified version.")
    public void use() {

    }

    private void build(String version) {
        System.out.println(version);
    }
}
