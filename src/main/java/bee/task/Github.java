/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import static bee.TaskOperations.*;

import java.util.List;

import bee.Task;
import bee.api.Command;
import bee.api.VCS;
import bee.util.GithubAPI;

public interface Github extends Task<GithubAPI.Settings> {

    @Command("Create repository and connect it.")
    default void create() {
        boolean privateRepository = ui().ask("Select the repository visibility.", List.of("public", "private")).equals("private");
        GithubAPI.createRepository(project().getProduct(), privateRepository);
        require(Github::connect);
        require(Github::configure);
    }

    @Command("Connect the local project with the GitHub repository.")
    default void connect() {
        VCS vcs = project().getVersionControlSystem();
        String url = vcs != null && vcs.name().equals("github") ? vcs.uri() + ".git" : GithubAPI.repositoryUrl(project().getProduct());
        GithubAPI.connect(url, project().getRoot());
    }

    @Command("Configure the GitHub repository.")
    default void configure() {
        GithubAPI.configure(project().getProduct(), config());
    }
}
