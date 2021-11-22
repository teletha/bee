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

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import bee.Task;
import bee.api.Command;
import bee.api.VCS;
import kiss.I;

public class Release extends Task {

    @Command("Release the artifact")
    public void release() {
        VCS vcs = project.getVersionControlSystem();

        if (vcs == null) {
            ui.info("No version control system.");
        } else {
            try (Repository repository = new FileRepository(".git")) {
                Ref head = repository.exactRef("refs/heads/master");

                // a RevWalk allows to walk over commits based on some filtering that is defined
                try (RevWalk walk = new RevWalk(repository)) {
                    RevCommit commit = walk.parseCommit(head.getObjectId());
                    System.out.println("Start-Commit: " + commit);

                    System.out.println("Walking all commits starting at HEAD");
                    walk.markStart(commit);
                    int count = 0;
                    for (RevCommit rev : walk) {
                        System.out.println("Commit: " + rev.getFullMessage().strip());
                        count++;
                    }
                    System.out.println(count);

                    walk.dispose();
                }
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
    }
}
