/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import static kiss.Element.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import kiss.I;
import kiss.Manageable;

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
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.impl.MetadataGeneratorFactory;
import org.sonatype.aether.impl.internal.DefaultArtifactResolver;
import org.sonatype.aether.impl.internal.DefaultDependencyCollector;
import org.sonatype.aether.impl.internal.DefaultFileProcessor;
import org.sonatype.aether.impl.internal.DefaultInstaller;
import org.sonatype.aether.impl.internal.DefaultLocalRepositoryProvider;
import org.sonatype.aether.impl.internal.DefaultMetadataResolver;
import org.sonatype.aether.impl.internal.DefaultRemoteRepositoryManager;
import org.sonatype.aether.impl.internal.DefaultRepositoryEventDispatcher;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.impl.internal.DefaultSyncContextFactory;
import org.sonatype.aether.impl.internal.DefaultUpdateCheckManager;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManagerFactory;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.transfer.TransferCancelledException;
import org.sonatype.aether.transfer.TransferEvent;
import org.sonatype.aether.transfer.TransferListener;
import org.sonatype.aether.transfer.TransferResource;
import org.sonatype.aether.util.DefaultRepositoryCache;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.DefaultArtifactType;
import org.sonatype.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.sonatype.aether.util.artifact.SubArtifact;
import org.sonatype.aether.util.graph.manager.ClassicDependencyManager;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.sonatype.aether.util.graph.transformer.ConflictMarker;
import org.sonatype.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.sonatype.aether.util.graph.transformer.JavaEffectiveScopeCalculator;
import org.sonatype.aether.util.graph.transformer.NearestVersionConflictResolver;
import org.sonatype.aether.util.graph.traverser.FatArtifactTraverser;
import org.sonatype.aether.util.repository.DefaultAuthenticationSelector;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;
import org.sonatype.aether.util.repository.DefaultProxySelector;

import bee.UserInterface;

/**
 * @version 2012/03/25 14:55:21
 */
@Manageable(lifestyle = ProjectSpecific.class)
public class Repository {

    /** The current processing project. */
    private final Project project;

    /** The current processing user interface. */
    private final UserInterface ui;

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

    /** The list of metadata generator factory. */
    private final List<MetadataGeneratorFactory> metadataGeneratorFactories = new ArrayList();

    /** The dependency management importer. */
    private final DefaultDependencyManagementImporter dependencyManagementImporter = new DefaultDependencyManagementImporter();

    /** The dependency management injector. */
    private final DefaultDependencyManagementInjector dependencyManagementInjector = new DefaultDependencyManagementInjector();

    /** The remote repository manager. */
    private final DefaultRemoteRepositoryManager remoteRepositoryManager = new DefaultRemoteRepositoryManager();

    /** The local repository provider. */
    private final DefaultLocalRepositoryProvider localRepositoryProvider = new DefaultLocalRepositoryProvider();

    /** The remote repository provider. */
    private final List<RepositoryConnectorFactory> remoteRepositoryProvider = new ArrayList();

    /** The local repository manager factory. */
    private final LocalRepositoryManagerFactory localRepositoryManagerFactory = new SimpleLocalRepositoryManagerFactory();

    /** The repository connector factory for wagon. */
    private final WagonRepositoryConnectorFactory wagonRepositoryConnectorFactory = new WagonRepositoryConnectorFactory();

