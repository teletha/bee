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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import kiss.Extensible;
import kiss.I;

public class AutomaticProjectAware {

    private UserInterface ui;

    @BeforeEach
    void setup(TestInfo info) {
        Method method = info.getTestMethod().get();

        for (Class<AutoProject> project : I.findAs(AutoProject.class)) {
            if (method.equals(project.getEnclosingMethod())) {
                LifestyleForProject.local.set(I.make(project));
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
    protected static abstract class AutoProject extends Project implements Extensible {
    }
}
