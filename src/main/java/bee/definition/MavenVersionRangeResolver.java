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
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

/**
 * @version 2012/03/25 20:40:54
 */
class MavenVersionRangeResolver implements VersionRangeResolver {

    /**
     * {@inheritDoc}
     */
    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request)
            throws VersionRangeResolutionException {
        VersionRangeResult result = new VersionRangeResult(request);
        VersionScheme scheme = new GenericVersionScheme();

        try {
            VersionConstraint constraint = scheme.parseVersionConstraint(request.getArtifact().getVersion());
            result.setVersionConstraint(constraint);
            result.addVersion(constraint.getVersion());

            // API definition
            return result;
        } catch (InvalidVersionSpecificationException e) {
            result.addException(e);
            throw new VersionRangeResolutionException(result);
        }
    }
}
