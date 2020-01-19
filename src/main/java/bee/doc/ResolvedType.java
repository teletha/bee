/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc;

/**
 * Completed resolved type.
 */
public class ResolvedType {

    public String typeName = "";

    public String enclosingName = "";

    public String packageName = "";

    public String moduleName = "";

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ResolvedType [typeName=" + typeName + ", enclosingName=" + enclosingName + ", packageName=" + packageName + ", moduleName=" + moduleName + "]";
    }
}