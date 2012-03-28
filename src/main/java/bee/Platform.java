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

import java.nio.charset.Charset;

/**
 * <p>
 * Defint platform specific default configurations.
 * </p>
 * 
 * @version 2012/03/29 0:57:53
 */
public class Platform {

    /** The default encoding. */
    public static final Charset Encoding = Charset.forName(System.getProperty("file.encoding"));
}
