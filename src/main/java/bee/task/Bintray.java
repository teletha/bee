/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferEvent.RequestType;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;

import bee.api.Command;
import bee.api.Library;
import bee.api.Project;
import bee.api.Task;
import bee.util.Paths;
import bee.util.TransferView;
import kiss.Disposable;
import kiss.Events;
import kiss.I;

/**
 * @version 2017/01/06 11:29:35
 */
public class Bintray extends Task {

    @Command("Test Bintray")
    public void rest() {
        require(Install.class).project();

        Client client = new Client();

        String orgnaization = "npc";
        String repositoryName = "maven";

        Library library = project.getLibrary();

        Description desc = new Description(orgnaization, repositoryName);

        Package pack = new Package();
        pack.name = library.name;
        pack.desc = project.getDescription();

        client.get(desc, "repos/" + desc)
                .errorResume(client.post(desc, "repos/" + desc))
                .flatMap(r -> client.get(pack, "packages/" + desc + "/" + library.name))
                .errorResume(client.post(pack, "packages/" + desc))
                .flatMap(p -> client.get(new RepositoryFiles(), "packages/" + desc + "/" + library.name + "/files"))
                .flatIterable(files -> {
                    RepositoryFiles completes = new RepositoryFiles();
                    completes.add(RepositoryFile.of(library.getPOM(), library.getLocalPOM()));
                    completes.add(RepositoryFile.of(library.getJar(), library.getLocalJar()));
                    completes.add(RepositoryFile.of(library.getSourceJar(), library.getLocalSourceJar()));
                    completes.add(RepositoryFile.of(library.getJavadocJar(), library.getLocalJavadocJar()));
                    completes.removeAll(files);
                    return completes;
                })
                .take(file -> Files.exists(file.local))
                .flatMap(file -> client
                        .put(new Up(), "maven/" + desc + "/" + library.name + "/" + file.path + ";publish=1;override=1", file.resource()))
                .to(r -> {
                    System.out.println(r.message);
                }, e -> {
                    e.printStackTrace();
                });
    }

    /**
     * @version 2017/01/06 13:59:11
     */
    private class Client {

        private String name = "teletha";

        private String key = "7d639e65a03d3714524a719f63ce818af7f7114a";

        private final TransferView view = I.make(TransferView.class);

        /** The actual http client. */
        private final HttpClient client;

        /**
         * 
         */
        private Client() {
            Credentials credentials = new UsernamePasswordCredentials(name, key);
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY, credentials);

            this.client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        }

        /**
         * <p>
         * Create GET request.
         * </p>
         * 
         * @param api
         * @return
         */
        private <T> Events<T> get(T value, String api) {
            return request(value, new HttpGet(fromPath(api)));
        }

        /**
         * <p>
         * Create POST request.
         * </p>
         * 
         * @param api
         * @return
         */
        private <T> Events<T> post(T value, String api) {
            StringBuilder builder = new StringBuilder();
            I.write(value, builder);

            HttpPost post = new HttpPost(fromPath(api));
            post.setEntity(new StringEntity(builder.toString(), StandardCharsets.UTF_8));
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            return request(value, post);
        }

        /**
         * <p>
         * Create PUT request.
         * </p>
         * 
         * @param api
         * @return
         */
        private <T> Events<T> put(T value, String api, TransferResource resource) {
            TransferView view = I.make(TransferView.class);

            HttpPut put = new HttpPut(fromPath(api));
            put.setEntity(new CountingHttpEntity(new FileEntity(resource.getFile(), type(resource)), view, resource));

            return request(value, put);
        }

        /**
         * <p>
         * Detect content-type.
         * </p>
         * 
         * @param resource
         * @return
         */
        private ContentType type(TransferResource resource) {
            switch (Paths.getExtension(resource.getFile().toPath())) {
            case "pom":
                return ContentType.APPLICATION_XML;

            case "md5":
            case "sha1":
                return ContentType.TEXT_PLAIN;

            default:
                return ContentType.DEFAULT_BINARY;
            }
        }

        /**
         * <p>
         * Create REST API.
         * </p>
         * 
         * @param api
         * @return
         */
        private String fromPath(String api) {
            return "https://api.bintray.com/" + api;
        }

