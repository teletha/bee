/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import javax.inject.Named;

import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultModelCacheFactory;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.UnsolvableVersionConflictException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.impl.LocalRepositoryProvider;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.OfflineController;
import org.eclipse.aether.impl.RemoteRepositoryFilterManager;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryConnectorProvider;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.RepositorySystemLifecycle;
import org.eclipse.aether.impl.UpdateCheckManager;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
import org.eclipse.aether.internal.impl.DefaultDeployer;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.impl.DefaultLocalPathComposer;
import org.eclipse.aether.internal.impl.DefaultLocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.DefaultMetadataResolver;
import org.eclipse.aether.internal.impl.DefaultOfflineController;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryConnectorProvider;
import org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher;
import org.eclipse.aether.internal.impl.DefaultRepositoryLayoutProvider;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.internal.impl.DefaultRepositorySystemLifecycle;
import org.eclipse.aether.internal.impl.DefaultTrackingFileManager;
import org.eclipse.aether.internal.impl.DefaultTransporterProvider;
import org.eclipse.aether.internal.impl.DefaultUpdateCheckManager;
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.LocalPathComposer;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.TrackingFileManager;
import org.eclipse.aether.internal.impl.checksum.DefaultChecksumAlgorithmFactorySelector;
import org.eclipse.aether.internal.impl.checksum.Md5ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha1ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha256ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha512ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.SparseDirectoryTrustedChecksumsSource;
import org.eclipse.aether.internal.impl.checksum.SummaryFileTrustedChecksumsSource;
import org.eclipse.aether.internal.impl.checksum.TrustedToProvidedChecksumsSourceAdapter;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.collect.bf.BfDependencyCollector;
import org.eclipse.aether.internal.impl.filter.DefaultRemoteRepositoryFilterManager;
import org.eclipse.aether.internal.impl.filter.GroupIdRemoteRepositoryFilterSource;
import org.eclipse.aether.internal.impl.filter.PrefixesRemoteRepositoryFilterSource;
import org.eclipse.aether.internal.impl.resolution.TrustedChecksumsArtifactResolverPostProcessor;
import org.eclipse.aether.internal.impl.synccontext.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.synccontext.named.NameMapper;
import org.eclipse.aether.internal.impl.synccontext.named.NameMappers;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactory;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactoryImpl;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.named.providers.FileLockNamedLockFactory;
import org.eclipse.aether.named.providers.LocalReadWriteLockNamedLockFactory;
import org.eclipse.aether.named.providers.LocalSemaphoreNamedLockFactory;
import org.eclipse.aether.named.providers.NoopNamedLockFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.spi.checksums.ProvidedChecksumsSource;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactorySelector;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ConflictContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ConflictItem;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeDeriver;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.VersionSelector;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

import bee.BeeOption;
import bee.Platform;
import bee.UserInterface;
import kiss.ExtensionFactory;
import kiss.I;
import kiss.Lifestyle;
import kiss.Managed;
import kiss.Singleton;
import kiss.Storable;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

@Managed(Singleton.class)
public class Repository {

    /** The path to remote repository. */
    static final List<RemoteRepository> builtinRepositories = new CopyOnWriteArrayList();

    static {
        addRemoteRepository("Maven", "https://repo1.maven.org/maven2/");
        addRemoteRepository("JitPack", "https://jitpack.io/");
    }

    /**
     * Add remote repository.
     * 
     * @param name
     * @param url
     */
    private static final void addRemoteRepository(String name, String url) {
        builtinRepositories.add(new RemoteRepository.Builder(name, "default", url).build());
    }

    /** The current processing project. */
    private final Project project;

    /** The root repository system. */
    private final RepositorySystem system;

    /** The session. */
    private final DefaultRepositorySystemSession session;

    /** The path to local repository. */
    private LocalRepository localRepository;

    /** The user interface. */
    private UserInterface ui = I.make(UserInterface.class);

