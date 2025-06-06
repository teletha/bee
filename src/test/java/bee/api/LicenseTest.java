/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.Test;

import bee.TestableProject;

class LicenseTest {

    @Test
    void noSpecified() {
        new TestableProject();

        List<String> text = License.MIT.text(true);
        assert text.get(0).equals("Copyright (C) " + Year.now().getValue() + " The TEST Development Team");
        assert text.get(2).equals("Licensed under the MIT License (the \"License\");");
    }

    @Test
    void specified() {
        TestableProject project = new TestableProject();
        project.license(License.BSD);

        List<String> text = project.license().text(true);
        assert text.get(0).equals("Copyright (C) " + Year.now().getValue() + " The TEST Development Team");
        assert text.get(2).equals("Licensed under the BSD License (the \"License\");");
    }

    @Test
    void durationFrom() {
        TestableProject project = new TestableProject();
        project.license(License.MPL);
        project.licenser(2020, "The Team");

        List<String> text = project.license().text(true);
        assert text.get(0).equals("Copyright (C) 2020-" + Year.now().getValue() + " The Team");
        assert text.get(2).equals("Licensed under the MPL License (the \"License\");");
    }

    @Test
    void durationFromTo() {
        TestableProject project = new TestableProject();
        project.license(License.MPL);
        project.licenser(2018, 2020, "The Team");

        List<String> text = project.license().text(true);
        assert text.get(0).equals("Copyright (C) 2018-2020" + " The Team");
        assert text.get(2).equals("Licensed under the MPL License (the \"License\");");
    }

    @Test
    void durationMultiple() {
        TestableProject project = new TestableProject();
        project.license(License.EPL);
        project.licenser(2018, 2020, "The Team");
        project.licenser(2021, 2021, "The New Team");

        List<String> text = project.license().text(true);
        assert text.get(0).equals("Copyright (C) 2018-2020" + " The Team");
        assert text.get(1).equals("Copyright (C) 2021" + " The New Team");
        assert text.get(3).equals("Licensed under the EPL License (the \"License\");");
    }
}