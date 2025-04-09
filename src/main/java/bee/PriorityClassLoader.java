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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kiss.I;
import psychopath.Location;

/**
 * A ClassLoader that provides fine-grained control over class loading delegation.
 * <ul>
 * <li>Allows specifying "high-priority" classes loaded child-first from its own URLs.</li>
 * <li>Allows specifying "isolated" package prefixes (e.g., "bee.isolated.") whose classes
 * are always loaded by this loader, even if present in the parent. If the class
 * bytecode is not found in this loader's URLs, it attempts to fetch the bytecode
 * from the parent/system and define it within this loader.</li>
 * <li>For all other classes, it follows the standard parent-first delegation model.</li>
 * </ul>
 */
public class PriorityClassLoader extends URLClassLoader {

    /** The logger. */
    private final UserInterface ui;

    /**
     * Set containing the fully qualified names of classes to be loaded with high priority
     * from this classloader's paths first.
     */
    final Set<String> highPriorityClasses = new HashSet<>();

    /**
     * Constructs an empty PriorityClassLoader.
     */
    public PriorityClassLoader(UserInterface ui) {
        super(new URL[0]);
        this.ui = ui;
    }

    /**
     * Loads the class with the specified binary name according to the priority/isolation rules.
     *
     * @param name The binary name of the class to load.
     * @param resolve If {@code true}, resolve the class after loading.
     * @return The resulting {@code Class} object.
     * @throws ClassNotFoundException If the class could not be found according to the defined
     *             rules.
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 1. Check if class is already loaded by THIS loader instance
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                if (ui.debuggable) ui.debug("PriorityClassLoader: Returning already loaded class : " + name);
                return loadedClass;
            }

            // 2. Check if it's a high-priority class
            if (highPriorityClasses.contains(name) || name.contains(".isolated.")) {
                // Try loading from this classloader's URLs first (child-first for priority classes)
                try {
                    loadedClass = findClass(name);
                    if (ui.debuggable) ui.debug("PriorityClassLoader: Loaded with high-priority from own classpath : " + name);
                } catch (ClassNotFoundException e) {
                    // Not found in local URLs, try fetching bytecode from parent/system
                    byte[] bytes = fetchFromParentOrSystem(name);
                    if (bytes != null) {
                        // Define the class on this classloader
                        loadedClass = defineClass(name, bytes, 0, bytes.length);
                        if (ui.debuggable) {
                            ui.debug("PriorityClassLoader: Fetched and Loaded with high-priority from own classpath : " + name);
                        }
                    } else {
                        throw new ClassNotFoundException("High-priority class is not found in own classpath or parent/system resources :" + name, e);
                    }
                }
                if (resolve) resolveClass(loadedClass);
                return loadedClass;
            }

            // 3. Not a high-priority class, follow standard delegation model (Parent-First)
            try {
                ClassLoader parent = getParent();
                if (parent != null) {
                    // Delegate loading to the parent class loader.
                    loadedClass = parent.loadClass(name);
                    if (ui.debuggable) ui.debug("PriorityClassLoader: Loaded by parent loader : " + name);
                    // Parent found it, return. Resolution is handled by parent's logic.
                    // If parent.loadClass throws ClassNotFoundException or NoClassDefFoundError,
                    // we'll catch it below.
                    return loadedClass;
                } else {
                    // No explicit parent, try the bootstrap class loader (findSystemClass)
                    loadedClass = findSystemClass(name); // Delegates to bootstrap
                    if (loadedClass != null) {
                        if (ui.debuggable) ui.debug("PriorityClassLoader: Loaded by bootstrap loader : " + name);
                        return loadedClass;
                    }
                }
            } catch (ClassNotFoundException e) {
                // This error means the parent (or its delegation chain, including bootstrap) didn't
                // find the class definition. This is an expected path for classes that should be
                // loaded by *this* loader's URLs.
                if (ui.debuggable) ui.debug("PriorityClassLoader: Class definition is not found in parent/bootstrap loader : " + name);
            } catch (NoClassDefFoundError e) {
                // *** LESSON LEARNED / IMPORTANT CAVEAT ***
                // This error (NoClassDefFoundError) typically occurs when the parent/bootstrap
                // loader finds the class definition but then fails during the linking phase
                // because it cannot find one of the class's dependencies using its own classpath.
                //
                // This happens if the dependency is only available in the child loader's classpath.
                // Standard parent-first delegation does NOT allow the parent to ask the child for
                // classes.
                //
                // By catching this specific error here, we allow the process to fall back to step 4
                // (findClass() in this loader).
                // If both the class and its dependency are in *this* loader's URLs, findClass()
                // might succeed where the parent failed.
                ui.warn("PriorityClassLoader:  NoClassDefFoundError encountered during parent/bootstrap loading! This likely means the parent found the class definition but couldn't find a required dependency using its own classpath. Falling back to findClass() in local URLs, hoping to find both the class and its dependency there. Missing dependency detail might be in the cause: ", e);
            }

            // 4. If not found by parent/bootstrap OR if parent loading caused NoClassDefFoundError,
            // try finding in this classloader's URLs
            try {
                loadedClass = findClass(name); // Searches only this classloader's URLs
                if (resolve) {
                    resolveClass(loadedClass);
                }
                if (ui.debuggable) ui.debug("PriorityClassLoader: Loaded from own path : " + name);
                return loadedClass;
            } catch (ClassNotFoundException e) {
                // If findClass also throws CNFE, then the class is truly not found, either because
                // the class file is missing from URLs, or findClass itself triggered another
                // NCDFE/CNFE for a dependency.
                if (ui.debuggable) {
                    ui.debug("PriorityClassLoader: [" + name + "] ClassNotFoundException during findClass() after parent/bootstrap failed or NCDFE. Class not found anywhere. Classpath for this loader is:");
                    ui.debug(List.of(getURLs()));
                    ui.debug("System classpath is:");
                    ui.debug(List.of(System.getProperty("java.class.path").split(";")));
                }

                // Rethrow the exception from findClass.
                throw I.quiet(e);
            }
        }
    }

    /**
     * Attempts to load the bytecode for a given class name by searching the parent classloader
     * and potentially the system resources (which usually includes the bootstrap path).
     *
     * @param className The binary name of the class.
     * @return The class bytecode as a byte array, or null if not found.
     * @throws IOException If an error occurs reading the bytecode stream.
     */
    private byte[] fetchFromParentOrSystem(String className) {
        String resourceName = className.replace('.', '/') + ".class";
        InputStream input = null;

        // Try parent classloader first
        ClassLoader parent = getParent();
        if (parent != null) {
            input = parent.getResourceAsStream(resourceName);
        }

        // If not found in parent, try system resources (covers bootstrap and system classpath)
        if (input == null) {
            input = ClassLoader.getSystemResourceAsStream(resourceName);
        }

        if (input == null) {
            return null;
        }

        try {
            return input.readAllBytes();
        } catch (Exception e) {
            throw I.quiet(e);
        } finally {
            I.quiet(input);
        }
    }

