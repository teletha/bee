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

/**
 * @version 2012/01/29 21:53:20
 */
public class ProjectSummary {

    protected Dependency depend_on(String group, String name, String version) {
        return null;
    }

    public static class Dependency {

    }

    /**
     * @version 2012/01/29 21:53:41
     */
    private static class PrjectName extends ProjectSummary {

        @Product
        void ProductName() {
            depend_on("1", "", "");
        }
    }
}
