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

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;

/**
 * @version 2012/03/25 1:00:06
 */
public class ManualRepositorySystemFactory {

    public static RepositorySystem newRepositorySystem() {
        /*
         * Aether's components implement org.sonatype.aether.spi.locator.Service to ease manual
         * wiring and using the prepopulated DefaultServiceLocator, we only need to register the
         * repository connector factories.
         */
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.setServices(WagonProvider.class, new ManualWagonProvider());
        locator.setServices(VersionResolver.class, new DefaultVersionResolver());
        locator.setServices(VersionRangeResolver.class, new DefaultVersionRangeResolver());
        locator.setService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);

        return locator.getService(RepositorySystem.class);
    }

}