    /**
     * Adds the specified location (JAR file or class directory) to the classpath.
     * Uses the custom {@code Location} type.
     *
     * @param location The location to be added (your custom type).
     * @return This {@link PriorityClassLoader} instance for method chaining.
     * @throws RuntimeException If the location cannot be converted to a valid URL.
     */
    public final PriorityClassLoader addClassPath(Location location) {
        addURL(location.asURL());
        return this;
    }

    /**
     * Defines and potentially loads the specified class with high priority.
     * <p>
     * The class bytecode is retrieved as a resource using the system class loader.
     * The class name is added to the high-priority set. This method attempts to
     * define the class within this class loader.
     *
     * @param target The target class to be defined with high priority.
     * @return This {@link PriorityClassLoader} instance for method chaining.
     * @throws RuntimeException If the class resource cannot be found, read, or defined,
     *             or if a security violation occurs. Wraps checked exceptions using I.quiet.
     */
    public final PriorityClassLoader importAsPriorityClass(Class<?> target) {
        String className = target.getName();
        String resourceName = className.replace('.', '/') + ".class";

        // Check if the class is already loaded by *this* specific loader instance.
        if (findLoadedClass(className) != null) {
            highPriorityClasses.add(className);
            if (ui.debuggable) {
                ui.debug("PriorityClassLoader: Class was already loaded by this specific loader instance. Marking as high-priority. : " + className);
            }
            return this;
        }

        // Obtain the class bytecode via the system class loader.
        try (InputStream input = ClassLoader.getSystemResourceAsStream(resourceName)) {
            if (input == null) {
                // This ClassNotFoundException indicates the .class file wasn't on the initial
                // system classpath
                throw new ClassNotFoundException("Class resource not found via SystemClassLoader: " + resourceName + ". Ensure the class '" + className + "' is available on the system classpath when calling importAsPriorityClass.");
            }

            highPriorityClasses.add(className);

            byte[] code = input.readAllBytes();
            defineClass(className, code, 0, code.length);
            if (ui.debuggable) ui.debug("PriorityClassLoader: [" + className + "] defined as high-priority class.");
        } catch (LinkageError e) {
            // LinkageError (e.g., duplicate class definition) can happen if the same class
            // was already loaded by a different loader (e.g., the parent).
            ui.warn("PriorityClassLoader: LinkageError defining [" + className + "]. It might have been already loaded by another loader (e.g., parent). Still marked as high-priority.", e);
        } catch (Exception e) {
            if (ui.debuggable) ui.debug("PriorityClassLoader:  Failed to import as priority class : " + className, e);
            throw I.quiet(e);
        }
        return this;
    }
}