    /**
     * Wiring components by hand.
     */
    Repository(Project project) {
        this.project = project;

        // ============ RepositorySystem ============ //
        system = I.make(RepositorySystem.class);

        // ==================================================
        // Initialize
        // ==================================================
        setLocalRepository(BeeOption.Cacheless.value() ? Locator.temporaryDirectory() : Platform.BeeLocalRepository);

        // ============ RepositorySystemSession ============ //
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setDependencySelector(new AndDependencySelector(new OptionalDependencySelector(), new ScopeDependencySelector(Scope.Test.id, Scope.Provided.id, Scope.Annotation.id), new ExclusionDependencySelector(project.exclusions)));
        session.setDependencyGraphTransformer(new ChainedDependencyGraphTransformer(new ConflictResolver(new ConflictVersionSelector(true), new JavaScopeSelector(), new SimpleOptionalitySelector(), new BeeScopeDeriver()), new JavaDependencyContextRefiner()));
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
        session.setUpdatePolicy(BeeOption.Cacheless.value() ? RepositoryPolicy.UPDATE_POLICY_ALWAYS : "interval:7200");
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        session.setIgnoreArtifactDescriptorRepositories(true);
        session.setCache(new DefaultRepositoryCache());
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(ResolutionErrorPolicy.CACHE_ALL, ResolutionErrorPolicy.CACHE_ALL));
        session.setOffline(BeeOption.Offline.value());
        session.setSystemProperties(System.getProperties());
        session.setConfigProperties(System.getProperties());
        session.setConfigProperty("maven.artifact.threads", 10);
        session.setConfigProperty("maven.metadata.threads", 10);
        session.setConfigProperty("maven.metadataResolver.threads", 10);
        session.setConfigProperty("aether.dependencyCollector.impl", "fast");
        session.setConfigProperty("aether.dependencyCollector.bf.threads", 10);
        session.setConfigProperty("aether.artifactResolver.threads", 10);
        session.setConfigProperty("aether.metadataResolver.threads", 10);

        // event listener
        Loader transfers = I.make(Loader.class);
        session.setTransferListener(transfers);
        session.setRepositoryListener(transfers);

