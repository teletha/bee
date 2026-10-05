/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.util;

import org.junit.jupiter.api.Test;

import bee.util.SemanticVersion.Bump;

class SemanticVersionTest {

    @Test
    void parse() {
        SemanticVersion version = new SemanticVersion("0.79.1");
        assert version.major == 0;
        assert version.minor == 79;
        assert version.patch == 1;
        assert version.toString().equals("0.79.1");
    }

    @Test
    void parseShort() {
        assert new SemanticVersion("1").toString().equals("1.0.0");
        assert new SemanticVersion("1.2").toString().equals("1.2.0");
        assert new SemanticVersion("v1.2.3").toString().equals("1.2.3");
        assert new SemanticVersion("1.2.3-alpha.1").toString().equals("1.2.3");
    }

    @Test
    void isVersion() {
        assert SemanticVersion.isVersion("1.2.3");
        assert SemanticVersion.isVersion("v1.2.3");
        assert SemanticVersion.isVersion("1.2.3-rc.1");
        assert !SemanticVersion.isVersion("master");
        assert !SemanticVersion.isVersion("release-1");
        assert !SemanticVersion.isVersion("");
        assert !SemanticVersion.isVersion(null);
    }

    @Test
    void bump() {
        SemanticVersion version = new SemanticVersion("1.2.3");
        assert version.bump(Bump.MAJOR).toString().equals("2.0.0");
        assert version.bump(Bump.MINOR).toString().equals("1.3.0");
        assert version.bump(Bump.PATCH).toString().equals("1.2.4");
        assert version.bump(Bump.NONE) == version;
    }

    @Test
    void order() {
        assert Bump.NONE.max(Bump.PATCH) == Bump.PATCH;
        assert Bump.PATCH.max(Bump.MINOR) == Bump.MINOR;
        assert Bump.MINOR.max(Bump.MAJOR) == Bump.MAJOR;
        assert Bump.MAJOR.max(Bump.PATCH) == Bump.MAJOR;
    }

    @Test
    void compare() {
        assert new SemanticVersion("1.2.3").compareTo(new SemanticVersion("1.2.4")) < 0;
        assert new SemanticVersion("1.3.0").compareTo(new SemanticVersion("1.2.9")) > 0;
        assert new SemanticVersion("2.0.0").compareTo(new SemanticVersion("1.9.9")) > 0;
        assert new SemanticVersion("1.2.3").compareTo(new SemanticVersion("1.2.3")) == 0;
    }
}
