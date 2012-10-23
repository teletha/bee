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
 * @version 2012/10/11 10:05:35
 */
public class Project extends bee.api.Project {

    @Bind
    public String projectName;

    @Bind
    public String productName;

    @Bind
    public String version;

    {
        name(projectName, productName, version);
    }
}
