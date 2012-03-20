/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.cli;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import kiss.ClassListener;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.model.ClassUtil;

/**
 * @version 2012/02/29 19:00:36
 */
@Manageable(lifestyle = Singleton.class)
public final class CommandLine implements ClassListener<Command> {

    /** The information. */
    private static Map<String, Info> infos = new ConcurrentHashMap();

    static {
        I.load(ClassUtil.getArchive(CommandLine.class));
    }

    /**
     * Hide constructor.
     */
    private CommandLine() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(Class<Command> clazz) {
        Info info = new Info(clazz);

        for (String name : info.names) {
            infos.put(name, info);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload(Class<Command> clazz) {
        Info info = new Info(clazz);

        for (String name : info.names) {
            infos.remove(name);
        }
    }

    /**
     * <p>
     * Parse command input.
     * </p>
     * 
     * @param input
     * @return
     */
    public static Command parse(String input) {
        if (input == null) {
            input = "";
        }

        // normalize whitespace in head and tail
        input = input.trim().concat(" ");

        int start = 0;
        boolean inQuote = false;
        Info info = null;
        List<String> arguments = new ArrayList();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (c) {
            case ' ':
                if (!inQuote) {
                    if (info == null) {
                        // first whitespace means command name
                        info = infos.get(input.substring(start, i));

                        if (info == null) {
                            // The command info is not found, so this input is invalid.
                            // Return null immediately.
                            return null;
                        }
                    } else {
                        // parameter is found
                        String arg = input.substring(start, i);

                        // de-quote if needed
                        if (arg.charAt(0) == '"') {
                            arg = arg.substring(1, arg.length() - 1);
                        }
                        arguments.add(arg);
                    }

                    // update index
                    start = i + 1;
                }
                break;

            case '"':
                inQuote = !inQuote;
                break;

            default:
                break;
            }
        }

        return info.invoke(arguments);
    }

    /**
     * @version 2012/02/29 19:34:17
     */
    private static class Info {

        /** The actual command class. */
        private final Class<Command> clazz;

        /** The command names. */
        private Set<String> names = new HashSet();

        /** The arguments. */
        private List<Field> args = new ArrayList();

        /**
         * @param clazz
         */
        private Info(Class clazz) {
            this.clazz = clazz;

            names.add(normalize(clazz.getSimpleName()));

            for (Field field : clazz.getDeclaredFields()) {
                Argument argument = field.getAnnotation(Argument.class);

                if (argument != null) {
                    field.setAccessible(true);
                    args.add(field);
                }
            }
        }

        /**
         * <p>
         * Normalize name.
         * </p>
         * 
         * @param name
         * @return
         */
        private static String normalize(String name) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);

                if (Character.isLowerCase(c)) {
                    builder.append(c);
                } else {
                    if (i != 0) {
                        builder.append('-');
                    }
                    builder.append(Character.toLowerCase(c));
                }
            }
            return builder.toString();
        }

        /**
         * <p>
         * Invoke command.
         * </p>
         * 
         * @param arguments
         * @return
         */
        private Command invoke(List<String> arguments) {
            try {
                Command command = I.make(clazz);

                for (int i = 0; i < args.size(); i++) {
                    Field field = args.get(i);
                    String value;

                    if (i + 1 == args.size()) {
                        // tail argument
                        value = I.join(arguments.subList(i, arguments.size()), " ");
                    } else {
                        value = arguments.get(i);
                    }
                    field.set(command, I.transform(value, field.getType()));
                }
                return command;
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
    }
}
