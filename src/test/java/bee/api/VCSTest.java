/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import org.junit.jupiter.api.Test;

class VCSTest {

    @Test
    void github() {
        VCS vcs = VCS.of("https://github.com/teletha/bee");

        assert vcs != null;
        assert vcs.name().equals("github");
        assert vcs.owner.equals("teletha");
        assert vcs.repo.equals("bee");
        assert vcs.domain().equals("com/github/teletha/bee");
        assert vcs.uri().equals("https://github.com/teletha/bee");
        assert vcs.issue().equals("https://github.com/teletha/bee/issues");
    }

    @Test
    void stripGitSuffix() {
        VCS vcs = VCS.of("https://github.com/teletha/bee.git");

        assert vcs != null;
        assert vcs.repo.equals("bee");
    }

    @Test
    void unsupportedHost() {
        assert VCS.of("https://gitlab.com/teletha/bee") == null;
    }

    @Test
    void invalidInput() {
        assert VCS.of(null) == null;
        assert VCS.of("") == null;
        assert VCS.of("   ") == null;
    }
}
