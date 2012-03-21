/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */

/**
 * @version 2012/03/20 15:45:08
 */
public class Project extends bee.definition.Project {

    {
        require("npc", "sinobu", "0.9.1");
        require("npc", "antibug", "0.2").atTest();
    }
}
