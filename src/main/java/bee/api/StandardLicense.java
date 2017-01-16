/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

/**
 * @version 2017/01/16 15:44:31
 */
public enum StandardLicense implements License {

    Apache("2.0", "Apache License, Version $", "https://opensource.org/licenses/Apache-2.0"),

    BSD("3-Clause ", "The $ BSD License", "https://opensource.org/licenses/BSD-2-Clause"),

    EPL("1.0", "Eclipse Public License $", "https://opensource.org/licenses/EPL-1.0"),

    GPL("3.0", "GNU General Public License version 3", "https://opensource.org/licenses/GPL-3.0"),

    LGPL("3.0", "GNU Lesser General Public License version $", "https://opensource.org/licenses/LGPL-3.0"),

    MIT("", "The MIT License", "https://opensource.org/licenses/MIT"),

    MPL("2.0", "Mozilla Public License $", "https://opensource.org/licenses/MPL-2.0");

    /** The license identifier. */
    private final String id;

    /** The license full name. */
    private final String full;

    /** The license uri. */
    private final String uri;

    /**
     * <p>
     * Create {@link License}.
     * </p>
     * 
     * @param version
     * @param full
     * @param uri
     */
    private StandardLicense(String version, String full, String uri) {
        this.id = name() + (version.isEmpty() ? "" : "-" + version);
        this.full = full.replace("$", version);
        this.uri = uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String identifier() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String fullName() {
        return full;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String uri() {
        return uri;
    }
}
