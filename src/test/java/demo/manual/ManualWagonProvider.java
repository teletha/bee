/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package demo.manual;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.sonatype.aether.connector.wagon.WagonProvider;

/**
 * @version 2012/03/25 1:00:39
 */
public class ManualWagonProvider implements WagonProvider {

    public Wagon lookup(String roleHint) throws Exception {
        if ("http".equals(roleHint)) {
            return new LightweightHttpWagon();
        }
        return null;
    }

    public void release(Wagon wagon) {

    }

}