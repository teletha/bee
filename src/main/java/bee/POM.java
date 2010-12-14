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
package bee;

import ezbean.xml.Rule;
import ezbean.xml.XMLScanner;

/**
 * @version 2010/05/20 18:05:07
 */
public class POM extends XMLScanner {

    /** The group id. */
    private String groupId;

    /** The artifact id. */
    private String artifactId;

    /** The version number. */
    private String version;

    /** The project name. */
    private String name;

    /** The project description. */
    private String description;

    /**
     * Get the groupId property of this {@link POM}.
     * 
     * @return The groupId property.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Set the groupId property of this {@link POM}.
     * 
     * @param groupId The groupId value to set.
     */
    @Rule(match = "project/groupId")
    void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Get the artifactId property of this {@link POM}.
     * 
     * @return The artifactId property.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Set the artifactId property of this {@link POM}.
     * 
     * @param artifactId The artifactId value to set.
     */
    @Rule(match = "project/artifactId")
    void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Get the version property of this {@link POM}.
     * 
     * @return The version property.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version property of this {@link POM}.
     * 
     * @param version The version value to set.
     */
    @Rule(match = "project/version")
    void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the name property of this {@link POM}.
     * 
     * @return The name property.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name property of this {@link POM}.
     * 
     * @param name The name value to set.
     */
    @Rule(match = "project/name")
    void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description property of this {@link POM}.
     * 
     * @return The description property.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description property of this {@link POM}.
     * 
     * @param description The description value to set.
     */
    @Rule(match = "project/description")
    void setDescription(String description) {
        this.description = description;
    }

}
