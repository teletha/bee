/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.definition;

import kiss.I;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.eclipse.aether.connector.wagon.WagonProvider;

/**
 * @version 2012/03/25 20:46:37
 */
class MavenWagonProvider implements WagonProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public Wagon lookup(String scheme) throws Exception {
        if ("http".equals(scheme)) {
            return new LightweightHttpWagon();
        } else if ("https".equals(scheme)) {
            return new LightweightHttpsWagon();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(Wagon wagon) {
        try {
            wagon.disconnect();
        } catch (ConnectionException e) {
            throw I.quiet(e);
        }
    }
}