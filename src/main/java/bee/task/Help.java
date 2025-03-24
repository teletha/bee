/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import bee.Bee;
import bee.BeeOption;
import bee.Platform;
import bee.Task;
import bee.api.Command;
import kiss.I;
import psychopath.Locator;

public class Help extends Task {

    @Command(defaults = true, value = "Display all tasks.")
    public void task() {
        ui.info("The available tasks are listed below. If you want to know more about each task, please run [YourTaskName:help].");

        try {
            Field field = Task.class.getDeclaredField("commons");
            field.setAccessible(true);
            Map tasks = (Map) field.get(null);
            ui.info(List.copyOf(tasks.values()));
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    @Command("Display all options.")
    public void option() {
        ui.info("The available options are listed below.");

        try {
            Field field = BeeOption.class.getDeclaredField("options");
            field.setAccessible(true);
            List options = (List) field.get(null);
            ui.info(options);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    @Command("Display Bee runtime environment.")
    public void version() {
        ui.info("Bee   \t", Bee.API.getVersion(), " [", Locator.locate(Bee.class), "]");
        ui.info("Java  \t", System.getProperty("java.vendor"), " ", Runtime.version(), "@", System
                .getProperty("java.class.version"), " [", Platform.Java, "]");
        ui.info("OS     \t", Platform.OSName, " ", Platform.OSArch, " ", Platform.OSVersion);
        ui.info("Locale \t", Locale.getDefault().getDisplayName(Locale.ENGLISH));
        ui.info("Charset\t", Platform.Encoding.displayName(Locale.ENGLISH));
    }

    @Command("Show welcome message.")
    public void welcome() {
        ui.title("Welcome to Bee!");
        ui.info("""
                Thank you for installing Bee. We're excited to have you on board!
                Here’s what you can do next:

                Get Started Quickly
                If you are using an IDE, simply type [bee ide] in your project directory.
                After answering a few questions, the necessary files for your IDE should be generated.
                Once that’s done, use your IDE to edit the generated Project class and define your project.

                Explore Tasks
                You can also type [bee help] to display a list of all currently available tasks.
                Feel free to try out the commands and explore what you can do!
                """);

        require(Help::task);
    }
}