/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.compiler.source01;

import bee.compiler.SourceAnnotation;

/**
 * Test Class
 * 
 * @version 2011/03/13 15:51:30
 */
@SourceAnnotation("Main")
public class MainClass {

    /**
     * Constructor
     */
    @SourceAnnotation
    private MainClass(Object parent) {
    }

    /**
     * Method
     * 
     * @param message
     */
    @SourceAnnotation("Method1")
    public void documented(String message) {
    }

    /*
     * Non Javadoc
     */
    @SourceAnnotation("Method2")
    public void not(String value, int type) {
    }
}
