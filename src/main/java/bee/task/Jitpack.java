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
import java.nio.file.Path;
import java.util.HashMap;

import bee.api.Command;
import bee.api.Github;
import bee.api.Task;
import bee.util.Config;
import bee.util.Config.Description;
import bee.util.RESTClient;
import kiss.Events;
import kiss.Variable;

/**
 * @version 2017/01/17 10:55:11
 */
public class Jitpack extends Task {

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
        // create pom file
        Path pom = makeFile(project.getRoot().resolve("pom.xml"), project.toString());

        Github github = project.getVersionControlSystem();

        // check equality of pom files
        if (github.checkSame(pom)) {
            System.out.println("SAME");
        } else {
            System.out.println("NON");
        }

        // retrieve latest commit
        github.release().to(r -> {
            System.out.println(r);
        }, e -> {
            e.printStackTrace();
        });

        // check jitpack build
        String build = "https://jitpack.io/api/builds/com.github." + github.owner + "/" + github.repo;
        RESTClient client = new RESTClient();

        Variable<String> variable = client.get("https://jitpack.io/api/builds/com.github." + github.owner + "/" + github.repo, new Builds())
                .map(b -> b.get("com.github." + github.owner).get(github.repo).get(project.getVersion()))
                .flatMap(v -> {
                    if (v == null) {
                        return Events.from("NULL");
                    } else if (v.equals("Error")) {
                        return client.delete(build + "/" + project.getVersion());
                    }
                    return Events.from(v);
                })
                .to();

        System.out.println(variable);
    }

    /**
     * @version 2017/01/18 9:52:56
     */
    @Description("Github Account")
    protected static interface GitHubAccount {

        /** The login password */
        String password();
    }

    /**
     * @version 2017/01/21 16:32:23
     */
    private static class Builds extends HashMap<String, Build> {
    }

    /**
     * @version 2017/01/21 10:47:29
     */
    private static class Build extends HashMap<String, Versions> {
    }

    /**
     * @version 2017/01/21 10:47:29
     */
    private static class Versions extends HashMap<String, String> {
    }
}
