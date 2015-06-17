/*
 * Copyright (C) 2015 Nameless Production Committee
 * 
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

/**
 * @version 2015/06/16 12:11:21
 */
public enum StandardLicense implements License {

    Apache("http://opensource.org/licenses/Apache-2.0"),

    BSD("http://opensource.org/licenses/BSD-2-Clause"),

    EPL("http://opensource.org/licenses/EPL-1.0"),

    GPL("http://opensource.org/licenses/GPL-3.0"),

    LGPL("http://opensource.org/licenses/LGPL-3.0"),

    MIT("http://opensource.org/licenses/mit-license.php"),

    MPL("http://opensource.org/licenses/MPL-2.0");

    /** The license uri. */
    private final String uri;

    /**
     * @param uii
     */
    private StandardLicense(String uii) {
        this.uri = uii;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String uri() {
        return uri;
    }
}
