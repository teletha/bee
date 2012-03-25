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

import static kiss.Element.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import kiss.I;
import kiss.Manageable;
import kiss.Singleton;

import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.composition.DefaultDependencyManagementImporter;
import org.apache.maven.model.inheritance.DefaultInheritanceAssembler;
import org.apache.maven.model.interpolation.StringSearchModelInterpolator;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.management.DefaultDependencyManagementInjector;
import org.apache.maven.model.management.DefaultPluginManagementInjector;
import org.apache.maven.model.normalization.DefaultModelNormalizer;
import org.apache.maven.model.path.DefaultModelPathTranslator;
import org.apache.maven.model.path.DefaultModelUrlNormalizer;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.DefaultUrlNormalizer;
import org.apache.maven.model.profile.DefaultProfileInjector;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.superpom.DefaultSuperPomProvider;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.connector.file.FileRepositoryConnectorFactory;
import org.eclipse.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.DefaultMetadataResolver;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.internal.impl.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.DefaultUpdateCheckManager;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaEffectiveScopeCalculator;
import org.eclipse.aether.util.graph.transformer.NearestVersionConflictResolver;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

import bee.repository.ConsoleRepositoryListener;
import bee.repository.ConsoleTransferListener;
import demo.manual.DefaultArtifactDescriptorReader;
import demo.manual.DefaultVersionRangeResolver;
import demo.manual.DefaultVersionResolver;
import demo.manual.ManualWagonProvider;
import demo.util.ConsoleDependencyGraphDumper;

/**
 * @version 2012/03/25 14:55:21
 */
@Manageable(lifestyle = Singleton.class)
public class Concierge {

    /** The root repository system. */
    private final DefaultRepositorySystem system = new DefaultRepositorySystem();

    /** The repository event dispatcher. */
    private final DefaultRepositoryEventDispatcher repositoryEventDispatcher = new DefaultRepositoryEventDispatcher();

    /** The synchronous context. */
    private final DefaultSyncContextFactory syncContextFactory = new DefaultSyncContextFactory();

    /** The dependency collector. */
    private final DefaultDependencyCollector collector = new DefaultDependencyCollector();

    /** The file processor. */
    private final DefaultFileProcessor fileProcessor = new DefaultFileProcessor();

    /** The artifact installer. */
    private final DefaultInstaller installer = new DefaultInstaller();

    /** The update checker. */
    private final DefaultUpdateCheckManager updateCheckManager = new DefaultUpdateCheckManager();

    /** The artifact reader. */
    private final DefaultArtifactResolver artifactResolver = new DefaultArtifactResolver();

    /** he artifact description reader . */
    private final DefaultArtifactDescriptorReader artifactDescriptorReader = new DefaultArtifactDescriptorReader();

    /** The metadata resolver. */
    private final DefaultMetadataResolver metadataResolver = new DefaultMetadataResolver();

    /** The version resolver. */
    private final DefaultVersionResolver versionResolver = new DefaultVersionResolver();

    /** The version range resolver. */
    private final DefaultVersionRangeResolver versionRangeResolver = new DefaultVersionRangeResolver();

    /** The profile selector. */
    private final DefaultProfileSelector profileSelector = new DefaultProfileSelector();

    /** The profile injector. */
    private final DefaultProfileInjector profileInjector = new DefaultProfileInjector();

    /** The path translator. */
    private final DefaultPathTranslator pathTranslator = new DefaultPathTranslator();

    /** The plugin management injector. */
    private final DefaultPluginManagementInjector pluginManagementInjector = new DefaultPluginManagementInjector();

    /** The model builder. */
    private final DefaultModelBuilder modelBuilder = new DefaultModelBuilder();

    /** The model interpolator. */
    private final StringSearchModelInterpolator modelInterpolator = new StringSearchModelInterpolator();

    /** The model processor. */
    private final DefaultModelProcessor modelProcessor = new DefaultModelProcessor();

