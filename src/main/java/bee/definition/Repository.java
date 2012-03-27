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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
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
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.CollectRequest;
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
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.artifact.SubArtifact;
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
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

import bee.UserInterface;

/**
 * @version 2012/03/25 14:55:21
 */
@Manageable(lifestyle = Singleton.class)
class Repository {

    /** The current processing project. */
    private final Project project;

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
    private final MavenArtifactDescriptorReader artifactDescriptorReader = new MavenArtifactDescriptorReader();

    /** The metadata resolver. */
    private final DefaultMetadataResolver metadataResolver = new DefaultMetadataResolver();

    /** The version resolver. */
    private final MavenVersionResolver versionResolver = new MavenVersionResolver();

    /** The version range resolver. */
    private final MavenVersionRangeResolver versionRangeResolver = new MavenVersionRangeResolver();

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
    Repository(Project project) {
        this.project = project;

        // create filter
        dependencyFilters.add(new OptionalDependencySelector());
        dependencyFilters.add(new ScopeDependencySelector("test", "provided"));

        // ============ ArtifactResolver ============ //
        artifactResolver.setSyncContextFactory(syncContextFactory);
        artifactResolver.setRepositoryEventDispatcher(repositoryEventDispatcher);
        artifactResolver.setVersionResolver(versionResolver);
        artifactResolver.setRemoteRepositoryManager(remoteRepositoryManager);
        artifactResolver.setFileProcessor(fileProcessor);
        artifactResolver.setUpdateCheckManager(updateCheckManager);

        // ============ ArtifactDescriptionReader ============ //
        artifactDescriptorReader.artifactResolver = artifactResolver;
        artifactDescriptorReader.modelBuilder = modelBuilder;
        artifactDescriptorReader.remoteRepositoryManager = remoteRepositoryManager;
        artifactDescriptorReader.repositoryEventDispatcher = repositoryEventDispatcher;
        artifactDescriptorReader.versionResolver = versionResolver;

        // ============ Installer ============ //
        installer.setFileProcessor(fileProcessor);
        installer.setRepositoryEventDispatcher(repositoryEventDispatcher);
        installer.setSyncContextFactory(syncContextFactory);

        // ============ DependencyCollector ============ //
        collector.setRemoteRepositoryManager(remoteRepositoryManager);
        collector.setVersionRangeResolver(versionRangeResolver);
        collector.setArtifactDescriptorReader(artifactDescriptorReader);

        // ============ VersionResolver ============ //
        versionResolver.metadataResolver = metadataResolver;
        versionResolver.repositoryEventDispatcher = repositoryEventDispatcher;
        versionResolver.syncContextFactory = syncContextFactory;

        // ============ VersionRangeResolver ============ //
        versionRangeResolver.metadataResolver = metadataResolver;
        versionRangeResolver.repositoryEventDispatcher = repositoryEventDispatcher;
        versionRangeResolver.syncContextFactory = syncContextFactory;

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
        wagonRepositoryConnectorFactory.setWagonProvider(new MavenWagonProvider());
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
        Set<Library> set = new TreeSet();

        // dependency collector
        CollectRequest request = new CollectRequest();
        request.setRepositories(project.repositories);

        for (Library library : project.libraries) {
            request.addDependency(new Dependency(library.artifact, library.scope.toString()));
        }

        try {
            DependencyResult result = system.resolveDependencies(newSession(), new DependencyRequest(request, scope.getFilter()));

            for (ArtifactResult dependency : result.getArtifactResults()) {
                set.add(new Library(dependency.getRequest().getArtifact()));
            }

            return set;
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
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new SubArtifact(library.artifact, "*-sources", "jar"));
        request.setRepositories(project.repositories);

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
        List<DependencySelector> filters = new ArrayList(dependencyFilters);
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
        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, true));

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
            ui.talk("Missing artifact descriptor for " + event.getArtifact());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactInstalled(RepositoryEvent event) {
            ui.talk("Installed " + event.getArtifact() + " to " + event.getFile());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactInstalling(RepositoryEvent event) {
            ui.talk("Installing " + event.getArtifact() + " to " + event.getFile());
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
            Exception e = event.getException();

            if (e != null) {
                ui.talk("Artifact is not found : " + event.getArtifact() + " from " + event.getRepository());
            }
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
            ui.talk("Installed " + event.getMetadata() + " to " + event.getFile());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataInstalling(RepositoryEvent event) {
            ui.talk("Installing " + event.getMetadata() + " to " + event.getFile());
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
     * @version 2012/03/25 23:41:54
     */
    private static final class TransferView implements TransferListener {

        /** The actual console. */
        private UserInterface ui;

        private Map<TransferResource, Long> downloads = new ConcurrentHashMap<TransferResource, Long>();

        private int lastLength;

        /**
         * @param ui
         */
        private TransferView(UserInterface ui) {
            this.ui = ui;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferInitiated(TransferEvent event) {
            // String message = event.getRequestType() == TransferEvent.RequestType.PUT ?
            // "Uploading" : "Downloading";

            // ui.talk(message + ": " + event.getResource().getRepositoryUrl() +
            // event.getResource().getResourceName());
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
            TransferResource resource = event.getResource();
            downloads.put(resource, Long.valueOf(event.getTransferredBytes()));

            StringBuilder builder = new StringBuilder();

            for (Map.Entry<TransferResource, Long> entry : downloads.entrySet()) {
                long total = entry.getKey().getContentLength();
                long complete = entry.getValue().longValue();

                builder.append(getStatus(complete, total)).append(" ");
            }

            int pad = lastLength - builder.length();
            lastLength = builder.length();
            pad(builder, pad);
            builder.append('\r');

            ui.talk(builder.toString());
        }

        /**
         * @param complete
         * @param total
         * @return
         */
        private String getStatus(long complete, long total) {
            if (total >= 1024) {
                return toKB(complete) + "/" + toKB(total) + " KB ";
            } else if (total >= 0) {
                return complete + "/" + total + " B ";
            } else if (complete >= 1024) {
                return toKB(complete) + " KB ";
            } else {
                return complete + " B ";
            }
        }

        /**
         * @param buffer
         * @param spaces
         */
        private void pad(StringBuilder buffer, int spaces) {
            String block = " ";
            while (spaces > 0) {
                int n = Math.min(spaces, block.length());
                buffer.append(block, 0, n);
                spaces -= n;
            }
        }

        @Override
        public void transferSucceeded(TransferEvent event) {
            transferCompleted(event);

            TransferResource resource = event.getResource();
            long contentLength = event.getTransferredBytes();
            if (contentLength >= 0) {
                String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
                String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

                String throughput = "";
                long duration = System.currentTimeMillis() - resource.getTransferStartTime();
                if (duration > 0) {
                    DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                    double kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);
                    throughput = " at " + format.format(kbPerSec) + " KB/sec";
                }

                ui.talk(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len + throughput + ")");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferFailed(TransferEvent event) {
            downloads.remove(event.getResource());

            // String message = event.getRequestType() == TransferEvent.RequestType.PUT ?
            // "Uploading" : "Not found";
            //
            // ui.talk(message + ": " + event.getResource().getRepositoryUrl() +
            // event.getResource().getResourceName());
        }

        /**
         * @param event
         */
        private void transferCompleted(TransferEvent event) {
            downloads.remove(event.getResource());

            StringBuilder builder = new StringBuilder(64);
            pad(builder, lastLength);
            builder.append('\r');

            ui.talk(builder.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferCorrupted(TransferEvent event) {
            ui.error(event.getException());
        }

        /**
         * @param bytes
         * @return
         */
        protected long toKB(long bytes) {
            return (bytes + 1023) / 1024;
        }
    }
}
