/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.Test;

import bee.BlinkProject;

class LicenseTest {

    @Test
    void noSpecified() {
        new BlinkProject();

        List<String> text = License.MIT.text(true);
        assert text.get(0).equals("Copyright (C) " + Year.now().getValue() + " The BLINK Development Team");
        assert text.get(2).equals("Licensed under the MIT License (the \"License\");");
    }

    @Test
    void specified() {
        BlinkProject project = new BlinkProject();
        project.license(License.BSD);

        List<String> text = project.getLicense().text(true);
        assert text.get(0).equals("Copyright (C) " + Year.now().getValue() + " The BLINK Development Team");
        assert text.get(2).equals("Licensed under the BSD License (the \"License\");");
    }

    @Test
    void durationFrom() {
        BlinkProject project = new BlinkProject();
        project.license(License.MPL, 2020, "The Team");

        List<String> text = project.getLicense().text(true);
        assert text.get(0).equals("Copyright (C) 2020-" + Year.now().getValue() + " The Team");
        assert text.get(2).equals("Licensed under the MPL License (the \"License\");");
    }

    @Test
    void durationFromTo() {
        BlinkProject project = new BlinkProject();
        project.license(License.MPL, 2018, 2020, "The Team");

        List<String> text = project.getLicense().text(true);
        assert text.get(0).equals("Copyright (C) 2018-2020" + " The Team");
        assert text.get(2).equals("Licensed under the MPL License (the \"License\");");
    }

    @Test
    void durationMultiple() {
        BlinkProject project = new BlinkProject();
        project.license(License.EPL, 2018, 2020, "The Team");
        project.license(License.EPL, 2021, 2021, "The New Team");

        List<String> text = project.getLicense().text(true);
        assert text.get(0).equals("Copyright (C) 2018-2020" + " The Team");
        assert text.get(1).equals("Copyright (C) 2021" + " The New Team");
        assert text.get(3).equals("Licensed under the EPL License (the \"License\");");
    }
}
