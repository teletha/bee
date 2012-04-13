/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.definition;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.metadata.MergeableMetadata;

/**
 * @version 2012/04/10 14:06:44
 */
abstract class MavenMetadata implements MergeableMetadata {

    private final File file;

    protected Metadata metadata;

    private boolean merged;

    /**
     * @param metadata
     * @param file
     */
    protected MavenMetadata(Metadata metadata, File file) {
        this.metadata = metadata;
        this.file = file;
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return "maven-metadata.xml";
    }

    /**
     * {@inheritDoc}
     */
    public File getFile() {
        return file;
    }

    /**
     * {@inheritDoc}
     */
    public void merge(File existing, File result) throws RepositoryException {
        Metadata recessive = read(existing);

        merge(recessive);

        write(result, metadata);

        merged = true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMerged() {
        return merged;
    }

    protected abstract void merge(Metadata recessive);

    /**
     * @param metadataFile
     * @return
     * @throws RepositoryException
     */
    private Metadata read(File metadataFile) throws RepositoryException {
        if (metadataFile.length() <= 0) {
            return new Metadata();
        }

        Reader reader = null;
        try {
            reader = ReaderFactory.newXmlReader(metadataFile);
            return new MetadataXpp3Reader().read(reader, false);
        } catch (IOException e) {
            throw new RepositoryException("Could not read metadata " + metadataFile + ": " + e.getMessage(), e);
        } catch (XmlPullParserException e) {
            throw new RepositoryException("Could not parse metadata " + metadataFile + ": " + e.getMessage(), e);
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * @param metadataFile
     * @param metadata
     * @throws RepositoryException
     */
    private void write(File metadataFile, Metadata metadata) throws RepositoryException {
        Writer writer = null;
        try {
            metadataFile.getParentFile().mkdirs();
            writer = WriterFactory.newXmlWriter(metadataFile);
            new MetadataXpp3Writer().write(writer, metadata);
        } catch (IOException e) {
            throw new RepositoryException("Could not write metadata " + metadataFile + ": " + e.getMessage(), e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);
        if (getGroupId().length() > 0) {
            buffer.append(getGroupId());
        }
        if (getArtifactId().length() > 0) {
            buffer.append(':').append(getArtifactId());
        }
        if (getVersion().length() > 0) {
            buffer.append(':').append(getVersion());
        }
        buffer.append('/').append(getType());
        return buffer.toString();
    }
}
