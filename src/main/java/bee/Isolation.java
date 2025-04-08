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
 * Provides a mechanism to execute code within an isolated context defined by a specific set of
 * library dependencies.
 * <p>
 * This class achieves isolation by creating a dedicated {@link PriorityClassLoader} loaded with the
 * specified dependencies and their transitive runtime dependencies. It then uses a
 * serialization-deserialization trick within its constructor to switch the execution context
 * to this isolated classloader for the {@link #isolate()} or {@link #bridge()} method.
 * </p>
 * <p>
 * Subclasses should override either {@link #isolate()} for procedures that do not return a value,
 * or {@link #bridge()} for functions that return a value of type {@code V}. The constructor
 * automatically detects which method is overridden (preferring {@code isolate()} if both are
 * somehow
 * overridden) and executes it within the isolated context.
 * </p>
 * <p>
 * Use the {@link #load(Class, String)} and {@link #create(Class, String)} methods within
 * your overridden {@code isolate()} or {@code bridge()} method to interact with classes loaded
 * from the specified dependencies.
 * </p>
 *
 * @param <V> The type of the value returned by the {@link #bridge()} method. Use {@link Void} if
 *            no return value is needed (and override {@link #isolate()} instead).
 */
@SuppressWarnings("serial")
public abstract class Isolation<V> implements Serializable {

    /**
     * The dedicated class loader for the specified dependencies. It is marked {@code transient}
     * because a ClassLoader itself is generally not serializable and is specific to the
     * execution context where it was created. The deserialized copy running in the isolated
     * context will have its own reference implicitly managed.
     */
    private final transient PriorityClassLoader loader;

    /** Holds the result returned by the {@link #bridge()} method, if it was executed. */
    private V result;

    /**
     * Sets up the dependency context and executes the isolated code.
     * All operations involving dependency classes (loading, instantiation, method calls) should
     * occur within the overridden {@code isolate()} or {@code bridge()} method to ensure they
     * happen in the isolated context.
     * 
     * @param dependencies A list of dependency descriptors (e.g., "groupId:artifactId:version")
     *            required for the isolated execution context.
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

        // import this class and its inner classes into the isolated loader
        loader.importAsPriorityClass(getClass());
        for (Class<?> sub : getClass().getDeclaredClasses()) {
            loader.importAsPriorityClass(sub);
        }

        // serialize myself
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(this);
        } catch (Exception e) {
            throw new Fail("Failed to serialize Isolation task for context switch.").reason(e);
        }

        // copy object by deserialize
        try (ObjectInputStream in = new CustomObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()), loader)) {
            Isolation<V> isolation = (Isolation) in.readObject();

            try {
                // check whether this class overrides isolate method or not
                getClass().getDeclaredMethod("isolate");

                isolation.isolate();
            } catch (NoSuchMethodException e) {
                result = isolation.bridge();
            }
        } catch (Exception e) {
            throw new Fail("Failed to switch context or execute isolated code.").reason(e);
        }
    }

    /**
     * This method is intended to be overridden by subclasses to perform actions
     * within the isolated classloader context that do not produce a return value.
     * <p>
     * The code inside this method will execute with the dependencies specified in the
     * constructor loaded preferentially by the isolated classloader. Use
     * {@link #load(Class, String)} and {@link #create(Class, String)} to interact
     * with classes from these dependencies.
     * </p>
     * <p>
     * If this method is overridden, the {@link #bridge()} method will typically not be executed.
     * </p>
     * <p>
     * The default implementation does nothing.
     * </p>
     */
    protected void isolate() {
        // Default implementation does nothing. Subclasses override for side effects.
    }

    /**
     * This method is intended to be overridden by subclasses to perform actions
     * within the isolated classloader context that produce a return value of type {@code V}.
     * <p>
     * The code inside this method will execute with the dependencies specified in the
     * constructor loaded preferentially by the isolated classloader. Use
     * {@link #load(Class, String)} and {@link #create(Class, String)} to interact
     * with classes from these dependencies.
     * </p>
     * <p>
     * If the {@link #isolate()} method is overridden by the subclass, this {@code bridge()}
     * method will typically not be executed. The result of executing this method is stored
     * and can be retrieved using {@link #result()}.
     * </p>
     * <p>
     * The default implementation returns {@code null}.
     * </p>
     *
     * @return The computed result of type {@code V}.
     */
    protected V bridge() {
        return null; // subclasses override to return a value
    }

    /**
     * Loads a class by its fully qualified name using the internal {@link PriorityClassLoader}
     * associated with this dependency context. This ensures the class is loaded from the
     * specified dependencies if available, respecting the classloader's priority rules.
     *
     * @param <T> The expected type of the class.
     * @param type A {@link Class} object representing the expected type {@code T}, used only
     *            for compile-time type checking and casting convenience of the return value.
     * @param fqcn The fully qualified class name (e.g., "com.example.MyClass").
     * @return The loaded {@link Class} object, cast to {@code Class<T>}.
     * @throws RuntimeException wrapping a {@link ClassNotFoundException} if the class cannot be
     *             found by the internal class loader or its parent delegation chain.
     */
    public final <T> Class<T> load(Class<T> type, String fqcn) {
        try {
            // Use the isolated loader to find the class.
            return (Class<T>) loader.loadClass(fqcn);
        } catch (ClassNotFoundException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Loads a class by its fully qualified name using the internal {@link PriorityClassLoader}
     * and then creates a new instance of it using {@link I#make(Class)} (Kiss DI).
     * This is useful for instantiating classes from the dependency context, potentially
     * utilizing dependency injection if configured via {@code kiss.I}.
     *
     * @param <T> The expected type of the instance.
     * @param type A {@link Class} object representing the expected type {@code T}. This is used
     *            both for loading the correct class via {@link #load(Class, String)} and for
     *            type safety of the returned instance.
     * @param fqcn The fully qualified class name of the class to load and instantiate.
     * @return A new instance of the loaded class, cast to {@code T}.
     * @throws RuntimeException wrapping errors that might occur during class loading
     *             ({@link ClassNotFoundException}) or instantiation (via {@code I.make}).
     */
    public final <T> T create(Class<T> type, String fqcn) {
        return I.make(load(type, fqcn));
    }

    /**
     * Returns the result computed and returned by the {@link #bridge()} method, if it was executed.
     * If the {@link #isolate()} method was executed instead (because it was overridden), this
     * method will return the initial value of the result field (typically {@code null}).
     *
     * @return The result from the {@link #bridge()} execution, or {@code null}.
     */
    public final V result() {
        return result;
    }

    /**
     * Factory method to create an {@link Isolation} instance configured with the specified
     * dependencies, designed for cases where no return value is needed (i.e., side effects only).
     * The returned instance overrides {@link #isolate()} with an empty implementation.
     * A subclass would typically be created to override {@code isolate} with actual logic.
     * This factory might be less useful now compared to directly creating a subclass that
     * overrides {@code isolate}.
     *
     * @param dependencies A list of dependency descriptors (e.g., "groupId:artifactId:version").
     * @return A new {@link Isolation} instance whose {@code isolate()} method does nothing.
     *         The primary effect is the setup of the class loader with the dependencies
     *         during the construction of this object.
     */
    public static Isolation<Void> with(String... dependencies) {
        return new Isolation<Void>(dependencies) {
            @Override
            protected void isolate() {
                // This isolate method executes within the context of the PriorityClassLoader
                // but performs no actions by default. A subclass should override this.
            }
        };
    }

    /**
     * A custom {@link ObjectInputStream} that resolves classes using the provided
     * {@link ClassLoader}. This is crucial for deserializing the {@link Isolation}
     * instance within the context of the {@link PriorityClassLoader}, ensuring that the
     * deserialized object's class (and the classes of its fields) are loaded by the
     * correct, isolated classloader.
     */
    private static class CustomObjectInputStream extends ObjectInputStream {

        /** The isolated classloader to use for resolving classes during deserialization. */
        private final ClassLoader classLoader;

        /**
         * Constructs the custom input stream.
         * 
         * @param in The underlying input stream (e.g., from serialized bytes).
         * @param classLoader The classloader to use for resolving classes.
         * @throws IOException If an I/O error occurs creating the stream.
         */
        private CustomObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        /**
         * Resolves the class described by {@code desc} using the specific class loader
         * provided during construction. Handles both regular classes and array types.
         *
         * @param desc The description of the class to resolve.
         * @return The resolved {@link Class}.
         * @throws IOException If an I/O error occurs.
         * @throws ClassNotFoundException If the class (or its component type for arrays)
         *             cannot be found by the specified loader.
         */
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String name = desc.getName();
            try {
                // Use Class.forName with the specified loader, which handles primitive types
                // and array types correctly according to Javadoc.
                return Class.forName(name, false, classLoader);
            } catch (ClassNotFoundException e) {
                // If the isolated loader can't find it, maybe it's a system class?
                // Falling back to super.resolveClass() might find it via the default mechanism,
                // which could be necessary for core Java classes or classes shared via parent
                // loader. Be cautious if true isolation is required.
                return super.resolveClass(desc);
            }
        }
    }
}