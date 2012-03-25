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

import org.apache.maven.model.Repository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * @version 2012/03/25 10:49:40
 */
class MavenUtils {

    /**
     * <p>
     * Create pom artifact.
     * </p>
     * 
     * @param artifact
     * @return
     */
    public static Artifact convert(Artifact artifact) {
        if (artifact.getClassifier().length() > 0 || !"pom".equals(artifact.getExtension())) {
            artifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom", artifact.getVersion());
        }

        // API definition
        return artifact;
    }

    /**
     * <p>
     * Convert maven repository to aether remote repository.
     * </p>
     * 
     * @param repository
     * @return
     */
    static RemoteRepository convert(Repository repository) {
        RemoteRepository converted = new RemoteRepository(repository.getId(), repository.getLayout(), repository.getUrl());
        converted.setPolicy(true, convert(repository.getSnapshots()));
        converted.setPolicy(false, convert(repository.getReleases()));
        return converted;
    }

    /**
     * <p>
     * Convert maven policy to aether policy.
     * </p>
     * 
     * @param policy
     * @return
     */
    private static RepositoryPolicy convert(org.apache.maven.model.RepositoryPolicy policy) {
        boolean enabled = true;
        String checksums = RepositoryPolicy.CHECKSUM_POLICY_WARN;
        String updates = RepositoryPolicy.UPDATE_POLICY_DAILY;

        if (policy != null) {
            enabled = policy.isEnabled();

            if (policy.getUpdatePolicy() != null) {
                updates = policy.getUpdatePolicy();
            }

            if (policy.getChecksumPolicy() != null) {
                checksums = policy.getChecksumPolicy();
            }
        }

        // API definition
        return new RepositoryPolicy(enabled, updates, checksums);
    }

}
