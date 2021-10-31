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
import java.util.HashSet;
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
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
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
import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.synccontext.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.synccontext.NamedLockFactorySelector;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

import bee.BeeLoader;
import bee.Platform;
import bee.UserInterface;
import bee.util.TransferInterface;
import kiss.ExtensionFactory;
import kiss.I;
import kiss.Lifestyle;
import kiss.Managed;
import kiss.Singleton;
import kiss.Storable;
import kiss.Variable;
import kiss.model.Model;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

@Managed(Singleton.class)
public class Repository {

    /** The path to remote repository. */
    static final List<RemoteRepository> builtinRepositories = new CopyOnWriteArrayList();

    static {
        try {
            addRemoteRepository("Maven", "https://repo1.maven.org/maven2/");
            addRemoteRepository("JitPack", "https://jitpack.io/");
        } catch (Exception e) {
            throw I.quiet(e);
        }
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

    /** The default dependency filter. */
    private final List<DependencySelector> dependencyFilters = new ArrayList();

    /** The default dependency builder. */
    private final DependencyGraphTransformer dependencyBuilder = new ChainedDependencyGraphTransformer(new ConflictResolver(new NearestVersionSelector(), new JavaScopeSelector(), new SimpleOptionalitySelector(), new JavaScopeDeriver()), new JavaDependencyContextRefiner());

    /** The path to local repository. */
    private LocalRepository localRepository;

    /** The user interface. */
    private UserInterface ui = I.make(UserInterface.class);

    /**
     * Wiring components by hand.
     */
    Repository(Project project) {
        this.project = project;

        // create filter
        dependencyFilters.add(new OptionalDependencySelector());
        dependencyFilters.add(new ScopeDependencySelector("test", "provided"));

        // ============ RepositorySystem ============ //
        system = I.make(RepositorySystem.class);

        // ==================================================
        // Initialize
        // ==================================================
        setLocalRepository(Platform.BeeLocalRepository);

        // ============ RepositorySystemSession ============ //
        Set<DependencySelector> filters = new HashSet(dependencyFilters);
        filters.add(new ExclusionDependencySelector(project.exclusions));

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setDependencySelector(new AndDependencySelector(filters));
        session.setDependencyGraphTransformer(dependencyBuilder);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_DAILY);
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        session.setIgnoreArtifactDescriptorRepositories(true);
        session.setCache(new DefaultRepositoryCache());
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(ResolutionErrorPolicy.CACHE_ALL, ResolutionErrorPolicy.CACHE_ALL));

