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
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
 * Manages data transfers (downloading and uploading) and reports progress to the user interface.
 * This class listens to Maven repository events (via {@link AbstractRepositoryListener})
 * and transfer events (via {@link TransferListener}) to provide real-time feedback
 * on artifact resolution and deployment, as well as handling direct downloads via HTTP.
 */
@Managed(Singleton.class)
public class Loader extends AbstractRepositoryListener implements TransferListener {

    /** The minimum interval in milliseconds between progress update events sent to the UI. */
    private static final long interval = 200;

    /** The user interface instance used for displaying messages and progress updates. */
    private final UserInterface ui;

    /** Manages the state of currently downloading items. */
    private final Manager downloading = new Manager(true);

    /** Manages the state of currently uploading items. */
    private final Manager uploading = new Manager(false);

    /**
     * Constructs a Loader.
     *
     * @param ui The user interface implementation to interact with.
     */
    private Loader(UserInterface ui) {
        this.ui = ui;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Logs information about an artifact being installed into the local repository.
     * </p>
     */
    @Override
    public void artifactInstalled(RepositoryEvent event) {
        ui.info("Install " + event.getArtifact() + " to " + event.getFile());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called when a transfer is initiated (e.g., a download or upload is requested).
     * Registers the transfer item with the appropriate manager.
     * </p>
     */
    @Override
    public void transferInitiated(TransferEvent event) {
        initiate(new Item(event));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called when the transfer actually starts after initiation. Currently, no specific action is
     * taken here.
     * </p>
     */
    @Override
    public void transferStarted(TransferEvent event) throws TransferCancelledException {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called periodically as data is transferred. Updates the progress display in the UI,
     * throttled by the defined interval.
     * </p>
     */
    @Override
    public void transferProgressed(TransferEvent event) {
        progress(new Item(event));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called when a transfer completes successfully. Logs the completion message
     * and removes the item from the corresponding manager.
     * </p>
     */
    @Override
    public void transferSucceeded(TransferEvent event) {
        success(new Item(event));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called when a transfer fails (e.g., network error, file not found), excluding corruption.
     * Removes the item from the manager and logs an error.
     * </p>
     */
    @Override
    public void transferFailed(TransferEvent event) {
        fail(new Item(event));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called when a transfer fails specifically due to data corruption (e.g., checksum mismatch).
     * Removes the item from the manager and logs a corruption error.
     * </p>
     */
    @Override
    public void transferCorrupted(TransferEvent event) {
        corrupt(new Item(event));
    }

    /**
     * Selects the appropriate {@link Manager} (downloading or uploading) based on the transfer
     * item's type.
     *
     * @param item The transfer item.
     * @return The {@link Manager} responsible for this type of transfer.
     */
    private Manager managerBy(Item item) {
        return item.type == RequestType.GET ? downloading : uploading;
    }

    /**
     * Handles the initiation of a transfer by adding the item to the correct manager.
     *
     * @param item The transfer item being initiated.
     */
    private void initiate(Item item) {
        Manager manager = managerBy(item);
        manager.add(item);
    }

    /**
     * Handles progress updates for a transfer. Updates the item's state in the manager
     * and, if enough time has passed since the last UI update, displays the current progress.
     *
     * @param item The transfer item with updated progress information (bytes transferred).
     */
    private void progress(Item item) {
        Manager manager = managerBy(item);
        manager.add(item);

        long now = System.currentTimeMillis();
        if (interval < now - manager.last) {
            manager.last = now; // update last event time

            StringBuilder message = new StringBuilder();
            for (Item i : manager.items.values().stream().limit(5).toList()) {
                if (!message.isEmpty()) message.append(Platform.EOL);
                message.append(message(manager.download ? "Downloading" : "Uploading", i, true));
            }

            if (!message.isEmpty()) ui.trace(message);
        }
    }

    /**
     * Handles the successful completion of a transfer. Logs a confirmation message
     * and removes the item from the manager.
     *
     * @param item The item that has been successfully transferred.
     */
    private void success(Item item) {
        Manager manager = managerBy(item);
        manager.remove(item);

        ui.info(message(manager.download ? "Downloaded" : "Uploaded", item, false));
    }

    /**
     * Handles a failed transfer. Logs an error message and removes the item from the manager.
     *
     * @param item The item whose transfer failed.
     */
    private void fail(Item item) {
        Manager manager = managerBy(item);
        manager.remove(item);

        ui.debug("Failed ", (manager.download ? "download" : "upload"), ": ", name(item.name), " from ", item.repository);
    }

    /**
     * Handles a corrupted transfer. Logs a specific error message indicating corruption
     * and removes the item from the manager.
     *
     * @param item The item whose transfer was corrupted.
     */
    private void corrupt(Item item) {
        Manager manager = managerBy(item);
        manager.remove(item);

        ui.debug("Corrupted ", (manager.download ? "download" : "upload"), ": ", name(item.name), " from ", item.repository);
    }

    /**
     * Builds a human-readable message describing the state of a transfer item.
     *
     * @param action The action being performed (e.g., "Downloading", "Uploaded").
     * @param resource The transfer item containing details.
     * @param progress If true, display progress as "current/total"; otherwise, display only the
     *            final size.
     * @return A formatted string message for logging or UI display.
     */
    private static String message(String action, Item resource, boolean progress) {
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
     * Computes a more readable name from a resource path, providing context for Maven metadata
     * files.
     *
     * @param resourceName The full resource name, typically a path like
     *            "group/artifact/version/file.jar".
     * @return A user-friendly representation of the resource name.
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

    /**
     * Downloads a file from the specified URI to a temporary file location.
     * This method blocks until the download is complete or fails.
     * Progress is reported using the singleton {@link Loader} instance.
     *
     * @param uri The string representation of the URI to download from.
     * @return A {@link File} object representing the downloaded content in a temporary location.
     * @throws RuntimeException wrapping any exceptions that occur during download (e.g., HTTP
     *             errors, I/O errors).
     */
    public static File download(String uri) {
        File temp = Locator.temporaryFile();
        download(uri, temp);
        return temp;
    }

    /**
     * Downloads a file from the specified URI and saves it to the provided {@link File}
     * destination.
     * This method blocks until the download is complete or fails.
     * Progress is reported using the singleton {@link Loader} instance.
     * <p>
     * Note: The method name "donwload" contains a typo (should be "download"). It is kept here
     * for compatibility with existing code that might call it.
     * </p>
     *
     * @param uri The string representation of the URI to download from.
     * @param file The destination {@link File} to save the downloaded content to.
     * @throws RuntimeException wrapping any exceptions that occur during download (e.g., HTTP
     *             errors, I/O errors).
     */
    public static void download(String uri, File file) {
        I.http(uri, HttpResponse.class).waitForTerminate().to((WiseConsumer<HttpResponse>) res -> {
            String host = URI.create(uri).getHost();

            // analyze header
            HttpHeaders headers = res.headers();
            long total = headers.firstValueAsLong("Content-Length").orElse(0);

            Loader transfers = I.make(Loader.class);

            // Create initial transfer item and initiate tracking
            Item item = new Item(RequestType.GET, uri, host, total, 0);
            transfers.initiate(item);

            // transfer data
            try (InputStream in = (InputStream) res.body(); OutputStream out = new FileOutputStream(file.asJavaFile())) {
                int passed = 0;

                int read = -1;
                byte[] buffer = new byte[1024 * 32];
                while (0 < (read = in.read(buffer))) {
                    passed += read;
                    out.write(buffer, 0, read);
                    transfers.progress(new Item(RequestType.GET, uri, host, total, passed));
                }
                transfers.success(new Item(RequestType.GET, uri, host, total, passed));
            } catch (Exception e) {
                // Handle exceptions occurring during initial setup or HTTP request execution
                // Ensure failure is reported even if initiation didn't fully complete
                transfers.fail(item);
                file.delete(); // Attempt to delete file if created
                throw I.quiet(e);
            }
        });
    }

    /**
     * Internal manager for tracking a set of active transfers (either downloads or uploads).
     * Uses a {@link ConcurrentHashMap} for thread-safe storage of transfer items.
     */
    private static class Manager {

        /** True if this manager handles downloads, false for uploads. */
        private final boolean download;

        /** Atomic counter for the number of currently active transfers in this manager. */
        private final AtomicInteger count = new AtomicInteger();

        /** A map storing the active transfer items, keyed by "name@repository". */
        private final Map<String, Item> items = new ConcurrentHashMap<>();

        /**
         * Timestamp of the last progress update sent to the UI (volatile for thread visibility).
         */
        private volatile long last = 0;

        /**
         * Creates a Manager instance.
         * 
         * @param download Specifies whether this manager tracks downloads (true) or uploads
         *            (false).
         */
        private Manager(boolean download) {
            this.download = download;
        }

        /**
         * Adds a new transfer item or updates an existing one (e.g., for progress).
         * Increments the active transfer count.
         *
         * @param item The transfer item to add or update.
         */
        private void add(Item item) {
            items.put(item.name + "@" + item.repository, item);

            if (count.getAndIncrement() == 0) {
                // start loading
            }
        }

        /**
         * Removes a transfer item upon completion or failure.
         * Decrements the active transfer count if the item was present.
         *
         * @param item The transfer item to remove.
         */
        private void remove(Item item) {
            items.remove(item.name + "@" + item.repository);

            if (count.decrementAndGet() == 0) {
                // stop loading
                last = 0;
            }
        }
    }

    /**
     * An immutable record representing the state of a single transfer operation.
     *
     * @param type The type of transfer (GET for download, PUT for upload).
     * @param name The identifier/name of the resource being transferred (often a path).
     * @param repository The ID of the repository involved.
     * @param size The total size of the resource in bytes (can be 0 or negative if unknown).
     * @param current The number of bytes transferred so far.
     */
    private record Item(RequestType type, String name, String repository, long size, long current) {

        /**
         * Constructs an Item from a Maven {@link TransferEvent}.
         *
         * @param e The event containing transfer details.
         */
        Item(TransferEvent e) {
            this(e.getRequestType(), e.getResource().getResourceName(), e.getResource().getRepositoryId(), e.getResource()
                    .getContentLength(), e.getTransferredBytes());
        }
    }
}