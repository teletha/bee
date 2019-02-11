/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import org.eclipse.egit.github.core.client.GitHubClient;

import bee.api.Command;
import bee.api.Task;

/**
 * 
 */
public class GitHub extends Task {

    @Command(value = "Upload project  in GitHub's repository")
    public void upload() {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token("");
    }
}
