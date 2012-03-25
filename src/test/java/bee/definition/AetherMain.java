/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.definition;

import java.util.Set;

import kiss.I;

/**
 * @version 2012/03/25 0:45:52
 */
public class AetherMain {

    public static void main(String[] args) {
        Repository concierge = I.make(Repository.class);
        concierge.setLocalRepository(I.locate("target/local-repo"));

        Set<Library> libraries = concierge.collectDependency(new Library("hibernate:hibernate-entitymanager:3.4.0.GA"));

        System.out.println(libraries);
    }
}
