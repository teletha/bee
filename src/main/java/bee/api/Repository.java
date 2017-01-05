/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
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

import bee.Platform;
import bee.UserInterface;
import bee.util.Paths;
import kiss.I;
import kiss.Manageable;

/**
 * @version 2017/01/05 13:53:19
 */
@Manageable(lifestyle = ProjectSpecific.class)
public class Repository {

    /** The current processing project. */
    private final Project project;

    /** The root repository system. */
    private final RepositorySystem system;

    /** The default dependency filter. */
    private final List<DependencySelector> dependencyFilters = new ArrayList();

    /** The default dependency builder. */
    private final DependencyGraphTransformer dependencyBuilder = new ChainedDependencyGraphTransformer(new ConflictResolver(new NearestVersionSelector(), new JavaScopeSelector(), new SimpleOptionalitySelector(), new JavaScopeDeriver()), new JavaDependencyContextRefiner());

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

        // ============ RepositorySystem ============ //
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, WagonTransporterFactory.class);
        locator.addService(WagonProvider.class, BeeWagonProvider.class);
        system = locator.getService(RepositorySystem.class);

        // ==================================================
        // Initialize
        // ==================================================
        setLocalRepository(Platform.BeeLocalRepository);
        addRemoteRepository("central", "http://repo1.maven.org/maven2/");
        addRemoteRepository("jboss", "http://repository.jboss.org/nexus/content/groups/public-jboss/");
        addRemoteRepository("jitpack", "https://jitpack.io/");
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

        // dependency dependencyCollector
        CollectRequest request = new CollectRequest();
        request.setRepositories(repositories);

        for (Library library : libraries) {
            // install tools.jar if needed
            if ((library.group.equals("sun.jdk") || library.group.equals("com.sun")) && library.name.equals("tools") && Files
                    .notExists(library.getJar())) {
                Project dummy = new Project();
                dummy.product(library.group, library.name, library.version);

                install(dummy, Platform.JavaTool, Platform.JavaHome.resolve("src.zip"));
            }

            Dependency dependency = new Dependency(library.artifact, library.scope.toString());

            if (scope.accept(dependency)) {
                request.addDependency(dependency);
            }
        }

        try {
            DependencyResult result = system.resolveDependencies(newSession(), new DependencyRequest(request, (node, parents) -> {
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
                    set.add(new Library(artifact));
                }
            }
            return set;
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Validate artifact.
     * </p>
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
     */
    public void install(Project project) {
        install(project, project.locateJar());
    }

    /**
     * <p>
     * Install project into the local repository.
     * </p>
     * 
     * @param project A project to install.
     */
    public void install(Project project, Path classes) {
        install(project, classes, project.locateSourceJar());
    }

    /**
     * <p>
     * Install project into the local repository.
     * </p>
     * 
     * @param project A project to install.
     */
    public void install(Project project, Path classes, Path sources) {
        String group = project.getGroup();
        String product = project.getProduct();
        String version = project.getVersion();

        // create artifact for project
        Artifact jar = new DefaultArtifact(group, product, "", "jar", version, null, classes.toFile());

        try {
            InstallRequest request = new InstallRequest();
            request.addArtifact(jar);
            request.addArtifact(new SubArtifact(jar, "", "pom", Paths.write(project.toString()).toFile()));
            if (Paths.exist(sources)) {
                request.addArtifact(new SubArtifact(jar, "sources", "jar", sources.toFile()));
            }
            system.install(newSession(), request);
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
        remoteRepositories.add(new RemoteRepository.Builder(name, "default", url).build());
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

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setDependencySelector(new AndDependencySelector(filters));
        session.setDependencyGraphTransformer(dependencyBuilder);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_DAILY);
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        session.setIgnoreArtifactDescriptorRepositories(true);

        // event listener
        session.setTransferListener(I.make(TransferView.class));
        session.setRepositoryListener(I.make(RepositoryView.class));

        // API definition
        return session;
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
     * @version 2015/06/23 12:26:51
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
     * @version 2015/06/23 10:45:33
     */
    private static class BeeWagonProvider implements WagonProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public Wagon lookup(String scheme) throws Exception {
            if (scheme.equals("http")) {
                LightweightHttpWagon wagon = new LightweightHttpWagon();
                wagon.setAuthenticator(new LightweightHttpWagonAuthenticator());

                return wagon;
            } else if (scheme.equals("https")) {
                LightweightHttpsWagon wagon = new LightweightHttpsWagon();
                wagon.setAuthenticator(new LightweightHttpWagonAuthenticator());

                return wagon;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void release(Wagon wagon) {
        }
    }
}
