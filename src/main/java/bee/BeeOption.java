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
    public static final BeeOption<Boolean> Cacheless = new BeeOption("cacheless", "c", "Don't use any cache.", false);

    /** Instructs the system to output all debug log at build time. */
    public static final BeeOption<Boolean> Debug = new BeeOption("debug", "d", "Output all debug log.", false);

    /**
     * Instructs the system to display information related to the current execution environment.
     * Synonymous with the task [help:task help:option].
     */
    public static final BeeOption<Boolean> Help = new BeeOption("help", "h", "Show task information. Synonymous with the task [help:task help:option].", false);

    /** Instructs the system not to connect to an external network at build time. */
    public static final BeeOption<Boolean> Offline = new BeeOption("offline", "o", "Don't connect to external network.", false);

    /** Perform profiling at build time and display the analysis results. */
    public static final BeeOption<Boolean> Profiling = new BeeOption("profiling", "p", "Perform profiling and display the analysis results.", false);

    /** Instructs the system not to output error log only at build time. */
    public static final BeeOption<Boolean> Quiet = new BeeOption("quiet", "q", "Output error log only.", false);

    /**
     * Instructs the system to display information related to the current execution environment.
     * Synonymous with the task [help:version].
     */
    public static final BeeOption<Boolean> Version = new BeeOption("version", "v", "Show infomation for the current execution environment. Synonymous with the task [help:version].", false);

    /** The list of builtin options. */
    private static final List<BeeOption> options = List.of(Cacheless, Debug, Help, Offline, Profiling, Quiet, Version);

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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("-%-3s --%-8s \t%s", shortName, name, description);
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
                I.env(option.name, value);
            }
        }
        return null;
    }
}
