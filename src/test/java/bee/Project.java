/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import bee.project.Product;

/**
 * @version 2012/02/20 10:13:05
 */
public class Project extends bee.project.Project {

    Product Bee = new Product("npc", "bee", "0.1") {

        {
            require("asm", "asm", "3.2");
        }
    };

    /**
     * @param args
     */
    public static void main(String[] args) {
        Project project = new Project();

    }
}