    /** The model reader. */
    private final DefaultModelReader modelReader = new DefaultModelReader();

    /** The model validator. */
    private final DefaultModelValidator modelValidator = new DefaultModelValidator();

    /** The super pom provider. */
    private final DefaultSuperPomProvider modelParentPomProvider = new DefaultSuperPomProvider();

    /** The inheritance model assembler. */
    private final DefaultInheritanceAssembler modelInheritanceAssembler = new DefaultInheritanceAssembler();

    /** The model path translator. */
    private final DefaultModelPathTranslator modelPathTranslator = new DefaultModelPathTranslator();

    /** The model normalizer. */
    private final DefaultModelNormalizer modelNormalizer = new DefaultModelNormalizer();

    /** The model url normalizer. */
    private final DefaultModelUrlNormalizer modelUrlNormalizer = new DefaultModelUrlNormalizer();

    /** The url normalizer. */
    private final DefaultUrlNormalizer urlNormalizer = new DefaultUrlNormalizer();

    /** The dependency management importer. */
    private final DefaultDependencyManagementImporter dependencyManagementImporter = new DefaultDependencyManagementImporter();

    /** The dependency management injector. */
    private final DefaultDependencyManagementInjector dependencyManagementInjector = new DefaultDependencyManagementInjector();

    /** The remote repository manager. */
    private final DefaultRemoteRepositoryManager remoteRepositoryManager = new DefaultRemoteRepositoryManager();

    /** The local repository provider. */
    private final DefaultLocalRepositoryProvider localRepositoryProvider = new DefaultLocalRepositoryProvider();

    /** The local repository manager factory. */
    private final LocalRepositoryManagerFactory localRepositoryManagerFactory = new EnhancedLocalRepositoryManagerFactory();

    /** The repository connector factory for wagon. */
    private final WagonRepositoryConnectorFactory wagonRepositoryConnectorFactory = new WagonRepositoryConnectorFactory();

    private final ManualWagonProvider wagonProvider = new ManualWagonProvider();

    /** The default dependency filter. */
    private final DependencySelector dependencyFilter = new AndDependencySelector(new DependencySelector[] {
            new OptionalDependencySelector(), // by option
            new ExclusionDependencySelector(), // by exclusions
            new ScopeDependencySelector(new String[] {"test", "provided"})}); // by scope

    /** The default dependency builder. */
    private final DependencyGraphTransformer dependencyBuilder = new ChainedDependencyGraphTransformer(new DependencyGraphTransformer[] {
            new ConflictMarker(), // resolve conflict
            new JavaEffectiveScopeCalculator(), // resolve scope
            new NearestVersionConflictResolver(), // resolve version conflict
            new JavaDependencyContextRefiner()}); // resolve duplication

    /** The path to local repository. */
    private LocalRepository localRepository;

    /** The path to remote repository. */
    private List<RemoteRepository> remoteRepositories = new CopyOnWriteArrayList();

