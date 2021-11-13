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

import java.util.Comparator;
import java.util.List;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import bee.Bee;
import bee.Task;
import bee.api.Command;
import kiss.I;
import kiss.Ⅱ;

public class Env extends Task {

    public static String version = Bee.Tool.getVersion();

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
        List<DefaultArtifactVersion> list = I
                .signal(I.json("https://jitpack.io/api/builds/" + Bee.Tool.getGroup() + "/" + Bee.Tool.getProduct()).find("*", "*"))
                .flatIterable(json -> json.entries(String.class))
                .take(e -> e.ⅱ.equals("ok"))
                .map(Ⅱ::ⅰ)
                .map(DefaultArtifactVersion::new)
                .sort(Comparator.naturalOrder())
                .toList();

        build(ui.ask("Which version of Bee do you want to use?", list).toString());
    }

    @Command("Build bee environment using the specified version.")
    public void use() {
        build(version);
    }

    private void build(String version) {
        System.out.println(version);

        String text = String.format("""
                curl -sL -o bee.jar https://jitpack.io/com/github/teletha/bee/0.10.0/bee-%s.jar
                """, version);
    }
}
