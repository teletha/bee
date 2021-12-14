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
import bee.util.Process;
import kiss.I;
import kiss.Ⅱ;
import psychopath.File;
import psychopath.Locator;

public class Env extends Task {

    public static String version = Bee.Tool.getVersion();

    @Command(defaults = true, value = "Build local bee environment using the stable version.")
    public void stable() {
        build(I.http("https://git.io/stable-bee", String.class).waitForTerminate().to().v);
    }

    @Command("Build local bee environment using the latest version.")
    public void latest() {
        build(I.http("https://git.io/latest-bee", String.class).waitForTerminate().to().v);
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

    @Command("Build local bee environment using the local installed version.")
    public void local() {
        File from;
        File to = project.getRoot().file("bee-" + Bee.Tool.getVersion() + ".far");

        if (project.equals(Bee.Tool)) {
            require(Install::project);
            from = project.locateJar();
        } else {
            from = Locator.locate(Bee.class).asFile();
        }

        build(Bee.Tool.getVersion());

        // If you are executing Bee from wrapper, and you try to overwrite Bee's fat-jar
        // file, you will not be able to write it because the JVM has the file handle already.
        // So we are forcing the external process to copy it.
        runAfter("""
                copy /b /y "%s" "%s"
                """, """
                cp -f "%s" "%s"
                """, from, to);
    }

    @Command("Build local bee environment using the user specified version.")
    public void use() {
        build(version);
    }

    @Command("Clean local bee environment.")
    public void clean() {
        deleteFile("bee");
        deleteFile("bee.bat");
        deleteLocalFars();

        ui.info("Remove the local bee environment. From now on, you will use Bee installed at [", Platform.Bee, "].");
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
                setlocal enabledelayedexpansion
                set "bee=bee-{ⅰ}.far"
                if not exist %bee% (
                   set "bee=%JAVA_HOME%/lib/bee/bee-{ⅰ}.jar"
                    if not exist !bee! (
                        echo bee is not found locally, try to download it from network.
                        curl -#L -o $bee {ⅱ}
                    )
                )
                java -javaagent:%bee% -cp %bee% bee.Bee %*
                """, context);

        String sh = I.express("""
                #!bin/bash
                bee=bee-{ⅰ}.far
                if [ ! -e $bee ]; then
                    bee=$JAVA_HOME/lib/bee-{ⅰ}.jar
                    if [ ! -e $bee ]; then
                        echo $bee is not found locally, try to download it from network.
                        curl -#L -o $bee {ⅱ}
                    fi
                fi
                java -javaagent:$bee -cp $bee bee.Bee "$@"
                """, context);

        makeFile("bee.bat", bat);
        makeFile("bee", sh);

        ui.info("From now on, the bee command used in this directory will be fixed to version [", version, "].");
        ui.info("To clear this setting, execute the command [bee env:clean].");

        deleteLocalFars();
    }

    /**
     * Delete all local fat-jars.
     */
    private void deleteLocalFars() {
        project.getRoot().walkFile("bee-*.far").to(file -> {
            // If you are executing Bee from wrapper, and you try to detele Bee's fat-jar
            // files, you will not be able to delete it because the JVM has the file handle already.
            // So we are forcing the external process to delete it.
            runAfter("""
                    del /q "%s"
                    """, """
                    rm "%s"
                    """, file);
        });
    }

    private static final StringBuilder commands = new StringBuilder();

    /**
     * Invoke native command after the current build process.
     * 
     * @param bat
     * @param shell
     * @param params
     */
    private void runAfter(String bat, String shell, Object... params) {
        if (commands.isEmpty()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Process.with()
                        .inParallel()
                        .run(Platform.isWindows() //
                                ? List.of("cmd", "/c", "ping localhost -n 2" + commands)
                                : List.of("sleep 2" + commands));
            }));
        }
        commands.append(" && ").append((Platform.isWindows() ? bat : shell).strip().formatted(params));
    }
}
