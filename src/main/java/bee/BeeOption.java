/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.util.List;

import kiss.I;

public class BeeOption<T> {

    /**
     * Instructs the system to display information related to the current execution environment.
     * Synonymous with the task [help:task].
     */
    public static final BeeOption<Boolean> Help = new BeeOption("help", "h", "Instructs the system to display task information. Synonymous with the task [help:task].", false, "help:task");

    /** Instructs the system not to use any cache at build time. */
    public static final BeeOption<Boolean> NoCache = new BeeOption("nocache", "n", "Instructs the system not to use any cache at build time.", false, null);

    /** Instructs the system not to connect to an external network at build time. */
    public static final BeeOption<Boolean> Offline = new BeeOption("offline", "o", "Instructs the system not to connect to an external network at build time.", false, null);

    /** Perform profiling at build time and display the analysis results. */
    public static final BeeOption<Boolean> Profiling = new BeeOption("profiling", "p", "Perform profiling at build time and display the analysis results.", false, null);

    /**
     * Instructs the system to display information related to the current execution environment.
     * Synonymous with the task [help:version].
     */
    public static final BeeOption<Boolean> Version = new BeeOption("version", "v", "Instructs the system to display information related to the current execution environment. Synonymous with the task [help:version].", false, "help:version");

    /** The list of builtin options. */
    private static final List<BeeOption> options = List.of(Help, NoCache, Offline, Profiling, Version);

    /** The name. */
    public final String name;

    /** The alias names. */
    public final String shortName;

    /** The description for user. */
    public final String description;

    /** The default value. */
    public final T defaultValue;

    /** The alternative command. */
    private final String command;

    /**
     * Hide constructor.
     */
    private BeeOption(String name, String shortName, String description, T defaultValue, String command) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.command = command;
    }

    /**
     * Get the value of this option.
     * 
     * @return
     */
    public T value() {
        return I.env(name, defaultValue);
    }

    /**
     * Register the property.
     * 
     * @param key
     * @param value
     */
    static String register(String key, String value) {
        key = key.toLowerCase();

        for (BeeOption option : options) {
            if (option.name.equals(key) || option.shortName.equals(key)) {
                if (option.command == null) {
                    I.env(option.name, value);
                    return null;
                } else {
                    return option.command;
                }
            }
        }
        return null;
    }
}
