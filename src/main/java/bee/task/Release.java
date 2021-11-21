/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.util.List;

import bee.Task;
import bee.api.Command;
import bee.api.VCS;
import bee.api.VCS.Commit;

public class Release extends Task {

    @Command("Release the artifact")
    public void release() {
        VCS vcs = project.getVersionControlSystem();

        if (vcs == null) {
            ui.info("No version control system.");
        } else {
            List<Commit> commits = vcs.commits();
            for (Commit commit : commits) {
                System.out.println(commit.message);
            }
        }
    }
}
