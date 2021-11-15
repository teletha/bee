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
import java.util.Map.Entry;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import bee.Bee;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import kiss.I;
import kiss.Ⅱ;

public class Env extends Task {

    public static String version = Bee.Tool.getVersion();

    @Command(defaults = true, value = "Build local bee environment using the stable version.")
    public void stable() {
        build("0.13.0");
    }

    @Command("Build local bee environment using the latest version.")
    public void latest() {
        build(I.json("https://jitpack.io/api/builds/" + Bee.Tool.getGroup() + "/" + Bee.Tool.getProduct() + "/latestOk").text("version"));
    }

    @Command("Build local bee environment using the selected version.")
    public void list() {
        List<DefaultArtifactVersion> list = I
                .signal(I.json("https://jitpack.io/api/builds/" + Bee.Tool.getGroup() + "/" + Bee.Tool.getProduct()).find("*", "*"))
                .flatIterable(json -> json.asMap(String.class).entrySet())
                .take(e -> e.getValue().equals("ok"))
                .map(Entry::getKey)
                .map(DefaultArtifactVersion::new)
                .sort(Comparator.naturalOrder())
                .toList();

        build(ui.ask("Which version of Bee do you want to use?", list).toString());
    }

    @Command("Build local bee environment using the user specified version.")
    public void use() {
        build(version);
    }

    @Command("Clean local bee environment.")
    public void clean() {
        deleteFile("bee.bat");
        deleteFile("bee.sh");

        ui.info("Remove user specified local bee environment.");
        ui.info("From now on, you will use Bee installed at [", Platform.Bee, "].");
    }

    /**
     * Create shell script adn bat file to execute the user specified runtime.
     * 
     * @param version
     */
    private void build(String version) {
        Ⅱ<String, String> context = I.pair(version, "https://jitpack.io/com/github/teletha/bee/" + version + "/bee-" + version + ".jar");

        String bat = I.express("""
                @echo off
                SET bee="%JAVA_HOME%/lib/bee/bee-{ⅰ}.jar"
                if not exist %bee% (
                  echo %bee% is not found, try to download it from network.
                  curl -#L -o %bee% {ⅱ}
                )
                java -javaagent:%bee% -cp %bee% bee.Bee %*
                """, context);

        String sh = I.express("""
                #!bin/bash
                bee=$JAVA_HOME/lib/bee-{ⅰ}.jar
                if [ ! -e $bee ]; then
                  echo $bee is not found, try to download it from network.
                  curl -#L -o $bee {ⅱ}
                fi
                java -javaagent:$bee -cp $bee bee.Bee "$@"
                """, context);

        makeFile("bee.bat", bat);
        makeFile("bee.sh", sh);

        ui.info("From now on, the bee command used in this directory will be fixed to version [", version, "].");
        ui.info("To clear this setting, execute the command [bee env:clear].");
    }
}
