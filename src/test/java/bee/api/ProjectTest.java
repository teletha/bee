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

class ProjectTest {

    @Test
    void toDefinition() {
        License license = License.BSD;

        BlinkProject project = new BlinkProject();
        project.product("GROUP", "PRODUCT", "1.0");
        project.license(license);

        List<String> lines = project.toDefinition();
        assert lines.get(0).equals("/*");
        assert lines.get(1).equals(" * Copyright (C) " + Year.now() + " The PRODUCT Development Team");
        assert lines.get(2).equals(" *");
        assert lines.get(3).equals(" * Licensed under the " + license.name + " License (the \"License\");");
        assert lines.get(4).equals(" * you may not use this file except in compliance with the License.");
        assert lines.get(5).equals(" * You may obtain a copy of the License at");
        assert lines.get(6).equals(" *");
        assert lines.get(7).equals(" *          " + license.uri);
        assert lines.get(8).equals(" */");
        assert lines.get(9).equals("import static " + License.class.getName() + "." + license.name + ";");
        assert lines.get(10).equals("");
        assert lines.get(11).equals("public class Project extends bee.api.Project {");
        assert lines.get(12).equals("    {");
        assert lines.get(13).equals("        product(\"GROUP\", \"PRODUCT\", \"1.0\");");
        assert lines.get(14).equals("        license(License." + license.name + ");");
        assert lines.get(15).equals("    }");
        assert lines.get(16).equals("}");
        assert lines.size() == 17;
    }

    @Test
    void toDefinitionWithoutLicense() {
        License license = License.MIT;

        BlinkProject project = new BlinkProject();
        project.product("GROUP", "PRODUCT", "1.0");

        List<String> lines = project.toDefinition();
        assert lines.get(0).equals("/*");
        assert lines.get(1).equals(" * Copyright (C) " + Year.now() + " The PRODUCT Development Team");
        assert lines.get(2).equals(" *");
        assert lines.get(3).equals(" * Licensed under the " + license.name + " License (the \"License\");");
        assert lines.get(4).equals(" * you may not use this file except in compliance with the License.");
        assert lines.get(5).equals(" * You may obtain a copy of the License at");
        assert lines.get(6).equals(" *");
        assert lines.get(7).equals(" *          " + license.uri);
        assert lines.get(8).equals(" */");
        assert lines.get(9).equals("import static " + License.class.getName() + "." + license.name + ";");
        assert lines.get(10).equals("");
        assert lines.get(11).equals("public class Project extends bee.api.Project {");
        assert lines.get(12).equals("    {");
        assert lines.get(13).equals("        product(\"GROUP\", \"PRODUCT\", \"1.0\");");
        assert lines.get(14).equals("        license(License." + license.name + ");");
        assert lines.get(15).equals("    }");
        assert lines.get(16).equals("}");
        assert lines.size() == 17;
    }

}