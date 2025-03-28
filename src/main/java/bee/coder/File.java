/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.coder;

import java.util.HashMap;
import java.util.Map;

class File implements FileType {

    /** The built-in file types. */
    static final Map<String, FileType> BUILTIN = new HashMap();

    /** The extension. */
    private final String extension;

    /** The header type. */
    private final HeaderStyle header;

    File() {
        this("", StandardHeaderStyle.Unknown);
    }

    File(String extension, HeaderStyle header) {
        this(extension, header, true);
    }

    File(String extension, HeaderStyle header, boolean builtin) {
        this.extension = extension;
        this.header = header;

        if (builtin) {
            BUILTIN.put(extension, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String extension() {
        return extension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HeaderStyle header() {
        return header;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "File[" + extension + "]";
    }
}