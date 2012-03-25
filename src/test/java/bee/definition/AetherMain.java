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

import kiss.I;

/**
 * @version 2012/03/25 0:45:52
 */
public class AetherMain {

    public static void main(String[] args) {
        Concierge concierge = I.make(Concierge.class);
        concierge.setLocalRepository(I.locate("target/local-repo"));

        concierge.collectDependency(new Library("hibernate:hibernate-entitymanager:3.4.0.GA"));
    }
}
