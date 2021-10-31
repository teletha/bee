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

import org.apache.commons.lang3.RandomStringUtils;

import bee.BlinkProject;
import psychopath.Locator;

public class TemporaryProject extends BlinkProject {

    private static final ThreadLocal<Repository> REPO = ThreadLocal.withInitial(() -> {
        Repository repository = new Repository(new BlinkProject());
        repository.setLocalRepository(Locator.temporaryDirectory());

        return repository;
    });

    /**
     * 
     */
    public TemporaryProject() {
        this(RandomStringUtils.randomAlphabetic(20));
    }

    /**
     * 
     */
    public TemporaryProject(String name) {
        product("temporary.test.project", name, "1.0");

        setOutput(Locator.temporaryDirectory());
        locateJar().create();
    }

    /**
     * Declare dependency for project.
     * 
     * @param project Targetproject.
     * @return A dependency.
     */
    public Library require(Project project) {
        getRepository().install(project);

        return require(project.getGroup(), project.getProduct(), project.getVersion());
    }

    public Repository getRepository() {
        return REPO.get();
    }
}