        // event listener
        session.setTransferListener(I.make(TransferInterface.class));
        session.setRepositoryListener(I.make(RepositoryView.class));

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
        return collectDependency(project, scopes, project.libraries);
    }

    /**
     * Collect all dependencies in the specified scope.
     * 
     * @param library
     * @param scopes
     * @return
     */
    public Set<Library> collectDependency(Library library, Scope... scopes) {
        return collectDependency(project, scopes, Collections.singleton(library));
    }

    /**
     * Collect all dependencies in the specified scope.
     * 
     * @param libraries
     * @param scopes
     * @return
     */
    private Set<Library> collectDependency(Project project, Scope[] scopes, Set<Library> libraries) {
        Set<Library> set = new TreeSet();

        // collect remote repository
        List<RemoteRepository> repositories = new ArrayList();
        repositories.addAll(builtinRepositories);
        repositories.addAll(project.repositories);

        for (Scope scope : scopes) {
            // collect dependency
            CollectRequest request = new CollectRequest();
            request.setRepositories(repositories);

            for (Library library : libraries) {
                Dependency dependency = new Dependency(library.artifact, library.scope.toString());

                if (scope.accept(dependency)) {
                    request.addDependency(dependency);
                }
            }

            try {
                DependencyResult result = system.resolveDependencies(session, new DependencyRequest(request, (node, parents) -> {
                    if (node == null || node.getArtifact() == null) {
                        return true;
                    }

                    List<DependencyNode> list = new ArrayList();

                    for (int i = parents.size() - 1; 0 <= i; i--) {
                        DependencyNode parent = parents.get(i);

                        if (parent != null && parent.getArtifact() != null) {
                            list.add(parent);
                        }
                    }
                    list.add(node);

                    Scope root = Scope.by(list.get(0).getDependency().getScope());
                    return list.stream().allMatch(n -> root.accept(n.getDependency()));
                }));

                for (ArtifactResult dependency : result.getArtifactResults()) {
                    Artifact artifact = dependency.getArtifact();
                    if (validateDependency(artifact)) {
                        set.add(new Library(artifact, Variable.of(dependency.getRepository()).map(ArtifactRepository::getId)));
                    }
                }
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
        return set;
    }

    /**
     * Validate artifact.
     * 
     * @param artifact
     * @return
     */
    private boolean validateDependency(Artifact artifact) {
        String group = artifact.getGroupId();
        String product = artifact.getArtifactId();

        // remove project itself
        if (group.equalsIgnoreCase(project.getGroup()) && product.equalsIgnoreCase(project.getProduct())) {
            return false;
        }

        return true;
    }

    /**
     * Resolve the latest version of the specified library.
     * 
     * @param library
     * @return
     */
    public String resolveLatestVersion(Library library) {
        try {
            return system.resolveArtifact(session, new ArtifactRequest(library.artifact, repository(), null)).getArtifact().getVersion();
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
            ArtifactRequest request = new ArtifactRequest(sub, repository(library.repositoryId), null);

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
    private List<RemoteRepository> repository() {
        List<RemoteRepository> repositories = new ArrayList();
        repositories.addAll(builtinRepositories);
        repositories.addAll(project.repositories);
        return repositories;
    }

    /**
     * Search repository by ID.
     * 
     * @param id A repository identifier.
     * @return A matched repository.
     */
    private List<RemoteRepository> repository(Variable<String> id) {
        List<RemoteRepository> repositories = new ArrayList<>();
        repositories.addAll(builtinRepositories);
        repositories.addAll(project.repositories);

        for (RemoteRepository repo : repositories) {
            if (id.is(repo.getId())) {
                return Collections.singletonList(repo);
            }
        }
        return Collections.EMPTY_LIST;
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
     * Load library and import it dynamically.
     * 
     * @param group A group name.
     * @param product A product name.
     * @param version A product version.
     */
    public static void require(String group, String product, String version) {
        Library require = new Library(group, product, version);

        for (Library library : I.make(Repository.class).collectDependency(require, Scope.Runtime)) {
            BeeLoader.load(library.getLocalJar());
        }
    }

    /**
     * @version 2015/06/23 12:26:57
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
            ui.info("Deployed " + event.getArtifact() + " to " + event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactDeploying(RepositoryEvent event) {
            ui.info("Deploying " + event.getArtifact() + " to " + event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void artifactDescriptorInvalid(RepositoryEvent event) {
            ui.info("Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage());
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
            ui.info("Install " + event.getArtifact() + " to " + event.getFile());
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
            ui.info("Deployed " + event.getMetadata() + " to " + event.getRepository());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void metadataDeploying(RepositoryEvent event) {
            ui.info("Deploying " + event.getMetadata() + " to " + event.getRepository());
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
            ui.info("Invalid metadata " + event.getMetadata());
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
     * @version 2017/03/09 14:17:27
     */
    private static final class LibraryInfo implements Storable<LibraryInfo> {

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
         * <p>
         * Confirm whether we should check the source resource or not.
         * </p>
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
         * <p>
         * Build {@link Library} infomation.
         * </p>
         * 
         * @param library
         * @return
         */
        private static LibraryInfo of(Library library) {
            return new LibraryInfo(library).restore();
        }
    }

    /**
     * Lazy intializable singleton.
     */
    protected static class LazySingleton<M> implements Lifestyle<M> {

        protected final Class<? extends M> type;

        protected M instance;

        private Consumer<M> initializer;

        protected LazySingleton(Class<? extends M> type) {
            this.type = type;
        }

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

    /**
     * Define various {@link Lifestyle}s.
     */
    @Managed(Singleton.class)
    private static class Lifestyles implements ExtensionFactory<Lifestyle> {

        private final Map<Class, Lifestyle> lifestyles = new ConcurrentHashMap();

        private Lifestyles() {
            define(DefaultRepositorySystem.class);
            define(DefaultArtifactResolver.class);
            define(DefaultDependencyCollector.class);
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
            define(NamedLockFactorySelector.class, NamedLockFactorySelector::new);
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
}