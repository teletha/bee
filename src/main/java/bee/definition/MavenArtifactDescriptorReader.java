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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.Builder;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

/**
 * @version 2012/03/25 20:25:11
 */
class MavenArtifactDescriptorReader implements ArtifactDescriptorReader {

    ArtifactResolver artifactResolver;

    ModelBuilder modelBuilder;

    RemoteRepositoryManager remoteRepositoryManager;

    RepositoryEventDispatcher repositoryEventDispatcher;

    VersionResolver versionResolver;

    /**
     * {@inheritDoc}
     */
    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(RepositorySystemSession session, ArtifactDescriptorRequest request)
            throws ArtifactDescriptorException {
        ArtifactDescriptorResult result = new ArtifactDescriptorResult(request);

        Model model = loadPom(session, request, result);

        if (model != null) {
            ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

            for (Repository r : model.getRepositories()) {
                result.addRepository(MavenUtils.convert(r));
            }

            for (org.apache.maven.model.Dependency dependency : model.getDependencies()) {
                result.addDependency(convert(dependency, stereotypes));
            }

            DependencyManagement mngt = model.getDependencyManagement();
            if (mngt != null) {
                for (org.apache.maven.model.Dependency dependency : mngt.getDependencies()) {
                    result.addManagedDependency(convert(dependency, stereotypes));
                }
            }

            Map<String, Object> properties = new LinkedHashMap<String, Object>();

            Prerequisites prerequisites = model.getPrerequisites();
            if (prerequisites != null) {
                properties.put("prerequisites.maven", prerequisites.getMaven());
            }

            List<License> licenses = model.getLicenses();
            properties.put("license.count", Integer.valueOf(licenses.size()));
            for (int i = 0; i < licenses.size(); i++) {
                License license = licenses.get(i);
                properties.put("license." + i + ".name", license.getName());
                properties.put("license." + i + ".url", license.getUrl());
                properties.put("license." + i + ".comments", license.getComments());
                properties.put("license." + i + ".distribution", license.getDistribution());
            }

            result.setProperties(properties);
        }

        return result;
    }

