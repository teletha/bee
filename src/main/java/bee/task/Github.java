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

import bee.api.Command;
import bee.api.Task;
import bee.util.Config;
import bee.util.Config.Description;

/**
 * @version 2017/01/17 10:55:11
 */
public class Github extends Task {

    /** The account manager. */
    private final GitHubAccount account = Config.project(GitHubAccount.class);

    /**
     * <p>
     * Create GitHub repository.
     * </p>
     *
     * @throws IOException
     */
    @Command("Generate GitHub repository.")
    public void release() throws Exception {
    }

    /**
     * @version 2017/01/18 9:52:56
     */
    @Description("Github Account")
    private static class GitHubAccount {

        /** The login password */
        public String password;
    }
}
