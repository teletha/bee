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
import kiss.Managed;
import kiss.Singleton;

/**
 * Data transfer manager.
 */
@Managed(Singleton.class)
public class Transfers extends AbstractRepositoryListener implements TransferListener {

    /** The user notifier. */
    private final UserInterface ui;

    /**
     * @param ui
     */
    private Transfers(UserInterface ui) {
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
    private Map<String, TransferEvent> downloading = new ConcurrentHashMap();

    /** The uploading items. */
    private Map<String, TransferEvent> uploading = new ConcurrentHashMap();

    /**
     * {@inheritDoc}
     */
    @Override
    public void transferInitiated(TransferEvent event) {
        boolean download = event.getRequestType() != RequestType.PUT;
        Map<String, TransferEvent> resources = download ? downloading : uploading;
        resources.put(event.getResource().getResourceName(), event);
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
        boolean download = event.getRequestType() != RequestType.PUT;
        Map<String, TransferEvent> resources = download ? downloading : uploading;
        resources.put(event.getResource().getResourceName(), event);

        long now = System.nanoTime();
        if (interval < now - last) {
            last = now; // update last event time

            StringBuilder message = new StringBuilder();
            for (Entry<String, TransferEvent> entry : resources.entrySet().stream().limit(5).toList()) {
                if (!message.isEmpty()) message.append(Platform.EOL);
                message.append(buildMessage(download ? "Downloading" : "Uploading", entry.getValue(), true));
            }
            ui.trace(message);
        }
    }

    public void transferProgressed(String resourceName, RequestType type, long current, long total) {
        boolean download = type != RequestType.PUT;
        Map<String, TransferEvent> resources = download ? downloading : uploading;
        resources.put(resourceName, event);

        long now = System.nanoTime();
        if (interval < now - last) {
            last = now; // update last event time

            StringBuilder message = new StringBuilder();
            for (Entry<String, TransferEvent> entry : resources.entrySet().stream().limit(5).toList()) {
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
        boolean download = event.getRequestType() != RequestType.PUT;
        Map<String, TransferEvent> resources = download ? downloading : uploading;
        resources.remove(event.getResource().getResourceName());

        ui.info(buildMessage(download ? "Downloaded" : "Uploaded", event, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transferFailed(TransferEvent event) {
        boolean download = event.getRequestType() != RequestType.PUT;
        Map<String, TransferEvent> resources = download ? downloading : uploading;
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
     * @param string
     * @param event
     * @param b
     * @return
     */
    private static String buildMessage(String type, TransferEvent event, boolean progress) {
        return buildMessage(event.getResource().getResourceName());
    }

    /**
     * Build item transfering message.
     * 
     * @param type
     * @param event
     * @return
     */
    private static String buildMessage(String resourceName, String type, long size, long current, String repositoryId, boolean progress) {
        StringBuilder message = new StringBuilder(type).append(" : ").append(name(resourceName)).append(" (");
        if (progress && 0 < size) {
            message.append(Inputs.formatAsSize(current, false)).append('/').append(Inputs.formatAsSize(size));
        } else {
            message.append(Inputs.formatAsSize(current));
        }
        message.append(" @ ").append(repositoryId).append(") ");

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
}