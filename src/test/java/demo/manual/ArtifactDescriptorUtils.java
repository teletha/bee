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

import org.apache.maven.model.Repository;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;

/**
 * @version 2012/03/25 10:49:40
 */
public class ArtifactDescriptorUtils {

    public static Artifact toPomArtifact(Artifact artifact) {
        Artifact pomArtifact = artifact;

        if (pomArtifact.getClassifier().length() > 0 || !"pom".equals(pomArtifact.getExtension())) {
            pomArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "pom", artifact.getVersion());
        }

        return pomArtifact;
    }

    public static RemoteRepository toRemoteRepository(Repository repository) {
        RemoteRepository result = new RemoteRepository(repository.getId(), repository.getLayout(), repository.getUrl());
        result.setPolicy(true, toRepositoryPolicy(repository.getSnapshots()));
        result.setPolicy(false, toRepositoryPolicy(repository.getReleases()));
        return result;
    }

    public static RepositoryPolicy toRepositoryPolicy(org.apache.maven.model.RepositoryPolicy policy) {
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

        return new RepositoryPolicy(enabled, updates, checksums);
    }

}
