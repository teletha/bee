/*
 * Copyright (C) 2024 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferEvent.RequestType;
import org.eclipse.aether.transfer.TransferListener;

import bee.Platform;
import bee.UserInterface;
import bee.util.Inputs;
import kiss.I;
import kiss.Managed;
import kiss.Singleton;
import kiss.WiseConsumer;
import psychopath.File;
import psychopath.Locator;

/**
 * Data transfer manager.
 */
@Managed(Singleton.class)
public class Loader extends AbstractRepositoryListener implements TransferListener {

    /** The user notifier. */
    private final UserInterface ui;

    /**
     * @param ui
     */
    private Loader(UserInterface ui) {
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
    private static final long interval = 200 * 1000 * 1000;

    /** The last progress event time. */
    private long last = 0;

    /** The downloading items. */
    private Map<String, Resource> downloading = new ConcurrentHashMap();

    /** The uploading items. */
    private Map<String, Resource> uploading = new ConcurrentHashMap();

    /**
     * {@inheritDoc}
     */
    @Override
    public void transferInitiated(TransferEvent event) {
        boolean download = event.getRequestType() != RequestType.PUT;
        Map<String, Resource> resources = download ? downloading : uploading;
        resources.put(event.getResource().getResourceName(), new Resource(event));
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
        transferProgressed(new Resource(event));
    }

    private void transferProgressed(Resource resource) {
        boolean download = resource.type != RequestType.PUT;
        Map<String, Resource> resources = download ? downloading : uploading;
        resources.put(resource.name, resource);

        long now = System.nanoTime();
        if (interval < now - last) {
            last = now; // update last event time

            StringBuilder message = new StringBuilder();
            for (Entry<String, Resource> entry : resources.entrySet().stream().limit(5).toList()) {
                if (!message.isEmpty()) message.append(Platform.EOL);
                message.append(buildMessage(download ? "Downloading" : "Uploading", entry.getValue(), true));
            }
            ui.trace(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transferSucceeded(TransferEvent event) {
        transferSucceeded(new Resource(event));
    }

    private void transferSucceeded(Resource resource) {
        boolean download = resource.type != RequestType.PUT;
        Map<String, Resource> resources = download ? downloading : uploading;
        resources.remove(resource.name);

        ui.info(buildMessage(download ? "Downloaded" : "Uploaded", resource, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transferFailed(TransferEvent event) {
        boolean download = event.getRequestType() != RequestType.PUT;
        Map<String, Resource> resources = download ? downloading : uploading;
        resources.remove(event.getResource().getResourceName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transferCorrupted(TransferEvent event) {
        ui.error(event.getException());
    }

    /**
     * Build item transfering message.
     * 
     * @param action
     * @param resource
     * @param progress
     * @return
     */
    private static String buildMessage(String action, Resource resource, boolean progress) {
        StringBuilder message = new StringBuilder(action).append(" : ").append(name(resource.name)).append(" (");
        if (progress && 0 < resource.size) {
            message.append(Inputs.formatAsSize(resource.current, false)).append('/').append(Inputs.formatAsSize(resource.size));
        } else {
            message.append(Inputs.formatAsSize(resource.current));
        }
        message.append(" @ ").append(resource.repository).append(") ");

        return message.toString();
    }

    /**
     * Compute readable resource name.
     * 
     * @param resourceName
     * @return
     */
    private static String name(String resourceName) {
        int last = resourceName.lastIndexOf('/');
        String name = resourceName.substring(last + 1);

        if (name.equals("maven-metadata.xml")) {
            return name + " for " + resourceName.substring(resourceName.lastIndexOf('/', last - 1) + 1, last);
        } else {
            return name;
        }
    }

    private record Resource(RequestType type, String name, String repository, long size, long current) {
        Resource(TransferEvent e) {
            this(e.getRequestType(), e.getResource().getResourceName(), e.getResource().getRepositoryId(), e.getDataLength(), e
                    .getTransferredBytes());
        }
    }

    /**
     * Download file from the specified uri.
     * 
     * @param uri
     * @return The temporary downloaded file.
     */
    public static File download(String uri) {
        File temp = Locator.temporaryFile();
        donwload(uri, temp);
        return temp;
    }

    /**
     * Download file from the specified uri.
     * 
     * @param uri
     * @param file
     */
    public static void donwload(String uri, File file) {
        I.http(uri, HttpResponse.class).waitForTerminate().to((WiseConsumer<HttpResponse>) res -> {
            String host = URI.create(uri).getHost();

            // analyze header
            HttpHeaders headers = res.headers();
            long total = headers.firstValueAsLong("Content-Length").orElse(0);

            // transfer data
            try (InputStream in = (InputStream) res.body(); OutputStream out = new FileOutputStream(file.asJavaFile())) {
                int passed = 0;
                Loader transfers = I.make(Loader.class);
                transfers.transferProgressed(new Resource(RequestType.GET, uri, host, total, passed));

                int read = -1;
                byte[] buffer = new byte[1024 * 32];
                while (0 < (read = in.read(buffer))) {
                    passed += read;
                    out.write(buffer, 0, read);
                    transfers.transferProgressed(new Resource(RequestType.GET, uri, host, total, passed));
                }
                transfers.transferSucceeded(new Resource(RequestType.GET, uri, host, total, passed));
            }
        });
    }
}