        /**
         * <p>
         * Create request.
         * </p>
         * 
         * @param type
         * @param request
         * @return
         */
        private <T> Events<T> request(T value, HttpUriRequest request) {
            return new Events<T>(observer -> {
                try {
                    System.out.println(request.getURI());
                    HttpResponse response = client.execute(request);

                    switch (response.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_OK:
                    case HttpStatus.SC_CREATED:
                        I.read(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), value);
                        observer.accept(value);
                        break;

                    default:
                        observer.error(new HttpException(response.toString()));
                        break;
                    }
                } catch (Exception e) {
                    throw I.quiet(e);
                }
                return Disposable.Φ;
            });
        }
    }

    /**
     * @version 2017/01/06 13:10:36
     */
    private static class Description {

        /** The name. */
        public String name;

        /** The repository name. */
        public String repo;

        /** The repository owner. */
        public String owner;

        /** The repository type. (default is maven) */
        public String type = "maven";

        /** The repository description. */
        public String desc;

        /** The response message. */
        public String message;

        /**
         * <p>
         * With name.
         * </p>
         * 
         * @param name
         */
        public Description(String owner, String name) {
            this.name = name;
            this.owner = owner;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return owner + "/" + name;
        }
    }

    /**
     * @version 2017/01/08 15:59:24
     */
    private static class Package {

        /** The name. */
        public String name;

        /** The description. */
        public String desc;
    }

    /**
     * @version 2017/01/08 16:29:13
     */
    @SuppressWarnings("serial")
    private static class RepositoryFiles extends ArrayList<RepositoryFile> {
    }

    /**
     * @version 2017/01/08 16:29:23
     */
    private static class RepositoryFile {

        /** The file name. */
        public String name;

        /** The file path. */
        public String path;

        /** The local file path. */
        public Path local;

        /** The file size. */
        public long size;

        /** The file checksum. */
        public String sha1;

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(sha1);
        }

        /**
         * <p>
         * Create the resource to transfer.
         * </p>
         * 
         * @return
         */
        private TransferResource resource() {
            try {
                Project project = I.make(Project.class);
                String repository = "http://" + project.getGroup() + ".bintray.com/maven";

                return new TransferResource(repository, path, local.toFile(), null).setContentLength(Files.size(local));
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RepositoryFile == false) {
                return false;
            }
            return Objects.equals(sha1, ((RepositoryFile) obj).sha1);
        }

        /**
         * @param path
         * @param file
         * @return
         */
        public static RepositoryFile of(String path, Path file) {
            try {
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                sha1.update(Files.readAllBytes(file));

                RepositoryFile f = new RepositoryFile();
                f.name = I.locate(path).getFileName().toString();
                f.path = path;
                f.local = file;
                f.size = Files.size(file);
                f.sha1 = String.valueOf(Hex.encodeHex(sha1.digest()));

                return f;
            } catch (NoSuchAlgorithmException e) {
                throw I.quiet(e);
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "RepositoryFile [path=" + path + ", size=" + size + ", sha1=" + sha1 + "]";
        }
    }

    /**
     * @version 2017/01/08 17:34:10
     */
    private static class Up {

        public String message;
    }

    /**
     * @version 2017/01/08 16:09:59
     */
    private static class CountingHttpEntity extends HttpEntityWrapper {

        /** The transfer view. */
        private final TransferView view;

        /** The resource to transfer. */
        private final TransferResource resource;

        /**
         * @param entity
         * @param listener
         * @param resource
         */
        private CountingHttpEntity(HttpEntity entity, TransferView listener, TransferResource resource) {
            super(entity);

            this.view = listener;
            this.resource = resource;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeTo(final OutputStream out) throws IOException {
            this.wrappedEntity.writeTo(out instanceof CountingOutputStream ? out : new CountingOutputStream(out, view, resource));
        }
    }

    /**
     * @version 2017/01/08 16:09:55
     */
    private static class CountingOutputStream extends FilterOutputStream {

        private static final RepositorySystemSession session = new Session();

        /** The transfer view. */
        private final TransferView view;

        /** The resource to transfer. */
        private final TransferResource resource;

        private long transferred;

        /**
         * @param out
         * @param view
         * @param resource
         */
        private CountingOutputStream(OutputStream out, TransferView view, TransferResource resource) {
            super(out);

            this.view = view;
            this.resource = resource;
            this.transferred = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            transferred += len;

            view.transferProgressed(event());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int b) throws IOException {
            write(new byte[] {(byte) b}, 0, 1);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            super.close();

            view.transferSucceeded(event());
        }

        /**
         * <p>
         * Create event.
         * </p>
         * 
         * @return
         */
        private TransferEvent event() {
            return new TransferEvent.Builder(session, resource).setRequestType(RequestType.PUT).setTransferredBytes(transferred).build();
        }
    }

    /**
     * @version 2017/01/08 23:25:25
     */
    private static class Session implements RepositorySystemSession {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isOffline() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isIgnoreArtifactDescriptorRepositories() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResolutionErrorPolicy getResolutionErrorPolicy() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ArtifactDescriptorPolicy getArtifactDescriptorPolicy() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getChecksumPolicy() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getUpdatePolicy() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalRepository getLocalRepository() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LocalRepositoryManager getLocalRepositoryManager() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WorkspaceReader getWorkspaceReader() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RepositoryListener getRepositoryListener() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TransferListener getTransferListener() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, String> getSystemProperties() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, String> getUserProperties() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, Object> getConfigProperties() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MirrorSelector getMirrorSelector() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ProxySelector getProxySelector() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AuthenticationSelector getAuthenticationSelector() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ArtifactTypeRegistry getArtifactTypeRegistry() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DependencyTraverser getDependencyTraverser() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DependencyManager getDependencyManager() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DependencySelector getDependencySelector() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VersionFilter getVersionFilter() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DependencyGraphTransformer getDependencyGraphTransformer() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SessionData getData() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RepositoryCache getCache() {
            return null;
        }
    }
}
