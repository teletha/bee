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

    /** Instructs the system not to use any cache at build time. */
    public static final BeeOption<Boolean> NoCache = new BeeOption("nocache", "n", "Instructs the system not to use any cache at build time.", false);

    /** Instructs the system not to connect to an external network at build time. */
    public static final BeeOption<Boolean> Offline = new BeeOption("offline", "o", "Instructs the system not to connect to an external network at build time.", false);

    /** Perform profiling at build time and display the analysis results. */
    public static final BeeOption<Boolean> Profiling = new BeeOption("profiling", "p", "Perform profiling at build time and display the analysis results.", false);

    /** The list of builtin options. */
    private static final List<BeeOption> options = List.of(NoCache, Offline, Profiling);

    /** The name. */
    public final String name;

    /** The alias names. */
    public final String shortName;

    /** The description for user. */
    public final String description;

    /** The default value. */
    public final T defaultValue;

    /**
     * Hide constructor.
     */
    private BeeOption(String name, String shortName, String description, T defaultValue) {
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.defaultValue = defaultValue;
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
    public static void register(String key, String value) {
        key = key.toLowerCase();

        for (BeeOption option : options) {
            if (option.name.equals(key) || option.shortName.equals(key)) {
                I.env(option.name, value);
                return;
            }
        }
    }
}
