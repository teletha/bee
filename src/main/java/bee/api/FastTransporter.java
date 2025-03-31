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
import java.util.Optional;
import java.util.OptionalLong;

import javax.inject.Named;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.connector.transport.TransportListener;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.ChecksumFailureException;
import org.eclipse.aether.transfer.NoTransporterException;

import bee.UserInterface;
import kiss.I;
import kiss.WiseConsumer;

/**
 * For HTTP and HTTPS.
 */
@Named("https")
class FastTransporter implements TransporterFactory {

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
                            if (start != 4 && end != -1) {
                                return Optional.of(etag.substring(start, end));
                            }

                            start = etag.indexOf('"') + 1;
                            end = etag.indexOf('"', start);
                            if (start != 0 && end != -1) {
                                return Optional.of(etag.substring(start, end));
                            }

                            return etag.isBlank() ? Optional.empty() : Optional.of(etag);
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

                default:
                    I.make(UserInterface.class).debug("CHECKSUM is not found in http headers [", task.getLocation(), "]", headers.map().entrySet());
                }
            }
        };
    }
}