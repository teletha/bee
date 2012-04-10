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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.metadata.Metadata;

/**
 * @version 2012/04/10 14:04:45
 */
class MavenVersionsMetadataGenerator implements MetadataGenerator {

    private Map<Object, MavenVersionsMetadata> versions;

    private Map<Object, MavenVersionsMetadata> processedVersions;

    /**
     * @param session
     * @param request
     */
    public MavenVersionsMetadataGenerator(RepositorySystemSession session, InstallRequest request) {
        this(session, request.getMetadata());
    }

    /**
     * @param session
     * @param request
     */
    public MavenVersionsMetadataGenerator(RepositorySystemSession session, DeployRequest request) {
        this(session, request.getMetadata());
    }

    /**
     * @param session
     * @param metadatas
     */
    private MavenVersionsMetadataGenerator(RepositorySystemSession session, Collection<? extends Metadata> metadatas) {
        versions = new LinkedHashMap<Object, MavenVersionsMetadata>();
        processedVersions = new LinkedHashMap<Object, MavenVersionsMetadata>();

        /*
         * NOTE: This should be considered a quirk to support interop with Maven's legacy
         * ArtifactDeployer which processes one artifact at a time and hence cannot associate the
         * artifacts from the same project to use the same version index. Allowing the caller to
         * pass in metadata from a previous deployment allows to re-establish the association
         * between the artifacts of the same project.
         */
        for (Iterator<? extends Metadata> it = metadatas.iterator(); it.hasNext();) {
            Metadata metadata = it.next();
            if (metadata instanceof MavenVersionsMetadata) {
                it.remove();
                MavenVersionsMetadata versionsMetadata = (MavenVersionsMetadata) metadata;
                processedVersions.put(versionsMetadata.getKey(), versionsMetadata);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<? extends Metadata> prepare(Collection<? extends Artifact> artifacts) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public Artifact transformArtifact(Artifact artifact) {
        return artifact;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<? extends Metadata> finish(Collection<? extends Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            Object key = MavenVersionsMetadata.getKey(artifact);
            if (processedVersions.get(key) == null) {
                MavenVersionsMetadata versionsMetadata = versions.get(key);
                if (versionsMetadata == null) {
                    versionsMetadata = new MavenVersionsMetadata(artifact);
                    versions.put(key, versionsMetadata);
                }
            }
        }

        return versions.values();
    }
}
