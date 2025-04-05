/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import kiss.Extensible;
import kiss.I;

/**
 * Base class for JUnit 5 tests that need awareness of an inline {@link InlineProject}.
 * <p>
 * This class provides setup and teardown logic using JUnit 5 lifecycle annotations
 * ({@link BeforeAll}, {@link BeforeEach}, {@link AfterEach}) to manage the project
 * context and user interface during test execution.
 * </p>
 * <p>
 * Specifically, it:
 * </p>
 * <ul>
 * <li>Initializes the Bee environment by loading necessary classes ({@link BeforeAll}).</li>
 * <li>Detects if the currently executing test method has an inner class definition
 * extending {@link InlineProject}. If found, it instantiates this project and sets it
 * as the current project context via {@link LifestyleForProject}. If not found,
 * a default anonymous {@link InlineProject} is used ({@link BeforeEach}).</li>
 * <li>Temporarily replaces the current {@link UserInterface} with {@link Null#UI}
 * before each test to suppress UI output during tests ({@link BeforeEach}).</li>
 * <li>Restores the original {@link UserInterface} after each test finishes
 * ({@link AfterEach}).</li>
 * </ul>
 * <p>
 * Tests inheriting from this class can define their own project configuration by declaring
 * a {@code protected static abstract class YourProject extends InlineProject { ... }}
 * inside the test method body. This allows for test-specific project setups.
 * </p>
 */
public class InlineProjectAware {

    /**
     * Initializes the Bee environment once before any tests in the class run.
     * Ensures that necessary Bee components and this awareness class itself are loaded
     * into the Kiss DI container.
     */
    @BeforeAll
    static void setup() {
        // Load essential Bee components and this class for dependency injection.
        I.load(Bee.class);
        I.load(InlineProjectAware.class);
    }

    /** Stores the original UserInterface to restore it after the test. */
    private UserInterface ui;

    /**
     * Sets up the test environment before each test method execution.
     * <p>
     * It attempts to find an {@link InlineProject} subclass defined within the currently
     * executing test method. If found, an instance of that project is created and set as the
     * active project using {@link LifestyleForProject}. Otherwise, a default, empty
     * {@link InlineProject} is set.
     * <p>
     * It also replaces the current {@link UserInterface} with {@link Null#UI} to prevent
     * test output pollution. The original UI is saved for restoration in {@link #clean()}.
     *
     * @param info JUnit 5 {@link TestInfo} providing access to the current test method.
     */
    @BeforeEach
    void setup(TestInfo info) {
        bee.api.Project inlineProject = null;
        Method method = info.getTestMethod().orElseThrow(() -> new IllegalStateException("Test method information is unavailable."));

        // Search for an InlineProject class defined inside the current test method.
        for (Class<?> projectClass : I.findAs(InlineProject.class)) {
            // Check if the found class is an inner class of the current test method.
            if (method.equals(projectClass.getEnclosingMethod())) {
                // If found, create an instance and assign it.
                inlineProject = (bee.api.Project) I.make(projectClass);
                break;
            }
        }

        // If no specific inline project was defined in the test method, use a default one.
        if (inlineProject == null) {
            inlineProject = new InlineProject() {
            };
        }

        // Set the determined project (either specific or default) as the current project context.
        LifestyleForProject.local.set(inlineProject);

        // Switch the UserInterface to Null.UI for the duration of the test.
        ui = LifestyleForUI.local.get(); // Save the original UI.
        LifestyleForUI.local.set(Null.UI); // Set the null UI.
    }

    /**
     * Cleans up the test environment after each test method execution.
     * Restores the original {@link UserInterface} that was active before the test started.
     */
    @AfterEach
    void clean() {
        // Restore the original UserInterface.
        LifestyleForUI.local.set(ui);
        // Project context cleanup might happen automatically depending on LifestyleForProject's
        // scope, but explicitly setting it to null could also be done if needed:
        // LifestyleForProject.local.set(null);
    }

    /**
     * A base class for defining project configurations inline within test methods.
     * <p>
     * Test classes inheriting from {@link InlineProjectAware} can define specific project
     * setups by creating a static inner class within a test method. The
     * {@link InlineProjectAware#setup(TestInfo)} method will automatically detect
     * and activate this project for the duration of the {@code myTest} execution.
     * </p>
     * It extends {@link bee.api.Project} and implements {@link Extensible} allowing it
     * to be managed by the Sinobu DI container.
     */
    protected static abstract class InlineProject extends bee.api.Project implements Extensible {
        // Users should extend this class and define project specifics.
    }
}