/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
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

import kiss.Disposable;
import kiss.Events;
import kiss.I;

/**
 * @version 2017/01/10 1:07:57
 */
public class RESTClient {

    private final TransferView view = I.make(TransferView.class);

    /** The actual http client. */
    private final HttpClient client;

    /**
     * 
     */
    public RESTClient() {
        this(builder -> {
        });
    }

    /**
     * <p>
     * Create client with basic credentials.
     * </p>
     * 
     * @param name
     * @param password
     */
    public RESTClient(String name, String password) {
        this(builder -> {
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(name, password));

            builder.setDefaultCredentialsProvider(provider);
        });
    }

    /**
     * 
     */
    public RESTClient(Consumer<HttpClientBuilder> builder) {
        HttpClientBuilder b = HttpClientBuilder.create();
        builder.accept(b);
        client = b.build();
    }

    /**
     * <p>
     * GET request.
     * </p>
     * 
     * @param uri
     * @param type
     * @return
     */
    public <T> Events<String> get(String uri) {
        return request(new HttpGet(uri), null);
    }

    /**
     * <p>
     * GET request.
     * </p>
     * 
     * @param uri
     * @param type
     * @return
     */
    public <T> Events<T> get(String uri, Class<T> type) {
        return get(uri, I.make(type));
    }

    /**
     * <p>
     * GET request.
     * </p>
     * 
     * @param uri
     * @param type
     * @return
     */
    public <T> Events<T> get(String uri, T value) {
        return request(new HttpGet(uri), value);
    }

    /**
     * <p>
     * GET file request.
     * </p>
     * 
     * @param uri
     * @param type
     * @return
     */
    public Events<Path> get(String uri, Path file) {
        return request(new HttpGet(uri), file);
    }

    /**
     * <p>
     * Create POST request.
     * </p>
     * 
     * @param uri
     * @return
     */
    public <T> Events<T> post(String uri, T value) {
        StringBuilder builder = new StringBuilder();
        I.write(value, builder);

        HttpPost post = new HttpPost(uri);
        post.setEntity(new StringEntity(builder.toString(), StandardCharsets.UTF_8));
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");

        return request(post, value);
    }

    /**
     * <p>
     * Create PATCH request.
     * </p>
     * 
     * @param uri
     * @return
     */
    public <T> Events<T> patch(String uri, T value) {
        StringBuilder builder = new StringBuilder();
        I.write(value, builder);

        HttpPatch patch = new HttpPatch(uri);
        patch.setEntity(new StringEntity(builder.toString(), StandardCharsets.UTF_8));
        patch.setHeader("Accept", "application/json");
        patch.setHeader("Content-type", "application/json");

        return request(patch, value);
    }

    /**
     * <p>
     * Create PUT request.
     * </p>
     * 
     * @param uri
     * @return
     */
    public <T> Events<T> put(String uri, T value, TransferResource resource) {
        HttpPut put = new HttpPut(uri);
        put.setEntity(new CountingHttpEntity(new FileEntity(resource.getFile(), type(resource)), view, resource));

        return request(put, value);
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

        default:
            return ContentType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * <p>
     * Execute http request.
     * </p>
     * 
     * @param request
     * @param value
     * @return
     */
    private <T> Events<T> request(HttpUriRequest request, T value) {
        return new Events<T>(observer -> {
            try {
                HttpResponse response = client.execute(request);
                switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                case HttpStatus.SC_CREATED:
                    if (request.getMethod().equals("GET")) {
                        if (value == null || value instanceof String) {
                            observer.accept((T) readAsString(response));
                        } else if (value instanceof Path) {
                            observer.accept((T) readAsFile(response, (Path) value));
                        } else {
                            I.read(readAsString(response), value);
                            observer.accept(value);
                        }
                    } else {
                        observer.accept(value);
                    }
                    break;

                default:
                    observer.error(new HttpException(response.toString()));
                    break;
                }
                return Disposable.Φ;
            } catch (Exception e) {
                observer.error(e);
            }
            return Disposable.Φ;
        });
    }

    /**
     * <p>
     * Helper method to read entity as {@link String}.
     * </p>
     * 
     * @param response
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private String readAsString(HttpResponse response) throws ParseException, IOException {
        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }

    /**
     * <p>
     * Helper method to read entity as {@link String}.
     * </p>
     * 
     * @param response
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    private Path readAsFile(HttpResponse response, Path file) throws IllegalStateException, IOException {
        BufferedInputStream input = new BufferedInputStream(response.getEntity().getContent());
        BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(file));

        I.copy(input, output, true);

        return file;
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

        /** The transfered size. */
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
        public void flush() throws IOException {
            super.flush();

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