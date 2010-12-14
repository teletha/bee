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

/**
 * @version 2010/09/28 23:42:32
 */
public class ProjectDescriptor {

    /** The group id. */
    public final String groupId;

    /** The artifact id. */
    public final String artifactId;

    /** The version number. */
    public final Version version;

    /** The project name. */
    private String name;

    /** The project description. */
    private String description;

    /**
     * @param groupId
     * @param artifactId
     * @param version
     */
    protected ProjectDescriptor(String groupId, String artifactId, Version version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

}
