/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee.definition;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import bee.repository.Repository;

/**
 * @version 2010/09/09 20:10:56
 */
public class Library implements Comparable<Library> {

    /** The group name. */
    public final String group;

    /** The artifact name. */
    public final String name;

    /** The version identifier. */
    public final Version version;

    /**
     * @param name
     * @param group
     * @param version
     */
    Library(String qualified) {
        String[] values = qualified.split(":");
        this.group = values[0];
        this.name = values[1];
        this.version = new Version(values[2]);
    }

    /**
     * @param group
     * @param artifact
     * @param version
     */
    Library(String group, String artifact, String version) {
        this.group = group;
        this.name = artifact;
        this.version = new Version(version);
    }

    /**
     * <p>
     * This library is needed at compile phase.
     * </p>
     * 
     * @return
     */
    public Library atCompile() {
        return this;
    }

    /**
     * <p>
     * This library is needed at compile phase.
     * </p>
     * 
     * @return
     */
    public Library atTest() {
        return this;
    }

    /**
     * Load library from the specified repositories.
     * 
     * @param repositories
     * @return
     */
    public Path load(List<Repository> repositories) {
        return null;
    }

    /**
     * <p>
     * Locate jar file in your internal repository.
     * </p>
     * 
     * @return A location.
     */
    Path toInternal(String extension) {
        return null;
    }

    /**
     * <p>
     * Locate jar file in the central repository.
     * </p>
     * 
     * @return A location.
     */
    URL toExternal(String extension) {
        return null;
    }

    /**
     * <p>
     * Locate jar file in the specified external repository.
     * </p>
     * 
     * @param repository A target repository.
     * @return A location.
     */
    URL toExternal(String extension, Repository repository) {
        // try {
        // return new URL(repository.url, toPath(extension));
        // } catch (MalformedURLException e) {
        // // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // // wrapped error in here.
        // throw new Error(e);
        // }
        return null;
    }

    /**
     * <p>
     * Helper method to build path string.
     * </p>
     * 
     * @param extension An extension value.
     * @return A path string.
     */
    public Path toLocalPath(String extension) {
        StringBuilder builder = new StringBuilder();
        builder.append(group).append('/');
        builder.append(name).append('/');
        builder.append(version).append('/');
        builder.append(name).append('-').append(version).append(extension);

        return Repository.Local.path.resolve(builder.toString());
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Library other = (Library) obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (group == null) {
            if (other.group != null) return false;
        } else if (!group.equals(other.group)) return false;
        if (version == null) {
            if (other.version != null) return false;
        } else if (!version.equals(other.version)) return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Library o) {
        return toString().compareTo(o.toString());
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append('-').append(version);

        return builder.toString();
    }
}
