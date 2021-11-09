/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import kiss.I;
import net.bytebuddy.agent.ByteBuddyAgent;
import psychopath.Location;
import psychopath.Option;

public class BeeLoader {

    /** The duplication checker. */
    private static final Set<Location> records = new HashSet();

    // The batch file for launching Bee contains a command line with parameters for JavaAgent, so
    // bytebuddy-agent is not required in the execution environment.
    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    /**
     * Support dynamic classpath or module loading.
     * 
     * @param path
     */
    public static synchronized void load(Location path) {
        if (records.add(path)) {
            try {
                if (path.isPresent()) {
                    // If you are not able to get instrumentation from JavaAgent, you can use
                    // bytebuddy-agent to get it dynamically. Since this method is only used during
                    // Bee development, the dependency of bytebuddy-agent is provided.
                    if (instrumentation == null) instrumentation = ByteBuddyAgent.install();

                    if (path.isDirectory()) path = path.packToTemporary(Option::strip);

                    instrumentation.appendToSystemClassLoaderSearch(new JarFile(path.asJavaFile()));
                }
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }
}