    /** The default dependency filter. */
    private final List<DependencySelector> dependencyFilters = new ArrayList();

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
    Repository(Project project, UserInterface ui) {
        this.project = project;
        this.ui = ui;

        // create filter
        dependencyFilters.add(new OptionalDependencySelector());
        dependencyFilters.add(new ScopeDependencySelector("test", "provided"));

        // create metadata factories
        metadataGeneratorFactories.add(new VersionsMetadataGeneratorFactory());
        metadataGeneratorFactories.add(new SnapshotMetadataGeneratorFactory());

        // ============ ArtifactResolver ============ //
        artifactResolver.setSyncContextFactory(syncContextFactory);
        artifactResolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        artifactResolver.setVersionResolver(versionResolver);
        artifactResolver.setRemoteRepositoryManager(remoteRepositoryManager);
        artifactResolver.setFileProcessor(fileProcessor);
        artifactResolver.setUpdateCheckManager(updateCheckManager);

        // ============ ArtifactDescriptionReader ============ //
        artifactDescriptorReader.setArtifactResolver(artifactResolver);
        artifactDescriptorReader.setModelBuilder(modelBuilder);
        artifactDescriptorReader.setRemoteRepositoryManager(remoteRepositoryManager);
        artifactDescriptorReader.setRepositoryEventDispatcher(repositoryEventDispatcher);
        artifactDescriptorReader.setVersionResolver(versionResolver);

        // ============ Installer ============ //
        installer.setFileProcessor(fileProcessor);
        installer.setMetadataFactories(metadataGeneratorFactories);
        installer.setRepositoryEventDispatcher(repositoryEventDispatcher);
        installer.setSyncContextFactory(syncContextFactory);

        // ============ DependencyCollector ============ //
        collector.setRemoteRepositoryManager(remoteRepositoryManager);
        collector.setVersionRangeResolver(versionRangeResolver);
        collector.setArtifactDescriptorReader(artifactDescriptorReader);

        // ============ VersionResolver ============ //
        versionResolver.setMetadataResolver(metadataResolver);
        versionResolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        versionResolver.setSyncContextFactory(syncContextFactory);

        // ============ VersionRangeResolver ============ //
        versionRangeResolver.setMetadataResolver(metadataResolver);
        versionRangeResolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        versionRangeResolver.setSyncContextFactory(syncContextFactory);

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

        // ============ RemoteRepositoryProvider ============ //
        remoteRepositoryProvider.add(new FileRepositoryConnectorFactory());
        remoteRepositoryProvider.add(wagonRepositoryConnectorFactory);

        // ============ RemoteRepositoryManger ============ //
        remoteRepositoryManager.setUpdateCheckManager(updateCheckManager);
        remoteRepositoryManager.setRepositoryConnectorFactories(remoteRepositoryProvider);

        // ============ WagonConnector ============ //
        wagonRepositoryConnectorFactory.setWagonProvider(new BeeWagonProvider());
        wagonRepositoryConnectorFactory.setFileProcessor(fileProcessor);

        // ============ RepositorySystem ============ //
        system.setArtifactDescriptorReader(artifactDescriptorReader);
        system.setArtifactResolver(artifactResolver);
        system.setDependencyCollector(collector);
        system.setInstaller(installer);
        system.setLocalRepositoryProvider(localRepositoryProvider);
        system.setMetadataResolver(metadataResolver);
        system.setSyncContextFactory(syncContextFactory);
        system.setVersionResolver(versionResolver);
        system.setVersionRangeResolver(versionRangeResolver);

        // ==================================================
        // Initialize
        // ==================================================
        setLocalRepository(searchLocalRepository());
        addRemoteRepository("central", "http://repo1.maven.org/maven2/");
        addRemoteRepository("jboss", "http://repository.jboss.org/nexus/content/groups/public-jboss/");
    }

    /**
     * <p>
     * Collect all dependencies in the specified scope.
     * </p>
     * 
     * @param libraries
     * @param scope
     * @return
     */
    public Set<Library> collectDependency(Project project, Scope scope) {
        return collectDependency(project, scope, project.libraries);
    }

    /**
     * <p>
     * Collect all dependencies in the specified scope.
     * </p>
     * 
     * @param libraries
     * @param scope
     * @return
     */
    public Set<Library> collectDependency(String group, String product, String version, Scope scope) {
        return collectDependency(new Library(group, product, version), scope);
    }

    /**
     * <p>
     * Collect all dependencies in the specified scope.
     * </p>
     * 
     * @param libraries
     * @param scope
     * @return
     */
    public Set<Library> collectDependency(Library library, Scope scope) {
        return collectDependency(project, scope, Collections.singleton(library));
    }

