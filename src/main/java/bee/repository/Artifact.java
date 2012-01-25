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
package bee.repository;

import java.io.File;

import bee.Version;

/**
 * @version 2010/09/19 13:38:36
 */
public class Artifact {

    /** The artifact name. */
    public final String artifact;

    /** The group name. */
    public final String group;

    /** The version identifier. */
    public final Version version;

    /**
     * @param artifact
     * @param group
     * @param version
     */
    public Artifact(String group, String artifact, String version) {
        this.group = group;
        this.artifact = artifact;
        this.version = new Version(version);
        
        
    }

    public File locate() throws NoSuchArtifactException {
        donwload();
        
        return new File(Repository.getLocation(), toPath(".jar"));
    }

    public File locateProjectDescriptor() throws NoSuchArtifactException {
        donwload();
        return new File(Repository.getLocation(), toPath(".pom"));
    }

    public File locateSource() throws NoSuchArtifactException {
        donwload();
        return new File(Repository.getLocation(), toPath("-sources.jar"));
    }

    private void donwload() throws NoSuchArtifactException {

    }

    /**
     * <p>
     * Locate jar file in your internal repository.
     * </p>
     * 
     * @return A location.
     */
    File toInternal(String extension) {
        return new File(Repository.getLocation(), toPath(extension));
    }

    /**
     * <p>
     * Helper method to build path string.
     * </p>
     * 
     * @param extension An extension value.
     * @return A path string.
     */
    String toPath(String extension) {
        StringBuilder builder = new StringBuilder();
        builder.append(group.replace('.', '/')).append('/');
        builder.append(artifact).append('/');
        builder.append(version).append('/');
        builder.append(artifact).append('-').append(version).append(extension);

        return builder.toString();
    }
}
