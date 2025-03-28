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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import bee.api.Library;
import bee.api.Repository;
import bee.api.Scope;
import kiss.I;

/**
 * Provides a mechanism to execute code within a context where specified dependencies
 * are loaded by a dedicated {@link PriorityClassLoader}. This class handles the
 * setup of the class loader, serialization/deserialization to switch context,
 * and execution of the {@link #run()} method within that context.
 */
@SuppressWarnings("serial")
public abstract class Isolation implements Runnable, Serializable {

    /**
     * The dedicated class loader for the specified dependencies. Marked transient as it's
     * context-specific.
     */
    private final transient PriorityClassLoader loader;

    /**
     * Sets up the dependency context with the specified libraries.
     * The constructor creates a {@link PriorityClassLoader}, adds the specified
     * dependencies and their transitive runtime dependencies to its classpath,
     * defines the concrete subclass of {@link Isolation} within this loader,
     * and then executes the {@link #run()} method within the context of this loader
     * via serialization/deserialization trick.
     *
     * @param dependencies A list of dependency descriptors (e.g., "groupId:artifactId:version").
     */
    protected Isolation(String... dependencies) {
        this.loader = new PriorityClassLoader(I.make(UserInterface.class));

        for (String dependency : dependencies) {
            Library require = Library.parse(dependency);
            loader.addClassPath(require.getLocalJar());

            for (Library library : I.make(Repository.class).collectDependency(require, Scope.Runtime)) {
                loader.addClassPath(library.getLocalJar());
            }
        }

        // import and define classes related to this class
        loader.importAsPriorityClass(getClass());
        for (Class<?> sub : getClass().getDeclaredClasses()) {
            loader.importAsPriorityClass(sub);
        }

        // serialize myself
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(this);
        } catch (Exception e) {
            throw I.quiet(e);
        }

        // copy object by deserialize
        try (ObjectInputStream in = new CustomObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()), loader)) {
            Runnable runnable = (Runnable) in.readObject();
            runnable.run();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * Loads a class by its fully qualified name using the internal {@link PriorityClassLoader}
     * associated with this dependency context. This ensures the class is loaded from the
     * specified dependencies if available.
     *
     * @param <T> The expected type of the class.
     * @param type A {@link Class} object representing the expected type {@code T}, used for type
     *            safety.
     * @param fqcn The fully qualified class name (e.g., "com.example.MyClass").
     * @return The loaded {@link Class} object, cast to {@code Class<T>}.
     * @throws RuntimeException wrapping a {@link ClassNotFoundException} if the class cannot be
     *             found by the internal class loader.
     */
    public <T> Class<T> load(Class<T> type, String fqcn) {
        try {
            // The type parameter is mainly for compile-time type checking and casting convenience.
            // The actual loading is done using the fqcn string.
            return (Class<T>) loader.loadClass(fqcn);
        } catch (ClassNotFoundException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Loads a class by its fully qualified name using the internal {@link PriorityClassLoader}
     * and then creates a new instance of it using {@link I#make(Class)}.
     * This is useful for instantiating classes from the dependency context, potentially
     * utilizing dependency injection if configured via {@code kiss.I}.
     *
     * @param <T> The expected type of the instance.
     * @param type A {@link Class} object representing the expected type {@code T}.
     * @param fqcn The fully qualified class name of the class to load and instantiate.
     * @return A new instance of the loaded class, cast to {@code T}.
     * @throws RuntimeException wrapping errors that might occur during class loading
     *             ({@link ClassNotFoundException}) or instantiation.
     */
    public <T> T create(Class<T> type, String fqcn) {
        return I.make(load(type, fqcn));
    }

    /**
     * Factory method to create a {@link Isolation} instance configured with the specified
     * dependencies, but without any specific task defined in its {@link #run()} method.
     * This is useful when the primary goal is just to set up the classpath with the
     * given dependencies, perhaps to use the {@link #load(Class, String)} or
     * {@link #create(Class, String)} methods afterwards on the returned instance (though
     * the instance itself won't be easily accessible after construction due to the
     * serialization trick). Typically used when the action is performed *within* the
     * constructor or initialization phase of a subclass.
     *
     * @param dependencies A list of dependency descriptors (e.g., "groupId:artifactId:version").
     * @return A new {@link Isolation} instance whose {@code run()} method does nothing.
     *         The primary effect is the setup of the class loader with the dependencies
     *         during the construction of this object.
     */
    public static Isolation with(String... dependencies) {
        return new Isolation(dependencies) {
            @Override
            public void run() {
                // This run method executes within the context of the PriorityClassLoader
                // but performs no actions by default for Depend.on().
            }
        };
    }

    /**
     * A custom {@link ObjectInputStream} that resolves classes using the provided
     * {@link ClassLoader}. This is crucial for deserializing the {@link Isolation}
     * instance within the context of the {@link PriorityClassLoader}.
     */
    private static class CustomObjectInputStream extends ObjectInputStream {
        private final ClassLoader classLoader;

        private CustomObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        /**
         * Resolves the class described by {@code desc} using the specific class loader
         * provided during construction.
         *
         * @param desc The description of the class to resolve.
         * @return The resolved {@link Class}.
         * @throws IOException If an I/O error occurs.
         * @throws ClassNotFoundException If the class cannot be found by the specified loader.
         */
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return classLoader.loadClass(desc.getName());
            } catch (ClassNotFoundException e) {
                throw e;
            }
        }
    }
}