    /**
     * Wiring components by hand.
     */
    private Concierge() {
        // ============ ArtifactResolver ============ //
        artifactResolver.setSyncContextFactory(syncContextFactory);
        artifactResolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        artifactResolver.setVersionResolver(versionResolver);
        artifactResolver.setRemoteRepositoryManager(remoteRepositoryManager);
        artifactResolver.setFileProcessor(fileProcessor);
        artifactResolver.setUpdateCheckManager(updateCheckManager);

        // ============ ArtifactDescriptionReader ============ //
        artifactDescriptorReader.setVersionResolver(versionResolver);
        artifactDescriptorReader.setArtifactResolver(artifactResolver);
        artifactDescriptorReader.setModelBuilder(modelBuilder);
        artifactDescriptorReader.setRepositoryEventDispatcher(repositoryEventDispatcher);
        artifactDescriptorReader.setRemoteRepositoryManager(remoteRepositoryManager);

        // ============ Installer ============ //
        installer.setFileProcessor(fileProcessor);
        installer.setRepositoryEventDispatcher(repositoryEventDispatcher);
        installer.setSyncContextFactory(syncContextFactory);

        // ============ DependencyCollector ============ //
        collector.setRemoteRepositoryManager(remoteRepositoryManager);
        collector.setVersionRangeResolver(versionRangeResolver);
        collector.setArtifactDescriptorReader(artifactDescriptorReader);

        // ============ VersionResolver ============ //
        versionResolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        versionResolver.setSyncContextFactory(syncContextFactory);
        versionResolver.setMetadataResolver(metadataResolver);

        // ============ MetadataResolver ============ //
        metadataResolver.setRemoteRepositoryManager(remoteRepositoryManager);
        metadataResolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        metadataResolver.setSyncContextFactory(syncContextFactory);
        metadataResolver.setUpdateCheckManager(updateCheckManager);

        // ============ ModelURLNormalizer ============ //
        modelUrlNormalizer.setUrlNormalizer(urlNormalizer);

        // ============ ModelBuilder ============ //
        modelBuilder.setProfileSelector(profileSelector);
        modelBuilder.setProfileInjector(profileInjector);
        modelBuilder.setModelProcessor(modelProcessor);
        modelBuilder.setModelValidator(modelValidator);
        modelBuilder.setSuperPomProvider(modelParentPomProvider);
        modelBuilder.setModelNormalizer(modelNormalizer);
        modelBuilder.setInheritanceAssembler(modelInheritanceAssembler);
        modelBuilder.setModelUrlNormalizer(modelUrlNormalizer);
        modelBuilder.setDependencyManagementImporter(dependencyManagementImporter);
        modelBuilder.setDependencyManagementInjector(dependencyManagementInjector);
        modelBuilder.setModelPathTranslator(modelPathTranslator);
        modelBuilder.setPluginManagementInjector(pluginManagementInjector);
        modelBuilder.setModelInterpolator(modelInterpolator);

        // ============ ModelInterpolator ============ //
        modelInterpolator.setPathTranslator(pathTranslator);
        modelInterpolator.setUrlNormalizer(urlNormalizer);

        // ============ ModelProcessor ============ //
        modelProcessor.setModelReader(modelReader);

        // ============ SuperPOMProvider ============ //
        modelParentPomProvider.setModelProcessor(modelProcessor);

        // ============ LocalRepositoryProvider ============ //
        localRepositoryProvider.addLocalRepositoryManagerFactory(localRepositoryManagerFactory);

        // ============ RemoteRepositoryManger ============ //
        remoteRepositoryManager.setUpdateCheckManager(updateCheckManager);
        remoteRepositoryManager.addRepositoryConnectorFactory(new FileRepositoryConnectorFactory());
        remoteRepositoryManager.addRepositoryConnectorFactory(wagonRepositoryConnectorFactory);

        // ============ WagonConnector ============ //
        wagonRepositoryConnectorFactory.setWagonProvider(wagonProvider);
        wagonRepositoryConnectorFactory.setFileProcessor(fileProcessor);

        // ============ RepositorySystem ============ //
        system.setLocalRepositoryProvider(localRepositoryProvider);
        system.setDependencyCollector(collector);
        system.setArtifactResolver(artifactResolver);
        system.setArtifactDescriptorReader(artifactDescriptorReader);
        system.setVersionRangeResolver(versionRangeResolver);
        system.setDependencyCollector(collector);
        system.setSyncContextFactory(syncContextFactory);
        system.setVersionResolver(versionResolver);
        system.setMetadataResolver(metadataResolver);
        system.setInstaller(installer);

        // ==================================================
        // Initialize
        // ==================================================
        setLocalRepository(searchLocalRepository());
        addRemoteRepository("central", "http://repo1.maven.org/maven2/");
    }

