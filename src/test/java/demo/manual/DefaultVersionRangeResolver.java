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
 * @version 2012/03/25 9:58:34
 */
public class DefaultVersionRangeResolver implements VersionRangeResolver {

    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request)
            throws VersionRangeResolutionException {
        VersionRangeResult result = new VersionRangeResult(request);

        VersionScheme versionScheme = new GenericVersionScheme();

        VersionConstraint versionConstraint;
        try {
            versionConstraint = versionScheme.parseVersionConstraint(request.getArtifact().getVersion());
        } catch (InvalidVersionSpecificationException e) {
            result.addException(e);
            throw new VersionRangeResolutionException(result);
        }

        result.setVersionConstraint(versionConstraint);

        // if (versionConstraint.getRanges().isEmpty()) {
        result.addVersion(versionConstraint.getVersion());
        // } else {
        // Map<String, ArtifactRepository> versionIndex = getVersions(session, result, request);
        //
        // List<Version> versions = new ArrayList<Version>();
        // for (Map.Entry<String, ArtifactRepository> v : versionIndex.entrySet()) {
        // try {
        // Version ver = versionScheme.parseVersion(v.getKey());
        // if (versionConstraint.containsVersion(ver)) {
        // versions.add(ver);
        // result.setRepository(ver, v.getValue());
        // }
        // } catch (InvalidVersionSpecificationException e) {
        // result.addException(e);
        // }
        // }
        //
        // Collections.sort(versions);
        // result.setVersions(versions);
        // }

        return result;
    }
}
