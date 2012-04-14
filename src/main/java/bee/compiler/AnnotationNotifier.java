/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.compiler;

/**
 * <p>
 * Notifiable user interface.
 * </p>
 * 
 * @version 2011/03/23 16:46:09
 */
public interface AnnotationNotifier {

    /**
     * <p>
     * Talk to user.
     * </p>
     * 
     * @param messages Your message.
     */
    void notice(Object... messages);

    /**
     * <p>
     * Warn to user.
     * </p>
     * 
     * @param messages Your warning message.
     */
    void warn(Object... messages);

    /**
     * <p>
     * Declare a state of emergency.
     * </p>
     * 
     * @param message Your emergency message.
     */
    void error(Object... messages);
}
