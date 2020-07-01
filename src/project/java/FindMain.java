
/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
import bee.BeeInstaller;

public class FindMain extends bee.task.FindMain {

    {
        main = BeeInstaller.class.getName();
    }
}