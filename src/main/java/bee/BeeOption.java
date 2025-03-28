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

import java.util.ArrayList;
import java.util.List;

import kiss.I;
import psychopath.Directory;
import psychopath.Locator;

public class BeeOption<T> {

    /** Instructs the system not to use any cache at build time. */
    public static final BeeOption<Boolean> Cacheless = new BeeOption("cacheless", "Don't use any cache.", false, 0, "nocache");

    /** Instructs the system to output all debug log at build time. */
    public static final BeeOption<Boolean> Debug = new BeeOption("debug", "Output all debug log.", false, 0);

    /**
     * Instructs the system to display information related to the current execution environment.
     * Synonymous with the task [help:task help:option].
     */
    public static final BeeOption<Boolean> Help = new BeeOption("help", "Show task information. Synonymous with the task [help:task help:option].", false, 0, "?");

    /** Predefine the user input. */
    public static final BeeOption<List<String>> Input = new BeeOption("input", "Predefine the user input.", List.of(), 16);

    /** Instructs the system not to connect to an external network at build time. */
    public static final BeeOption<Boolean> Offline = new BeeOption("offline", "Don't connect to external network.", false, 0);

    /** Perform profiling at build time and display the analysis results. */
    public static final BeeOption<Boolean> Profiling = new BeeOption("profiling", "Perform profiling and display the analysis results.", false, 0, "profile", "profiler");

    /** Instructs the system not to output error log only at build time. */
    public static final BeeOption<Boolean> Quiet = new BeeOption("quiet", "Output error log only.", false, 0);

    /** Instructs the system not to output error log only at build time. */
    public static final BeeOption<Directory> Root = new BeeOption("root", "Specify the project's root directory.", Locator.directory("")
            .absolutize(), 1, "dir", "directory");

    /** Instructs the system not to output error log only at build time. */
    public static final BeeOption<List<String>> Skip = new BeeOption("skip", "Skip the specified task.", List.of(), 16, "x");

    /** Instructs the system not to use wrapper bee. */
    public static final BeeOption<List<Boolean>> Unwrap = new BeeOption("unwrap", "Use the installed bee instead of the local wrapper.", false, 0, "u");

    /**
     * Instructs the system to display information related to the current execution environment.
     * Synonymous with the task [help:version].
     */
    public static final BeeOption<Boolean> Version = new BeeOption("version", "Show infomation for the current execution environment. Synonymous with the task [help:version].", false, 0);

    /** The list of builtin options. */
    static final List<BeeOption> options = List.of(Cacheless, Debug, Input, Root, Skip, Help, Offline, Profiling, Quiet, Version);

    /** The name. */
    private final String name;

    /** The alias names. */
    private final String shortName;

    /** The alise list. */
    private final List<String> aliases;

    /** The description for user. */
    private final String description;

    /** The parameter size. */
    private final int paramSize;

    /** The default value. */
    final T defaultValue;

    /** The current value. */
    T value;

    /**
     * Hide constructor.
     */
    private BeeOption(String name, String description, T defaultValue, int parameterSize, String... aliases) {
        this.name = name;
        this.shortName = name.substring(0, 1);
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = I.env(name, defaultValue);
        this.aliases = List.of(aliases);
        this.paramSize = parameterSize;
    }

    /**
     * Get the value of this option.
     * 
     * @return
     */
    public T value() {
        return value;
    }

    /**
     * Determine if this option has been configured by the user.
     * 
     * @return
     */
    public boolean isConfigured() {
        return value != defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("-%-3s --%-8s \t%s", shortName, name, description);
    }

    /**
     * Parse options.
     * 
     * @param args
     * @return
     */
    static List<String> parse(String... args) {
        List<String> washed = new ArrayList();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.charAt(0) == '-') {
                arg = arg.substring(1);

                if (arg.charAt(0) == 'D') {
                    // define system property like maven
                    arg = arg.substring(1);
                    int equal = arg.indexOf('=');
                    if (equal == -1) {
                        System.setProperty(arg, "true");
                    } else {
                        System.setProperty(arg.substring(0, equal), arg.substring(equal + 1));
                    }
                } else if (arg.charAt(0) == '-') {
                    // long name option
                    arg = arg.substring(1);
                    int equal = arg.indexOf('=');
                    if (equal == -1) {
                        i += register(arg, args, i);
                    } else {
                        String param = arg.substring(equal + 1);
                        arg = arg.substring(0, equal);
                        i += register(arg, new String[] {arg, param}, 0);
                    }
                } else {
                    // short name option
                    for (int j = 0; j < arg.length(); j++) {
                        i += register(String.valueOf(arg.charAt(j)), args, i);
                    }
                }
            } else {
                washed.add(arg);
            }
        }

        return washed;
    }

    /**
     * Find the matched option.
     * 
     * @param name
     * @param args
     * @param index
     * @return
     */
    private static int register(String name, String[] args, int index) {
        int skip = 0;

        for (BeeOption o : options) {
            if (o.name.equals(name) || o.shortName.equals(name) || o.aliases.contains(name)) {
                if (o.paramSize == 0) {
                    o.value = true;
                } else {
                    skip = o.paramSize;

                    List params = new ArrayList(o.paramSize);
                    for (int i = 0, max = Math.min(o.paramSize, args.length - index - 1); i < max; i++) {
                        String param = args[index + i + 1];
                        if (param.charAt(0) == '-') {
                            skip = i;
                            break;
                        } else {
                            if (o.defaultValue instanceof List) {
                                params.add(param);
                            } else {
                                params.add(I.transform(param, o.defaultValue.getClass()));
                            }
                        }
                    }

                    if (o.paramSize == 1) {
                        o.value = params.get(0);
                    } else {
                        o.value = params;
                    }
                }
                break;
            }
        }
        return skip;
    }
}