    /**
     * <p>
     * Create maven like new session.
     * </p>
     * 
     * @return
     */
    private RepositorySystemSession newSession() {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setDependencySelector(dependencyFilter);
        session.setDependencyGraphTransformer(dependencyBuilder);
        session.setProxySelector(new DefaultProxySelector());
        session.setMirrorSelector(new DefaultMirrorSelector());
        session.setDependencyTraverser(new FatArtifactTraverser());
        session.setDependencyManager(new ClassicDependencyManager());
        session.setAuthenticationSelector(new DefaultAuthenticationSelector());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepository));

        // event listener
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        // file types registry
        DefaultArtifactTypeRegistry stereotypes = new DefaultArtifactTypeRegistry();
        stereotypes.add(new DefaultArtifactType("pom"));
        stereotypes.add(new DefaultArtifactType("maven-plugin", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("jar", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("ejb", "jar", "", "java"));
        stereotypes.add(new DefaultArtifactType("ejb-client", "jar", "client", "java"));
        stereotypes.add(new DefaultArtifactType("test-jar", "jar", "tests", "java"));
        stereotypes.add(new DefaultArtifactType("javadoc", "jar", "javadoc", "java"));
        stereotypes.add(new DefaultArtifactType("java-source", "jar", "sources", "java", false, false));
        stereotypes.add(new DefaultArtifactType("war", "war", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("ear", "ear", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("rar", "rar", "", "java", false, true));
        stereotypes.add(new DefaultArtifactType("par", "par", "", "java", false, true));
        session.setArtifactTypeRegistry(stereotypes);

        // API definition
        return session;
    }

    public void resolve(Library library) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(library.artifact);

        for (RemoteRepository remote : remoteRepositories) {
            request.addRepository(remote);
        }

        try {
            ArtifactResult result = system.resolveArtifact(newSession(), request);

            System.out.println(result.getArtifact().getFile());
        } catch (ArtifactResolutionException e) {
            throw I.quiet(e);
        }
    }

    public void collectDependency(Library library) {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(library.artifact, ""));
        collectRequest.setRequestContext(JavaScopes.COMPILE);

        for (RemoteRepository remote : remoteRepositories) {
            collectRequest.addRepository(remote);
        }

        try {
            CollectResult collectResult = system.collectDependencies(newSession(), collectRequest);

            collectResult.getRoot().accept(new ConsoleDependencyGraphDumper());
        } catch (DependencyCollectionException e) {
            throw I.quiet(e);
        }

    }

    /**
     * Set the local property of this {@link Concierge}.
     * 
     * @param path The local value to set.
     */
    public void setLocalRepository(Path path) {
        this.localRepository = new LocalRepository(path.toAbsolutePath().toString());
    }

    /**
     * <p>
     * Add remote repository.
     * </p>
     * 
     * @param name
     * @param url
     */
    public void addRemoteRepository(String name, String url) {
        remoteRepositories.add(new RemoteRepository(name, "default", url));
    }

    /**
     * <p>
     * Search maven home directory.
     * </p>
     * 
     * @return
     */
    private static Path searchMavenHome() {
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("path")) {
                for (String path : entry.getValue().split(File.pathSeparator)) {
                    Path mvn = I.locate(path).resolve("mvn");

                    if (Files.exists(mvn)) {
                        return mvn.getParent().getParent();
                    }
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * Search maven local respository.
     * </p>
     * 
     * @return
     */
    private static Path searchLocalRepository() {
        Path local = Paths.get(System.getProperty("user.home"), ".m2");
        Path home = searchMavenHome();

        if (home == null) {
            // maven is not found
            return local;
        } else {
            // maven is here
            Path conf = home.resolve("conf/settings.xml");

            if (Files.exists(conf)) {
                String path = $(conf).find("localRepository").text();

                if (path.length() != 0) {
                    return I.locate(path);
                }
            }

            // user custom local repository is not found
            return local;
        }
    }

}
