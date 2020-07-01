/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.coder;

/**
 * @version 2015/07/13 15:21:51
 */
public class JavaCoder {

    /** The code buffer. */
    private final StringBuilder code = new StringBuilder();

    /**
     * <p>
     * Java source code writer.
     * </p>
     * 
     * @param packageName
     * @param className
     */
    public JavaCoder(String packageName, String className) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return code.toString();
    }
}