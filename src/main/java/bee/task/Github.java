/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.service.RepositoryService;

import bee.api.Command;
import bee.api.Task;

/**
 * @version 2017/01/17 10:55:11
 */
public class Github extends Task {

    /** The github repository id. */
    private final RepositoryId id = new RepositoryId(project.getGroup(), project.getProduct());

    /** The github repository operator. */
    private final RepositoryService service = new RepositoryService();

    /**
     * <p>
     * Create GitHub repository.
     * </p>
     *
     * @throws IOException
     */
    @Command("Generate GitHub repository.")
    public void release() throws Exception {
        RepositoryTag current = findCurrentRelease();

        if (current == null) {
            // create current release
        }
    }

    /**
     * <p>
     * Find the current release tag.
     * </p>
     *
     * @return
     * @throws Exception
     */
    private RepositoryTag findCurrentRelease() throws Exception {
        List<RepositoryTag> tags = service.getTags(id);

        for (RepositoryTag tag : tags) {
            if (tag.getName().equals(project.getVersion())) {
                return tag;
            }
        }
        return null;
    }
}
