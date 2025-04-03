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

public class InlineProjectAware {

    @BeforeAll
    static void setup() {
        I.load(Bee.class);
        I.load(InlineProjectAware.class);
    }

    private UserInterface ui;

    @BeforeEach
    void setup(TestInfo info) {
        Method method = info.getTestMethod().get();

        for (Class project : I.findAs(InlineProject.class)) {
            if (method.equals(project.getEnclosingMethod())) {
                LifestyleForProject.local.set((bee.api.Project) I.make(project));
            }
        }

        // switch ui
        ui = LifestyleForUI.local.get();
        LifestyleForUI.local.set(Null.UI);
    }

    @AfterEach
    void clean() {
        // restore ui
        LifestyleForUI.local.set(ui);
    }

    /**
     * Your defined project will be detected automatically.
     */
    protected static abstract class InlineProject extends bee.api.Project implements Extensible {
    }
}
