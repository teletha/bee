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

import org.apache.maven.model.building.ModelCache;
import org.sonatype.aether.RepositoryCache;
import org.sonatype.aether.RepositorySystemSession;

/**
 * @version 2012/03/25 10:59:26
 */
class MavenModelCache implements ModelCache {

    private final RepositorySystemSession session;

    private final RepositoryCache cache;

    /**
     * @param session
     * @return
     */
    static ModelCache newInstance(RepositorySystemSession session) {
        if (session.getCache() == null) {
            return null;
        } else {
            return new MavenModelCache(session);
        }
    }

    /**
     * @param session
     */
    private MavenModelCache(RepositorySystemSession session) {
        this.session = session;
        this.cache = session.getCache();
    }

    /**
     * {@inheritDoc}
     */
    public Object get(String groupId, String artifactId, String version, String tag) {
        return cache.get(session, new Key(groupId, artifactId, version, tag));
    }

    /**
     * {@inheritDoc}
     */
    public void put(String groupId, String artifactId, String version, String tag, Object data) {
        cache.put(session, new Key(groupId, artifactId, version, tag), data);
    }

    /**
     * @version 2012/03/25 20:31:29
     */
    private static final class Key {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String tag;

        private final int hash;

        /**
         * @param groupId
         * @param artifactId
         * @param version
         * @param tag
         */
        public Key(String groupId, String artifactId, String version, String tag) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.tag = tag;

            int h = 17;
            h = h * 31 + this.groupId.hashCode();
            h = h * 31 + this.artifactId.hashCode();
            h = h * 31 + this.version.hashCode();
            h = h * 31 + this.tag.hashCode();
            hash = h;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (null == obj || !getClass().equals(obj.getClass())) {
                return false;
            }

            Key that = (Key) obj;
            return artifactId.equals(that.artifactId) && groupId.equals(that.groupId) && version.equals(that.version) && tag.equals(that.tag);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return hash;
        }
    }
}
