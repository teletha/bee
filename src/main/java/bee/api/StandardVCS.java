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

import java.net.URI;

/**
 * @version 2017/01/10 3:15:42
 */
abstract class StandardVCS implements VersionControlSystem {

    /** The VCS uri. */
    private final URI uri;

    /**
     * @param uri
     */
    protected StandardVCS(URI uri) {
        this.uri = uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String uri() {
        return uri.toASCIIString();
    }

    /**
     * <p>
     * Select standard VCS.
     * </p>
     * 
     * @param uri
     * @return
     */
    static VersionControlSystem of(URI uri) {
        switch (uri.getHost()) {
        case "github.com":
            return new GitHub(uri);

        default:
            return null;
        }
    }

    /**
     * @version 2017/01/10 3:16:13
     */
    static class GitHub extends StandardVCS {

        /**
         * @param uri
         */
        private GitHub(URI uri) {
            super(uri);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String issue() {
            return uri() + "/issues";
        }
    }
}
