/*
 * Copyright (C) 2022 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

@SuppressWarnings("serial")
public class TaskCancel extends RuntimeException {

    /**
     * @param message
     */
    public TaskCancel(String message) {
        super(message);
    }
}