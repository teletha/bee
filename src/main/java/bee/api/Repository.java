/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
import org.eclipse.aether.internal.impl.DefaultDeployer;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.DefaultMetadataResolver;
import org.eclipse.aether.internal.impl.DefaultOfflineController;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryConnectorProvider;
import org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher;
import org.eclipse.aether.internal.impl.DefaultRepositoryLayoutProvider;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.internal.impl.DefaultTrackingFileManager;
import org.eclipse.aether.internal.impl.DefaultTransporterProvider;
import org.eclipse.aether.internal.impl.DefaultUpdateCheckManager;
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.collect.FastDependencyCollector;
import org.eclipse.aether.internal.impl.synccontext.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactorySelector;
import org.eclipse.aether.internal.impl.synccontext.named.SimpleNamedLockFactorySelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
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

import bee.BeeLoader;
import bee.BeeOption;
import bee.Platform;
import bee.UserInterface;
import bee.util.Inputs;
import kiss.ExtensionFactory;
import kiss.I;
import kiss.Lifestyle;
import kiss.Managed;
import kiss.Singleton;
import kiss.Storable;
import kiss.model.Model;
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
        session.setConfigProperty("maven.artifact.threads", 24);
        session.setOffline(BeeOption.Offline.value());

        // event listener
        View view = I.make(View.class);
        session.setTransferListener(view);
        session.setRepositoryListener(view);

        this.session = session;
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
                    request.addDependency(new Dependency(library.artifact, library.scope.id));
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
                    set.add(new Library(dependency.getArtifact()));
                }
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
        return set;
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
            request.addArtifact(new SubArtifact(jar, "", "pom", Locator.temporaryFile().text(project.toString()).asJavaFile()));
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
    private static class View extends AbstractRepositoryListener implements TransferListener {

        /** The user notifier. */
        private final UserInterface ui;

        /**
         * @param ui
         */
        private View(UserInterface ui) {
            this.ui = ui;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactInstalled(RepositoryEvent event) {
            ui.info("Install " + event.getArtifact() + " to " + event.getFile());
        }

        /** The progress event interval. (ms) */
        private static final long interval = 500 * 1000 * 1000;

        /** The last progress event time. */
        private long last = 0;

        /** The downloading items. */
        private Map<TransferResource, TransferEvent> downloading = new ConcurrentHashMap();

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferInitiated(TransferEvent event) {
            downloading.put(event.getResource(), event);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferStarted(TransferEvent event) throws TransferCancelledException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferProgressed(TransferEvent event) {
            downloading.put(event.getResource(), event);

            long now = System.nanoTime();
            if (interval < now - last) {
                last = now; // update last event time

                // build message
                StringBuilder message = new StringBuilder();

                for (Map.Entry<TransferResource, TransferEvent> entry : downloading.entrySet()) {
                    TransferResource resource = entry.getKey();
                    long current = entry.getValue().getTransferredBytes();
                    long size = resource.getContentLength();

                    message.append(name(resource)).append(" (");
                    if (0 < size) {
                        message.append(Inputs.formatAsSize(current, false)).append('/').append(Inputs.formatAsSize(size));
                    } else {
                        message.append(Inputs.formatAsSize(current));
                    }
                    message.append(")   ");
                }

                // notify
                ui.trace(message);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transferSucceeded(TransferEvent event) {
            TransferResource resource = event.getResource();

            // unregister item
            downloading.remove(resource);

            long contentLength = event.getTransferredBytes();
            if (contentLength >= 0) {
                String length = Inputs.formatAsSize(contentLength);

                if (event.getRequestType() == TransferEvent.RequestType.GET) {
                    ui.info("Downloaded : " + name(resource) + " (" + length + ") from [" + resource.getRepositoryUrl() + "]");
                } else {
                    ui.info("Uploaded : " + name(resource) + " (" + length + ") to [" + resource.getRepositoryUrl() + "]");
                }
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
         * Compute readable resource name.
         * 
         * @param resource
         * @return
         */
        private static String name(TransferResource resource) {
            return resource.getResourceName().substring(resource.getResourceName().lastIndexOf('/') + 1);
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
            return lastAccessToSource.plusDays(7).isBefore(LocalDate.now());
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
            define(DefaultRepositorySystem.class);
            define(DefaultArtifactResolver.class);
            define(FastDependencyCollector.class);
            define(DefaultMetadataResolver.class);
            define(DefaultDeployer.class, impl -> {
                impl.addMetadataGeneratorFactory(I.make(SnapshotMetadataGeneratorFactory.class));
                impl.addMetadataGeneratorFactory(I.make(VersionsMetadataGeneratorFactory.class));
            });
            define(DefaultInstaller.class, impl -> {
                impl.addMetadataGeneratorFactory(I.make(SnapshotMetadataGeneratorFactory.class));
                impl.addMetadataGeneratorFactory(I.make(VersionsMetadataGeneratorFactory.class));
            });
            define(DefaultRepositoryLayoutProvider.class, impl -> {
                impl.addRepositoryLayoutFactory(I.make(RepositoryLayoutFactory.class));
            });
            define(Maven2RepositoryLayoutFactory.class);
            define(DefaultTransporterProvider.class, impl -> {
                impl.addTransporterFactory(I.make(TransporterFactory.class));
            });
            define(DefaultChecksumPolicyProvider.class);
            define(DefaultRepositoryConnectorProvider.class, impl -> {
                impl.addRepositoryConnectorFactory(I.make(BasicRepositoryConnectorFactory.class));
            });
            define(DefaultRemoteRepositoryManager.class);
            define(DefaultUpdateCheckManager.class);
            define(DefaultUpdatePolicyAnalyzer.class);
            define(DefaultFileProcessor.class);
            define(org.eclipse.aether.internal.impl.synccontext.legacy.DefaultSyncContextFactory.class);
            define(DefaultSyncContextFactory.class);
            define(DefaultRepositoryEventDispatcher.class);
            define(DefaultOfflineController.class);
            define(DefaultLocalRepositoryProvider.class, impl -> {
                impl.addLocalRepositoryManagerFactory(I.make(SimpleLocalRepositoryManagerFactory.class));
                impl.addLocalRepositoryManagerFactory(I.make(EnhancedLocalRepositoryManagerFactory.class));
            });
            define(DefaultArtifactDescriptorReader.class);
            define(DefaultTrackingFileManager.class);
            define(DefaultVersionResolver.class);
            define(DefaultVersionRangeResolver.class);
            defineSelf(SnapshotMetadataGeneratorFactory.class);
            defineSelf(VersionsMetadataGeneratorFactory.class);
            defineSelf(SimpleLocalRepositoryManagerFactory.class);
            defineSelf(EnhancedLocalRepositoryManagerFactory.class);
            define(BasicRepositoryConnectorFactory.class);
            define(HttpTransporterFactory.class);
            define(NamedLockFactorySelector.class, SimpleNamedLockFactorySelector::new);
            define(ModelBuilder.class, new DefaultModelBuilderFactory()::newInstance);
        }

        private <T> void define(Class<T> clazz, Consumer<T>... initializer) {
            for (Class type : Model.collectTypes(clazz)) {
                if (type.isInterface() && type != Service.class) {
                    lifestyles.put(type, new LazySingleton(clazz, initializer));
                    break;
                }
            }
        }

        private <T> void defineSelf(Class<T> clazz, Consumer<T>... initializer) {
            lifestyles.put(clazz, new LazySingleton(clazz, initializer));
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
    private static class LazySingleton<M> implements Lifestyle<M> {

        protected final Class<? extends M> type;

        protected M instance;

        private Consumer<M> initializer;

        protected LazySingleton(Class<? extends M> type, Consumer<M>... initializer) {
            this.type = type;
            this.initializer = initializer.length == 0 ? null : initializer[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized M call() throws Exception {
            if (instance == null) {
                instance = I.prototype(type).get();

                if (initializer != null) {
                    initializer.accept(instance);
                }
            }
            return instance;
        }
    }
}