    /**
     * <p>
     * Collect all dependencies in the specified scope.
     * </p>
     * 
     * @param libraries
     * @param scope
     * @return
     */
    private Set<Library> collectDependency(Project project, Scope scope, Set<Library> libraries) {
        Set<Library> set = new TreeSet();

        // collect remote repository
        List<RemoteRepository> repositories = new ArrayList();
        repositories.addAll(remoteRepositories);
        repositories.addAll(project.repositories);

        // dependency collector
        CollectRequest request = new CollectRequest();
        request.setRepositories(repositories);

        for (Library library : libraries) {
            request.addDependency(new Dependency(library.artifact, library.scope.toString()));
        }

        try {
            DependencyResult result = system.resolveDependencies(newSession(), new DependencyRequest(request, scope.getFilter()));

            for (ArtifactResult dependency : result.getArtifactResults()) {
                set.add(new Library(dependency.getRequest().getArtifact()));
            }

            return set;
        } catch (DependencyResolutionException e) {
            throw I.quiet(e);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Resolve the source codes of the specified library.
     * </p>
     * 
     * @param library
     * @return
     */
    public Path resolveSource(Library library) {
        // collect remote repository
        List<RemoteRepository> repositories = new ArrayList();
        repositories.addAll(remoteRepositories);
        repositories.addAll(project.repositories);

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new SubArtifact(library.artifact, "*-sources", "jar"));
        request.setRepositories(repositories);

        try {
            ArtifactResult result = system.resolveArtifact(newSession(), request);

            if (result.isResolved()) {
                return result.getArtifact().getFile().toPath();
            }

        } catch (ArtifactResolutionException e) {
            return null;
        }

        return null;
    }

    /**
     * <p>
     * Install project into the local repository.
     * </p>
     * 
     * @param project A project to install.
     * @return A installed location.
     */
    public Path install(Project project, Path file) {
        Path temp = I.locateTemporary();

        try {
            Files.write(temp, project.toString().getBytes());
        } catch (IOException e) {
            throw I.quiet(e);
        }

        DefaultArtifact jar = new DefaultArtifact(project.getGroup(), project.getProduct(), "", "jar", project.getVersion(), null, file.toFile());
        Artifact pom = new SubArtifact(jar, "", "pom", temp.toFile());

        InstallRequest request = new InstallRequest();
        request.addArtifact(jar);
        request.addArtifact(pom);

        try {
            system.install(newSession(), request);

            return null;
        } catch (InstallationException e) {
            e.printStackTrace();
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Get the local repository path.
     * </p>
     * 
     * @return
     */
    public final Path getLocalRepository() {
        return localRepository.getBasedir().toPath();
    }

    /**
     * Set the local property of this {@link Repository}.
     * 
     * @param path The local value to set.
     */
    public final void setLocalRepository(Path path) {
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
    public final void addRemoteRepository(String name, String url) {
        remoteRepositories.add(new RemoteRepository(name, "default", url));
    }

    /**
     * <p>
     * Create maven like new session.
     * </p>
     * 
     * @return
     */
    private RepositorySystemSession newSession() {
        Set<DependencySelector> filters = new HashSet(dependencyFilters);
        filters.add(new ExclusionDependencySelector(project.exclusions));

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setDependencySelector(new AndDependencySelector(filters));
        session.setDependencyGraphTransformer(dependencyBuilder);
        session.setProxySelector(new DefaultProxySelector());
        session.setMirrorSelector(new DefaultMirrorSelector());
        session.setDependencyTraverser(new FatArtifactTraverser());
        session.setDependencyManager(new ClassicDependencyManager());
        session.setAuthenticationSelector(new DefaultAuthenticationSelector());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepository));
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_DAILY);
        session.setCache(new DefaultRepositoryCache());
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        session.setIgnoreInvalidArtifactDescriptor(true);

        // event listener
        session.setTransferListener(I.make(TransferView.class));
        session.setRepositoryListener(I.make(RepositoryView.class));

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

    /**
     * @version 2012/03/25 23:08:57
     */
    private static final class RepositoryView implements RepositoryListener {

        /** The user notifier. */
        private final UserInterface ui;

        /**
         * @param ui
         */
        private RepositoryView(UserInterface ui) {
            this.ui = ui;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactDeployed(RepositoryEvent event) {
            ui.talk("Deployed " + event.getArtifact() + " to " + event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactDeploying(RepositoryEvent event) {
            ui.talk("Deploying " + event.getArtifact() + " to " + event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactDescriptorInvalid(RepositoryEvent event) {
            ui.talk("Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactDescriptorMissing(RepositoryEvent event) {
            ArtifactDescriptorRequest request = (ArtifactDescriptorRequest) event.getTrace().getData();
            ui.error(request.getArtifact(), " is not found at the following remote repositories.", request.getRepositories());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactInstalled(RepositoryEvent event) {
            ui.talk("Install " + event.getArtifact() + " to " + event.getFile());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactInstalling(RepositoryEvent event) {
            // ui.talk("Installing " + event.getArtifact() + " to " + event.getFile());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactResolved(RepositoryEvent event) {
            // ui.talk("Resolved artifact " + event.getArtifact() + " from " +
            // event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactDownloading(RepositoryEvent event) {
            // ui.talk("Downloading artifact " + event.getArtifact() + " from " +
            // event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactDownloaded(RepositoryEvent event) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactResolving(RepositoryEvent event) {
            // ui.talk("Resolving artifact " + event.getArtifact());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataDeployed(RepositoryEvent event) {
            ui.talk("Deployed " + event.getMetadata() + " to " + event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataDeploying(RepositoryEvent event) {
            ui.talk("Deploying " + event.getMetadata() + " to " + event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataInstalled(RepositoryEvent event) {
            // ui.talk("Installed " + event.getMetadata() + " to " + event.getFile());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataInstalling(RepositoryEvent event) {
            // ui.talk("Installing " + event.getMetadata() + " to " + event.getFile());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataInvalid(RepositoryEvent event) {
            ui.talk("Invalid metadata " + event.getMetadata());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataResolved(RepositoryEvent event) {
            // ui.talk("Resolved metadata " + event.getMetadata() + " from " +
            // event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataResolving(RepositoryEvent event) {
            // ui.talk("Resolving metadata " + event.getMetadata() + " from " +
            // event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataDownloading(RepositoryEvent event) {
            // ui.talk("Downloading metadata " + event.getMetadata() + " from " +
            // event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataDownloaded(RepositoryEvent event) {
            // ui.talk("Downloaded metadata " + event.getMetadata() + " from " +
            // event.getRepository());
        }
    }

    /**
     * @version 2012/04/13 15:51:37
     */
    private static final class TransferView implements TransferListener {

        /** The progress event interval. (ns) */
        private static final long interval = 200 * 1000 * 1000;

        /** The last progress event time. */
        private long last = 0;

        /** The actual console. */
        private UserInterface ui;

        /** The downloading items. */
        private Map<TransferResource, TransferEvent> downloading = new ConcurrentHashMap();

        /**
         * <p>
         * Injectable constructor.
         * </p>
         * 
         * @param ui A user interface to notify.
         */
        private TransferView(UserInterface ui) {
            this.ui = ui;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferInitiated(TransferEvent event) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferStarted(TransferEvent paramTransferEvent) throws TransferCancelledException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferProgressed(TransferEvent event) {
            // register current downloading artifact
            downloading.put(event.getResource(), event);

            long now = System.nanoTime();

            if (interval < now - last) {
                last = now; // update last event time

                // build message
                StringBuilder message = new StringBuilder();

                for (Map.Entry<TransferResource, TransferEvent> entry : downloading.entrySet()) {
                    TransferResource resource = entry.getKey();
                    String name = resource.getResourceName();

                    message.append(name.substring(name.lastIndexOf('/') + 1));
                    message.append(" (");
                    message.append(format(entry.getValue().getTransferredBytes(), resource.getContentLength()));
                    message.append(")   ");
                }
                message.append('\r');

                // notify
                ui.talk(message.toString());
            }
        }

        @Override
        public void transferSucceeded(TransferEvent event) {
            // unregister item
            downloading.remove(event.getResource());

            TransferResource resource = event.getResource();
            long contentLength = event.getTransferredBytes();
            if (contentLength >= 0) {
                String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
                String name = resource.getResourceName();
                String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

                ui.talk(type, ": ", name.substring(name.lastIndexOf('/') + 1), " (", len, ")");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferFailed(TransferEvent event) {
            // unregister item
            downloading.remove(event.getResource());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferCorrupted(TransferEvent event) {
            ui.error(event.getException());
        }

        /**
         * <p>
         * Format size.
         * </p>
         * 
         * @param current A current size.
         * @param size A total size.
         * @return
         */
        private static String format(long current, long size) {
            if (size >= 1024) {
                return toKB(current) + "/" + toKB(size) + " KB";
            } else if (size >= 0) {
                return current + "/" + size + " B";
            } else if (current >= 1024) {
                return toKB(current) + " KB";
            } else {
                return current + " B";
            }
        }

        /**
         * <p>
         * Format.
         * </p>
         * 
         * @param size
         * @return
         */
        private static long toKB(long size) {
            return (size + 1023) / 1024;
        }
    }

    /**
     * @version 2012/04/13 20:34:51
     */
    private static class BeeWagonProvider implements WagonProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public Wagon lookup(String scheme) throws Exception {
            if ("http".equals(scheme)) {
                return new LightweightHttpWagon();
            } else if ("https".equals(scheme)) {
                return new LightweightHttpsWagon();
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void release(Wagon wagon) {
            try {
                wagon.disconnect();
            } catch (ConnectionException e) {
                throw I.quiet(e);
            }
        }
    }
}
