/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpRetryException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
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
import org.eclipse.aether.internal.impl.DefaultChecksumProcessor;
import org.eclipse.aether.internal.impl.DefaultDeployer;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.impl.DefaultLocalPathComposer;
import org.eclipse.aether.internal.impl.DefaultLocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.DefaultMetadataResolver;
import org.eclipse.aether.internal.impl.DefaultOfflineController;
import org.eclipse.aether.internal.impl.DefaultPathProcessor;
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
import org.eclipse.aether.internal.impl.collect.FastDependencyCollector;
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
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.connector.transport.TransportListener;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.spi.io.ChecksumProcessor;
import org.eclipse.aether.spi.io.PathProcessor;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.aether.transfer.ChecksumFailureException;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;

import bee.BeeLoader;
import bee.BeeOption;
import bee.Platform;
import bee.UserInterface;
import kiss.ExtensionFactory;
import kiss.I;
import kiss.Lifestyle;
import kiss.Managed;
import kiss.Singleton;
import kiss.Storable;
import kiss.WiseConsumer;
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
        setLocalRepository(Platform.BeeLocalRepository);

        // ============ RepositorySystemSession ============ //
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setDependencySelector(new AndDependencySelector(new OptionalDependencySelector(), new ScopeDependencySelector(Scope.Test.id, Scope.Provided.id, Scope.Annotation.id), new ExclusionDependencySelector(project.exclusions)));
        session.setDependencyGraphTransformer(new ChainedDependencyGraphTransformer(new ConflictResolver(new NearestVersionSelector(), new JavaScopeSelector(), new SimpleOptionalitySelector(), new BeeScopeDeriver()), new JavaDependencyContextRefiner()));
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
        session.setUpdatePolicy(BeeOption.Cacheless.value() ? RepositoryPolicy.UPDATE_POLICY_ALWAYS : RepositoryPolicy.UPDATE_POLICY_DAILY);
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        session.setIgnoreArtifactDescriptorRepositories(true);
        session.setCache(new DefaultRepositoryCache());
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(ResolutionErrorPolicy.CACHE_ALL, ResolutionErrorPolicy.CACHE_ALL));
        session.setOffline(BeeOption.Offline.value());
        session.setSystemProperties(System.getProperties());
        session.setConfigProperties(System.getProperties());
        session.setConfigProperty("maven.artifact.threads", 24);

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
                    return Locator.file(result.getArtifact().getFile().toPath());
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
        return Locator.directory(localRepository.getBasedir().toPath());
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
     * Load the latest library and import it dynamically.
     * 
     * @param groupProductVersion A colon separated values. (group:product:version)
     */
    public static void require(String groupProductVersion) {
        String[] values = groupProductVersion.split(":");
        switch (values.length) {
        case 2:
            require(values[0], values[1]);
            break;

        case 3:
            require(values[0], values[1], values[2]);
            break;

        default:
            throw new IllegalArgumentException(groupProductVersion + " is invalid format.");
        }
    }

    /**
     * Load the latest library and import it dynamically.
     * 
     * @param group A group name.
     * @param product A product name.
     */
    public static void require(String group, String product) {
        require(group, product, I.make(Repository.class).resolveLatestVersion(new Library(group, product, "LATEST")));
    }

    /**
     * Load library and import it dynamically.
     * 
     * @param group A group name.
     * @param product A product name.
     * @param version A product version.
     */
    public static void require(String group, String product, String version) {
        Library require = new Library(group, product, version);
        BeeLoader.load(require.getLocalJar());

        for (Library library : I.make(Repository.class).collectDependency(require, Scope.Runtime)) {
            BeeLoader.load(library.getLocalJar());
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
        public String locate() {
            File pom = library.getLocalPOM();
            return pom.parent().file(pom.name().replace(".pom", ".bee")).path();
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
            define(RepositorySystem.class, DefaultRepositorySystem.class);
            define(ArtifactResolver.class, DefaultArtifactResolver.class, TrustedChecksumsArtifactResolverPostProcessor.class, GroupIdRemoteRepositoryFilterSource.class);
            define(DependencyCollector.class, FastDependencyCollector.class);
            define(MetadataResolver.class, DefaultMetadataResolver.class);
            define(Deployer.class, DefaultDeployer.class, SnapshotMetadataGeneratorFactory.class, VersionsMetadataGeneratorFactory.class);
            define(Installer.class, DefaultInstaller.class, SnapshotMetadataGeneratorFactory.class, VersionsMetadataGeneratorFactory.class);
            define(RepositoryLayoutProvider.class, DefaultRepositoryLayoutProvider.class, RepositoryLayoutFactory.class);
            define(RepositoryLayoutFactory.class, Maven2RepositoryLayoutFactory.class);

            define(ChecksumAlgorithmFactorySelector.class, DefaultChecksumAlgorithmFactorySelector.class, Md5ChecksumAlgorithmFactory.class, Sha1ChecksumAlgorithmFactory.class, Sha256ChecksumAlgorithmFactory.class, Sha512ChecksumAlgorithmFactory.class);
            define(ChecksumPolicyProvider.class, DefaultChecksumPolicyProvider.class);
            define(RepositoryConnectorProvider.class, DefaultRepositoryConnectorProvider.class, RepositoryConnectorFactory.class);
            define(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class, ProvidedChecksumsSource.class);
            define(ProvidedChecksumsSource.class, TrustedToProvidedChecksumsSourceAdapter.class, SparseDirectoryTrustedChecksumsSource.class, SummaryFileTrustedChecksumsSource.class);
            define(RemoteRepositoryManager.class, DefaultRemoteRepositoryManager.class);
            define(UpdateCheckManager.class, DefaultUpdateCheckManager.class);
            define(UpdatePolicyAnalyzer.class, DefaultUpdatePolicyAnalyzer.class);
            define(PathProcessor.class, DefaultPathProcessor.class);
            define(ChecksumProcessor.class, DefaultChecksumProcessor.class);
            define(SyncContextFactory.class, DefaultSyncContextFactory.class);
            define(RepositoryEventDispatcher.class, DefaultRepositoryEventDispatcher.class);
            define(OfflineController.class, DefaultOfflineController.class);
            define(LocalPathComposer.class, DefaultLocalPathComposer.class);
            define(LocalPathPrefixComposerFactory.class, DefaultLocalPathPrefixComposerFactory.class);
            define(LocalRepositoryProvider.class, DefaultLocalRepositoryProvider.class, SimpleLocalRepositoryManagerFactory.class, EnhancedLocalRepositoryManagerFactory.class);
            define(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
            define(TrackingFileManager.class, DefaultTrackingFileManager.class);
            define(VersionResolver.class, DefaultVersionResolver.class);
            define(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
            define(RemoteRepositoryFilterManager.class, DefaultRemoteRepositoryFilterManager.class, GroupIdRemoteRepositoryFilterSource.class, PrefixesRemoteRepositoryFilterSource.class);
            define(RepositorySystemLifecycle.class, DefaultRepositorySystemLifecycle.class);
            define(NamedLockFactoryAdapterFactory.class, BeeNamedLockFactoryAdapterFactory.class);
            define(VersionScheme.class, GenericVersionScheme.class);
            define(ModelCacheFactory.class, DefaultModelCacheFactory.class);
            define(TransporterProvider.class, DefaultTransporterProvider.class, TransporterFactory.class);
            define(TransporterFactory.class, NetTransporterFactory.class);
            define(ModelBuilder.class, new DefaultModelBuilderFactory()::newInstance);
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

    /**
     * For HTTP and HTTPS.
     */
    @Named("https")
    private static class NetTransporterFactory implements TransporterFactory {

        /**
         * {@inheritDoc}
         */
        @Override
        public float getPriority() {
            return 10;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Transporter newInstance(RepositorySystemSession session, RemoteRepository repository) throws NoTransporterException {
            return new Transporter() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void put(PutTask task) throws Exception {
                    throw new Error("HTTP PUT is not implemented, please FIX.");
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void peek(PeekTask task) throws Exception {
                    URI uri = URI.create(repository.getUrl() + task.getLocation());
                    Builder request = HttpRequest.newBuilder(uri).method("HEAD", BodyPublishers.noBody());
                    I.http(request, HttpResponse.class).waitForTerminate().to((WiseConsumer<HttpResponse>) res -> {
                        int code = res.statusCode();
                        if (400 <= code) {
                            throw new HttpRetryException("Fail to peek resource [" + uri + "]", code);
                        }
                    });
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void get(GetTask task) throws Exception {
                    String uri = repository.getUrl() + task.getLocation();

                    I.http(uri, HttpResponse.class).waitForTerminate().to((WiseConsumer<HttpResponse>) res -> {
                        // analyze header
                        HttpHeaders headers = res.headers();
                        OptionalLong length = headers.firstValueAsLong("Content-Length");

                        // transfer data
                        try (InputStream in = (InputStream) res.body(); OutputStream out = new FileOutputStream(task.getDataFile())) {
                            TransportListener listener = task.getListener();
                            listener.transportStarted(0, length.orElse(0));

                            int read = -1;
                            byte[] buffer = new byte[1024 * 32];
                            while (0 < (read = in.read(buffer))) {
                                out.write(buffer, 0, read);
                                listener.transportProgressed(ByteBuffer.wrap(buffer, 0, read));
                            }
                        }

                        // detect checksum
                        readChecksum(headers, task);
                    });
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void close() {
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int classify(Throwable error) {
                    return error instanceof HttpRetryException http && http.responseCode() == 404 ? ERROR_NOT_FOUND : ERROR_OTHER;
                }

                private void readChecksum(HttpHeaders headers, GetTask task) throws ChecksumFailureException {
                    String checksum = headers.firstValue("ETAG") //
                            .flatMap(etag -> {
                                int start = etag.indexOf("SHA1{") + 5;
                                int end = etag.indexOf("}", start);
                                return start == -1 || end == -1 ? Optional.empty() : Optional.of(etag.substring(start, end));
                            })
                            .or(() -> headers.firstValue("x-checksum-sha1"))
                            .or(() -> headers.firstValue("x-checksum-md5"))
                            .or(() -> headers.firstValue("x-goog-meta-checksum-sha1"))
                            .or(() -> headers.firstValue("x-goog-meta-checksum-md5"))
                            .orElse("");

                    switch (checksum.length()) {
                    case 32:
                        task.setChecksum("MD5", checksum);
                        break;

                    case 40:
                        task.setChecksum("SHA-1", checksum);
                        break;
                    }
                }
            };
        }
    }
}