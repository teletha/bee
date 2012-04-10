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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.installation.InstallRequest;

/**
 * @version 2012/04/10 14:04:01
 */
class MavenVersionsMetadataGeneratorFactory implements MetadataGeneratorFactory {

    /**
     * {@inheritDoc}
     */
    public MetadataGenerator newInstance(RepositorySystemSession session, InstallRequest request) {
        return new MavenVersionsMetadataGenerator(session, request);
    }

    /**
     * {@inheritDoc}
     */
    public MetadataGenerator newInstance(RepositorySystemSession session, DeployRequest request) {
        return new MavenVersionsMetadataGenerator(session, request);
    }

    /**
     * {@inheritDoc}
     */
    public float getPriority() {
        return 5;
    }
}
