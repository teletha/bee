/*
 * Copyright (C) 2025 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import bee.api.Command;

public interface SameName extends Task {
    @Command("")
    default String command() {
        return "sameName in same package";
    }
}