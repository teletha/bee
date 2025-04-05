/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.TaskOperations.*;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bee.api.Command;
import bee.api.Comment;
import kiss.I;
import psychopath.Locator;

public interface Help extends Task {

    @Command(defaults = true, value = "Display all tasks.")
    default void task() {
        UserInterface ui = ui();

        ui.info("The available tasks are listed below. If you want to know more about each task, please run [YourTaskName:help].");

        for (TaskInfo info : TaskInfo.names.values().stream().map(List::getLast).toList()) {
            ui.title(String.format("  %-12s\tdefault [%s]\tclass [%s]", info.name.toUpperCase(), info.defaultCommnad, info.task.getName()));
            ui.info(info.descriptions);

            if (info.config != Object.class) {
                ui.info("Configurations");
                ui.info(info.configs.entrySet().stream().map(e -> {
                    try {
                        Field field = e.getValue();
                        String comment = field.getAnnotation(Comment.class).value();
                        String type = type(field.getGenericType());
                        Object value = field.get(TaskOperations.config(info.task));
                        if (value != null) {
                            Class clazz = value.getClass();
                            if (clazz.isSynthetic()) {
                                value = null;
                            }
                        }

                        if (value == null) {
                            return String.format("%-12s\t%s (%s)", e.getKey(), comment, type);
                        } else {
                            return String.format("%-12s\t%s (%s : %s)", e.getKey(), comment, type, value);
                        }
                    } catch (Exception x) {
                        throw I.quiet(x);
                    }
                }).toList());
            }
        }
    }

    private String type(Type type) {
        if (type == null) {
            return "";
        } else if (type instanceof Class clazz) {
            return clazz.getSimpleName();
        } else if (type instanceof ParameterizedType param) {
            return type(param.getRawType()) + Stream.of(param.getActualTypeArguments())
                    .map(e -> type(e))
                    .collect(Collectors.joining(", ", "<", ">"));
        } else if (type instanceof TypeVariable variable) {
            return variable.getName();
        } else if (type instanceof WildcardType wild) {
            return "?" + Stream.of(wild.getUpperBounds()).map(e -> type(e)).collect(Collectors.joining(" & ", " extends ", ""));
        } else if (type instanceof GenericArrayType array) {
            return type(array.getGenericComponentType()) + "[]";
        } else {
            throw new Error("FIX ME " + type);
        }
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