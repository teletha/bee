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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version 2010/05/21 10:31:17
 */
public class Version {

    private static final Pattern REGEX = Pattern.compile("(\\d+)\\.(\\d+)(\\.(\\d+))?(-(.*))?");

    /** The major version number. */
    public final int major;

    /** The minor version number. */
    public final int minor;

    /** The incremental number. */
    public final int increment;

    /** The version identifier. */
    public final String identifier;

    /**
     * @param qualifier
     */
    public Version(String qualifier) {
        Matcher matcher = REGEX.matcher(qualifier);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid qualifier");
        }

        major = Integer.parseInt(matcher.group(1));
        minor = Integer.parseInt(matcher.group(2));
        increment = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));
        identifier = matcher.group(6) == null ? null : matcher.group(6);
    }

    /**
     * @param major
     * @param minor
     * @param increment
     * @param identifier
     */
    public Version(int major, int minor, int increment, String identifier) {
        if (major < 0) {
            throw new IllegalArgumentException("The major version number is negative.");
        }

        if (minor < 0) {
            throw new IllegalArgumentException("The minor version number is negative.");
        }

        this.major = major;
        this.minor = minor;
        this.increment = Math.max(0, increment);
        this.identifier = identifier;
    }

    /**
     * @param version
     */
    Version(ProjectVersion version) {
        this(version.major(), version.minor(), version.increment(), version.identifier());
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + increment;
        result = prime * result + major;
        result = prime * result + minor;
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
        Version other = (Version) obj;
        if (identifier == null) {
            if (other.identifier != null) return false;
        } else if (!identifier.equals(other.identifier)) return false;
        if (increment != other.increment) return false;
        if (major != other.major) return false;
        if (minor != other.minor) return false;
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(major).append('.').append(minor);

        if (increment != 0) {
            builder.append('.').append(increment);
        }

        if (identifier != null) {
            builder.append('-').append(identifier);
        }

        return builder.toString();
    }
}
