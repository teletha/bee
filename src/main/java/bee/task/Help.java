/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.util.Locale;

import bee.Bee;
import bee.BeeOption;
import bee.Platform;
import bee.Task;
import bee.TaskInfo;
import bee.UserInterface;
import bee.api.Command;
import psychopath.Locator;

public interface Help extends Task {

    @Command(defaults = true, value = "Display all tasks.")
    default void task() {
        UserInterface ui = ui();

        ui.info("The available tasks are listed below. If you want to know more about each task, please run [YourTaskName:help].");
        ui.info(TaskInfo.list());
    }

    @Command("Display all options.")
    default void option() {
        UserInterface ui = ui();

        ui.info("The available options are listed below.");
        ui.info(BeeOption.AVAILABLES);
    }

    @Command("Display Bee runtime environment.")
    default void version() {
        UserInterface ui = ui();

        ui.info("Bee   \t", Bee.API.getVersion(), " [", Locator.locate(Bee.class), "]");
        ui.info("Java  \t", System.getProperty("java.vendor"), " ", Runtime.version(), "@", System
                .getProperty("java.class.version"), " [", Platform.Java, "]");
        ui.info("OS     \t", Platform.OSName, " ", Platform.OSArch, " ", Platform.OSVersion);
        ui.info("Locale \t", Locale.getDefault().getDisplayName(Locale.ENGLISH));
        ui.info("Charset\t", Platform.Encoding.displayName(Locale.ENGLISH));
    }

    @Command("Show welcome message.")
    default void welcome() {
        UserInterface ui = ui();

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