    /**
     * @param session
     * @param request
     * @param result
     * @return
     * @throws ArtifactDescriptorException
     */
    private Model loadPom(RepositorySystemSession session, ArtifactDescriptorRequest request, ArtifactDescriptorResult result)
            throws ArtifactDescriptorException {
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        Set<String> visited = new LinkedHashSet<String>();
        for (Artifact artifact = request.getArtifact();;) {
            try {
                VersionRequest versionRequest = new VersionRequest(artifact, request.getRepositories(), request.getRequestContext());
                versionRequest.setTrace(trace);
                VersionResult versionResult = versionResolver.resolveVersion(session, versionRequest);

                artifact = artifact.setVersion(versionResult.getVersion());
            } catch (VersionResolutionException e) {
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            if (!visited.add(artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getBaseVersion())) {
                RepositoryException exception = new RepositoryException("Artifact relocations form a cycle: " + visited);
                invalidDescriptor(session, trace, artifact, exception);

                result.addException(exception);
                throw new ArtifactDescriptorException(result);
            }

            Artifact pomArtifact = MavenUtils.convert(artifact);

            ArtifactResult resolveResult;
            try {
                ArtifactRequest resolveRequest = new ArtifactRequest(pomArtifact, request.getRepositories(), request.getRequestContext());
                resolveRequest.setTrace(trace);
                resolveResult = artifactResolver.resolveArtifact(session, resolveRequest);
                pomArtifact = resolveResult.getArtifact();
                result.setRepository(resolveResult.getRepository());
            } catch (ArtifactResolutionException e) {
                if (e.getCause() instanceof ArtifactNotFoundException) {
                    missingDescriptor(session, trace, artifact, (Exception) e.getCause());
                }
                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            Model model;
            try {
                ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
                modelRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                modelRequest.setProcessPlugins(false);
                modelRequest.setTwoPhaseBuilding(false);
                modelRequest.setSystemProperties(toProperties(session.getUserProperties(), session.getSystemProperties()));
                modelRequest.setModelCache(MavenModelCache.newInstance(session));
                modelRequest.setModelResolver(new MavenModelResolver(session, trace.newChild(modelRequest), request.getRequestContext(), artifactResolver, remoteRepositoryManager, request.getRepositories()));
                if (resolveResult.getRepository() instanceof WorkspaceRepository) {
                    modelRequest.setPomFile(pomArtifact.getFile());
                } else {
                    modelRequest.setModelSource(new FileModelSource(pomArtifact.getFile()));
                }

                model = modelBuilder.build(modelRequest).getEffectiveModel();
            } catch (ModelBuildingException e) {
                for (ModelProblem problem : e.getProblems()) {
                    if (problem.getException() instanceof UnresolvableModelException) {
                        result.addException(problem.getException());
                        throw new ArtifactDescriptorException(result);
                    }
                }
                invalidDescriptor(session, trace, artifact, e);

                result.addException(e);
                throw new ArtifactDescriptorException(result);
            }

            Relocation relocation = getRelocation(model);

            if (relocation != null) {
                result.addRelocation(artifact);
                artifact = new MavenRelocatedArtifact(artifact, relocation.getGroupId(), relocation.getArtifactId(), relocation.getVersion());
                result.setArtifact(artifact);
            } else {
                return model;
            }
        }
    }

    /**
     * @param dominant
     * @param recessive
     * @return
     */
    private Properties toProperties(Map<String, String> dominant, Map<String, String> recessive) {
        Properties props = new Properties();
        if (recessive != null) {
            props.putAll(recessive);
        }
        if (dominant != null) {
            props.putAll(dominant);
        }
        return props;
    }

    /**
     * @param model
     * @return
     */
    private Relocation getRelocation(Model model) {
        Relocation relocation = null;
        DistributionManagement distMngt = model.getDistributionManagement();
        if (distMngt != null) {
            relocation = distMngt.getRelocation();
        }
        return relocation;
    }

    /**
     * @param dependency
     * @param stereotypes
     * @return
     */
    private Dependency convert(org.apache.maven.model.Dependency dependency, ArtifactTypeRegistry stereotypes) {
        ArtifactType stereotype = stereotypes.get(dependency.getType());
        if (stereotype == null) {
            stereotype = new DefaultArtifactType(dependency.getType());
        }

        boolean system = dependency.getSystemPath() != null && dependency.getSystemPath().length() > 0;

        Map<String, String> props = null;
        if (system) {
            props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, dependency.getSystemPath());
        }

        Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), null, dependency.getVersion(), props, stereotype);

        List<Exclusion> exclusions = new ArrayList<Exclusion>(dependency.getExclusions().size());
        for (org.apache.maven.model.Exclusion exclusion : dependency.getExclusions()) {
            exclusions.add(convert(exclusion));
        }

        Dependency result = new Dependency(artifact, dependency.getScope(), dependency.isOptional(), exclusions);

        return result;
    }

    /**
     * @param exclusion
     * @return
     */
    private Exclusion convert(org.apache.maven.model.Exclusion exclusion) {
        return new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*");
    }

    /**
     * @param session
     * @param trace
     * @param artifact
     * @param exception
     */
    private void missingDescriptor(RepositorySystemSession session, RequestTrace trace, Artifact artifact, Exception exception) {
        Builder builder = new RepositoryEvent.Builder(session, EventType.ARTIFACT_DESCRIPTOR_MISSING);
        builder.setArtifact(artifact).setException(exception).setTrace(trace);

        repositoryEventDispatcher.dispatch(builder.build());
    }

    /**
     * @param session
     * @param trace
     * @param artifact
     * @param exception
     */
    private void invalidDescriptor(RepositorySystemSession session, RequestTrace trace, Artifact artifact, Exception exception) {
        Builder builder = new RepositoryEvent.Builder(session, EventType.ARTIFACT_DESCRIPTOR_INVALID);
        builder.setArtifact(artifact).setException(exception).setTrace(trace);

        repositoryEventDispatcher.dispatch(builder.build());
    }
}