        this.session = session;
    }

    /**
     * Build the dependency graph for compile scope.
     * 
     * @param project
     * @return
     */
    public DependencyNode buildDependencyGraph(Project project) {
        try {
            CollectRequest request = new CollectRequest(null, remoteRepositories());
            for (Library library : project.libraries) {
                if (library.scope.accept(Scope.Compile.id)) {
                    request.addDependency(new Dependency(library.artifact, library.scope.id));
                }
            }
            request.setRootArtifact(project.asLibrary().artifact);

            return system.resolveDependencies(session, new DependencyRequest(request, null)).getRoot();
        } catch (DependencyResolutionException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Collect all dependencies in the specified scope.
     * 
     * @param project
     * @param scopes
     * @return
     */
    public Set<Library> collectDependency(Project project, Scope... scopes) {
        return collectDependency(project, Set.of(scopes), project.libraries);
    }

    /**
     * Collect all dependencies in the specified scope.
     * 
     * @param library
     * @param scopes
     * @return
     */
    public Set<Library> collectDependency(Library library, Scope... scopes) {
        return collectDependency(project, Set.of(scopes), Collections.singleton(library));
    }

    /**
     * Collect all dependencies in the specified scope.
     *
     * @param libraries
     * @param scopes
     * @return
     */
    private Set<Library> collectDependency(Project project, Set<Scope> scopes, Set<Library> libraries) {
        Set<Library> set = new TreeSet();

        for (Scope scope : scopes) {
            // collect dependency
            CollectRequest request = new CollectRequest(null, remoteRepositories());
            for (Library library : libraries) {
                if (scope.accept(library.scope.id)) {
                    // spcify the latest version
                    Artifact artifact = library.artifact;
                    if (artifact.getVersion().equals("LATEST")) {
                        artifact = artifact.setVersion("[" + resolveLatestVersion(library) + ",)");
                    }
                    request.addDependency(new Dependency(artifact, library.scope.id));
                }
            }

            try {
                DependencyResult result = system.resolveDependencies(session, new DependencyRequest(request, (node, parents) -> {
                    List<DependencyNode> list = I.signal(parents).startWith(node).skip(p -> p.getArtifact() == null).toList();
                    return list.isEmpty() || list.stream().allMatch(n -> {
                        return scope.accept(n.getDependency().getScope());
                    });
                }));

                for (ArtifactResult dependency : result.getArtifactResults()) {
                    Library lib = new Library(dependency.getArtifact());
                    Library old = checkDupilication(set, lib);
                    if (old != null) {
                        if (lib.version.compareToIgnoreCase(old.version) > 0) {
                            set.remove(old);
                            set.add(lib);
                        }
                    } else {
                        set.add(lib);
                    }
                }
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
        return set;
    }

    private Library checkDupilication(Set<Library> set, Library target) {
        for (Library lib : set) {
            if (lib.isSame(target)) {
                return lib;
            }
        }
        return null;
    }

    /**
     * Resolve the latest version of the specified library.
     * 
     * @param library
     * @return
     */
    public String resolveLatestVersion(Library library) {
        try {
            return system.resolveArtifact(session, new ArtifactRequest(library.artifact, remoteRepositories(), null))
                    .getArtifact()
                    .getVersion();
        } catch (Exception e) {
            return library.version;
        }
    }

    /**
     * Resolve the jar of the specified library.
     * 
     * @param library
     * @return
     */
    public File resolveJar(Library library) {
        File resolved = resolveSubArtifact(library, "");
        return resolved != null && resolved.isPresent() ? resolved : getLocalRepository().file(library.getJar());
    }

    /**
     * Resolve the javadoc of the specified library.
     * 
     * @param library
     * @return
     */
    public File resolveJavadoc(Library library) {
        File resolved = resolveSubArtifact(library, "javadoc");

        if (resolved != null && resolved.isPresent()) {
            return resolved;
        }

        resolved = getLocalRepository().file(library.getJavadocJar());

        if (resolved.isPresent()) {
            return resolved;
        }

        if (!library.classfier.isEmpty()) {
            return resolveSource(new Library(library.group, library.name, library.version));
        }
        return resolved;
    }

    /**
     * Resolve the source codes of the specified library.
     * 
     * @param library
     * @return
     */
    public File resolveSource(Library library) {
        File resolved = resolveSubArtifact(library, "sources");

        if (resolved != null && resolved.isPresent()) {
            return resolved;
        }

        resolved = getLocalRepository().file(library.getSourceJar());

        if (resolved.isPresent()) {
            return resolved;
        }

        if (!library.classfier.isEmpty()) {
            return resolveSource(new Library(library.group, library.name, library.version));
        }
        return resolved;
    }

    /**
     * Resolve the source codes of the specified library.
     * 
     * @param library
     * @return
     */
    private File resolveSubArtifact(Library library, String suffix) {
        // If some classified artifact is missing, there is high possibility that another classified
        // artifact with the same group, name and version is also missing.

        LibraryInfo info = LibraryInfo.of(library);

        if (info.shouldAccessToSource()) {
            SubArtifact sub = new SubArtifact(library.artifact, "*-" + suffix, "jar");
            ArtifactRequest request = new ArtifactRequest(sub, remoteRepositories(), null);

            try {
                ArtifactResult result = system.resolveArtifact(session, request);

                if (result.isResolved()) {
                    return Locator.file(result.getArtifact().getFile().getPath());
                } else {
                    ui.info("Artifact [", sub, "] is not resolved.");
                }
            } catch (ArtifactResolutionException e) {
                ui.info("Artifact [", sub, "] is not found.");
                info.lastAccessToSource = LocalDate.now();
                info.store();
            }
        }
        return null;
    }

    /**
     * Retrieve all repositories.
     * 
     * @return
     */
    private List<RemoteRepository> remoteRepositories() {
        List<RemoteRepository> repositories = new ArrayList();
        repositories.addAll(builtinRepositories);
        repositories.addAll(project.repositories);
        return repositories;
    }

    /**
     * Install project into the local repository.
     * 
     * @param project A project to install.
     */
    public void install(Project project) {
        install(project, project.locateJar());
    }

    /**
     * Install project into the local repository.
     * 
     * @param project A project to install.
     */
    public void install(Project project, File classes) {
        install(project, classes, project.locateSourceJar(), project.locateJavadocJar());
    }

    /**
     * Install project into the local repository.
     * 
     * @param project A project to install.
     */
    public void install(Project project, File classes, File sources, File javadoc) {
        String group = project.getGroup();
        String product = project.getProduct();
        String version = project.getVersion();

        // create artifact for project
        Artifact jar = new DefaultArtifact(group, product, "", "jar", version, null, classes.asJavaFile());

        try {
            InstallRequest request = new InstallRequest();
            request.addArtifact(jar);
            request.addArtifact(new SubArtifact(jar, "", "pom", Locator.temporaryFile().text(project.toMavenDefinition()).asJavaFile()));
            if (sources != null && sources.isPresent()) {
                request.addArtifact(new SubArtifact(jar, "sources", "jar", sources.asJavaFile()));
            }
            if (javadoc != null && javadoc.isPresent()) {
                request.addArtifact(new SubArtifact(jar, "javadoc", "jar", javadoc.asJavaFile()));
            }
            system.install(session, request);
        } catch (InstallationException e) {
            e.printStackTrace();
            throw I.quiet(e);
        }
    }

    /**
     * Get the local repository path.
     * 
     * @return
     */
    public final Directory getLocalRepository() {
        return Locator.directory(localRepository.getBasedir().getPath());
    }

    /**
     * Set the local property of this {@link Repository}.
     * 
     * @param path The local value to set.
     */
    public final void setLocalRepository(Directory path) {
        this.localRepository = new LocalRepository(path.absolutize().toString());

        if (session != null) {
            session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
        }
    }

    /**
     * 
     */
    private static class BeeScopeDeriver extends ScopeDeriver {

        /**
         * {@inheritDoc}
         */
        @Override
        public void deriveScope(ScopeContext context) throws RepositoryException {
            String derived;
            String parent = context.getParentScope();
            String child = context.getChildScope();

            if (Scope.System.id.equals(child) || Scope.Test.id.equals(child)) {
                derived = child;
            } else if (parent == null || parent.isEmpty() || Scope.Compile.id.equals(parent)) {
                derived = child;
            } else if (Scope.Test.id.equals(parent) || Scope.Runtime.id.equals(parent) || Scope.Annotation.id.equals(parent)) {
                derived = parent;
            } else if (Scope.System.id.equals(parent) || Scope.Provided.id.equals(parent)) {
                derived = Scope.Provided.id;
            } else {
                derived = Scope.Runtime.id;
            }

            context.setDerivedScope(derived);
        }
    }

    /**
     * 
     */
    private static class LibraryInfo implements Storable<LibraryInfo> {

        /** The last access time to remote source. */
        public LocalDate lastAccessToSource = LocalDate.MIN;

        private final Library library;

        /**
         * @param library
         */
        private LibraryInfo(Library library) {
            this.library = Objects.requireNonNull(library);
        }

        /**
         * Confirm whether we should check the source resource or not.
         * 
         * @return
         */
        private boolean shouldAccessToSource() {
            return BeeOption.Cacheless.value() ? true : lastAccessToSource.plusDays(14).isBefore(LocalDate.now());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Path locate() {
            File pom = library.getLocalPOM();
            return pom.parent().file(pom.name().replace(".pom", ".bee")).asJavaPath();
        }

        /**
         * Build {@link Library} infomation.
         * 
         * @param library
         * @return
         */
        private static LibraryInfo of(Library library) {
            return new LibraryInfo(library).restore();
        }
    }

    /**
     * Define various {@link Lifestyle}s.
     */
    @Managed(Singleton.class)
    private static class Lifestyles implements ExtensionFactory<Lifestyle> {

        private final Map<Class, Lifestyle> lifestyles = new ConcurrentHashMap();

        private Lifestyles() {
            define(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
            define(ArtifactResolver.class, DefaultArtifactResolver.class, TrustedChecksumsArtifactResolverPostProcessor.class, GroupIdRemoteRepositoryFilterSource.class);
            define(ChecksumAlgorithmFactorySelector.class, DefaultChecksumAlgorithmFactorySelector.class, Md5ChecksumAlgorithmFactory.class, Sha1ChecksumAlgorithmFactory.class, Sha256ChecksumAlgorithmFactory.class, Sha512ChecksumAlgorithmFactory.class);
            define(ChecksumPolicyProvider.class, DefaultChecksumPolicyProvider.class);
            define(Deployer.class, DefaultDeployer.class, SnapshotMetadataGeneratorFactory.class, VersionsMetadataGeneratorFactory.class);
            define(DependencyCollector.class, DefaultDependencyCollector.class, BfDependencyCollector.class, FastCollector.class);
            define(FileProcessor.class, DefaultFileProcessor.class);
            define(Installer.class, DefaultInstaller.class, SnapshotMetadataGeneratorFactory.class, VersionsMetadataGeneratorFactory.class);
            define(LocalPathComposer.class, DefaultLocalPathComposer.class);
            define(LocalPathPrefixComposerFactory.class, DefaultLocalPathPrefixComposerFactory.class);
            define(LocalRepositoryProvider.class, DefaultLocalRepositoryProvider.class, SimpleLocalRepositoryManagerFactory.class, EnhancedLocalRepositoryManagerFactory.class);
            define(MetadataResolver.class, DefaultMetadataResolver.class);
            define(ModelBuilder.class, new DefaultModelBuilderFactory()::newInstance);
            define(ModelCacheFactory.class, DefaultModelCacheFactory.class);
            define(NamedLockFactoryAdapterFactory.class, BeeNamedLockFactoryAdapterFactory.class);
            define(OfflineController.class, DefaultOfflineController.class);
            define(ProvidedChecksumsSource.class, TrustedToProvidedChecksumsSourceAdapter.class, SparseDirectoryTrustedChecksumsSource.class, SummaryFileTrustedChecksumsSource.class);
            define(RemoteRepositoryFilterManager.class, DefaultRemoteRepositoryFilterManager.class, GroupIdRemoteRepositoryFilterSource.class, PrefixesRemoteRepositoryFilterSource.class);
            define(RemoteRepositoryManager.class, DefaultRemoteRepositoryManager.class);
            define(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class, ProvidedChecksumsSource.class);
            define(RepositoryConnectorProvider.class, DefaultRepositoryConnectorProvider.class, RepositoryConnectorFactory.class);
            define(RepositoryEventDispatcher.class, DefaultRepositoryEventDispatcher.class);
            define(RepositoryLayoutFactory.class, Maven2RepositoryLayoutFactory.class);
            define(RepositoryLayoutProvider.class, DefaultRepositoryLayoutProvider.class, RepositoryLayoutFactory.class);
            define(RepositorySystem.class, DefaultRepositorySystem.class);
            define(RepositorySystemLifecycle.class, DefaultRepositorySystemLifecycle.class);
            define(SyncContextFactory.class, DefaultSyncContextFactory.class);
            define(TrackingFileManager.class, DefaultTrackingFileManager.class);
            define(TransporterFactory.class, FastTransporter.class);
            define(TransporterProvider.class, DefaultTransporterProvider.class, TransporterFactory.class);
            define(UpdateCheckManager.class, DefaultUpdateCheckManager.class);
            define(UpdatePolicyAnalyzer.class, DefaultUpdatePolicyAnalyzer.class);
            define(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
            define(VersionResolver.class, DefaultVersionResolver.class);
            define(VersionScheme.class, GenericVersionScheme.class);
        }

        private <T> void define(Class<T> type, Class<? extends T> clazz, Class... names) {
            lifestyles.put(type, new LazySingleton(clazz, names));
        }

        private <T> void define(Class<T> type, Lifestyle<T> lifestyle) {
            lifestyles.put(type, lifestyle);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Lifestyle create(Class key) {
            return lifestyles.get(key);
        }
    }

    /**
     * Lazy intializable singleton.
     */
    private static class LazySingleton<M, N> implements Lifestyle<M> {

        private final Lifestyle<M> lifestyle;

        private M instance;

        private LazySingleton(Class<M> type, Class<N>... subs) {
            this.lifestyle = I.prototype(type, paramType -> {
                if (paramType == Map.class) {
                    Map map = new HashMap();

                    for (Class<N> sub : subs) {
                        Named named = sub.getAnnotation(Named.class);
                        if (named != null) {
                            map.put(named.value(), I.make(sub));
                        } else {
                            N impl = I.make(sub);
                            named = impl.getClass().getAnnotation(Named.class);
                            if (named != null) {
                                map.put(named.value(), impl);
                            }
                        }
                    }
                    return map;
                } else {
                    return I.make(paramType);
                }
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized M call() throws Exception {
            if (instance == null) {
                instance = lifestyle.call();
            }
            return instance;
        }
    }

    /**
     * Select nearest or highest strategy.
     */
    private static class ConflictVersionSelector extends VersionSelector {

        private final BiPredicate<ConflictItem, ConflictItem> selectionStrategy;

        private ConflictVersionSelector(boolean highest) {
            this.selectionStrategy = highest ? (candidate, winner) -> {
                return candidate.getNode().getVersion().compareTo(winner.getNode().getVersion()) > 0;
            } : (candidate, winner) -> {
                if (candidate.isSibling(winner)) {
                    return candidate.getNode().getVersion().compareTo(winner.getNode().getVersion()) > 0;
                } else {
                    return candidate.getDepth() < winner.getDepth();
                }
            };
        }

        @Override
        public void selectVersion(ConflictContext context) throws RepositoryException {
            ConflictGroup group = new ConflictGroup();
            for (ConflictItem candidate : context.getItems()) {
                DependencyNode node = candidate.getNode();
                VersionConstraint constraint = node.getVersionConstraint();

                boolean backtrack = false;
                boolean hardConstraint = constraint.getRange() != null;

                if (hardConstraint) {
                    if (group.constraints.add(constraint)) {
                        if (group.winner != null && !constraint.containsVersion(group.winner.getNode().getVersion())) {
                            backtrack = true;
                        }
                    }
                }

                if (isAcceptableByConstraints(group, node.getVersion())) {
                    group.candidates.add(candidate);

                    if (backtrack) {
                        backtrack(group, context);
                    } else if (group.winner == null || selectionStrategy.test(candidate, group.winner)) {
                        group.winner = candidate;
                    }
                } else if (backtrack) {
                    backtrack(group, context);
                }
            }
            context.setWinner(group.winner);
        }

        private void backtrack(ConflictGroup group, ConflictContext context) throws UnsolvableVersionConflictException {
            group.winner = null;

            for (Iterator<ConflictItem> it = group.candidates.iterator(); it.hasNext();) {
                ConflictItem candidate = it.next();

                if (!isAcceptableByConstraints(group, candidate.getNode().getVersion())) {
                    it.remove();
                } else if (group.winner == null || selectionStrategy.test(candidate, group.winner)) {
                    group.winner = candidate;
                }
            }

            if (group.winner == null) {
                PathRecordingDependencyVisitor visitor = new PathRecordingDependencyVisitor((node, parents) -> context.isIncluded(node));
                context.getRoot().accept(new TreeDependencyVisitor(visitor));
                throw new UnsolvableVersionConflictException(visitor.getPaths());
            }
        }

        private boolean isAcceptableByConstraints(ConflictGroup group, Version version) {
            for (VersionConstraint constraint : group.constraints) {
                if (!constraint.containsVersion(version)) {
                    return false;
                }
            }
            return true;
        }

        private static class ConflictGroup {

            private final Set<VersionConstraint> constraints = new HashSet();

            private final List<ConflictItem> candidates = new ArrayList();

            private ConflictItem winner;
        }
    }

    private static class BeeNamedLockFactoryAdapterFactory extends NamedLockFactoryAdapterFactoryImpl {

        public BeeNamedLockFactoryAdapterFactory(RepositorySystemLifecycle lifecycle) {
            super(getManuallyCreatedFactories(), getManuallyCreatedNameMappers(), lifecycle);
        }

        private static Map<String, NamedLockFactory> getManuallyCreatedFactories() {
            HashMap<String, NamedLockFactory> factories = new HashMap<>();
            factories.put(NoopNamedLockFactory.NAME, new NoopNamedLockFactory());
            factories.put(LocalReadWriteLockNamedLockFactory.NAME, new LocalReadWriteLockNamedLockFactory());
            factories.put(LocalSemaphoreNamedLockFactory.NAME, new LocalSemaphoreNamedLockFactory());
            factories.put(FileLockNamedLockFactory.NAME, new FileLockNamedLockFactory());
            return Collections.unmodifiableMap(factories);
        }

        private static Map<String, NameMapper> getManuallyCreatedNameMappers() {
            HashMap<String, NameMapper> mappers = new HashMap<>();
            mappers.put(NameMappers.STATIC_NAME, NameMappers.staticNameMapper());
            mappers.put(NameMappers.GAV_NAME, NameMappers.gavNameMapper());
            mappers.put(NameMappers.DISCRIMINATING_NAME, NameMappers.discriminatingNameMapper());
            mappers.put(NameMappers.FILE_GAV_NAME, NameMappers.fileGavNameMapper());
            mappers.put(NameMappers.FILE_HGAV_NAME, NameMappers.fileHashingGavNameMapper());
            return Collections.unmodifiableMap(mappers);
        }
    }

}