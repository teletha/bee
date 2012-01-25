/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package testament;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.zip.CRC32;

import kiss.I;

/**
 * <p>
 * Support for simultaneous operations of Path.
 * </p>
 * 
 * @version 2011/03/09 8:23:57
 */
public class Synchrotron {

    /** The one. */
    private Path one;

    /** The other. */
    private Path other;

    /**
     * @param one
     * @param other
     */
    public Synchrotron(Path one, Path other) {
        this.one = one;
        this.other = other;
    }

    /**
     * <p>
     * Resolve path by name.
     * </p>
     * 
     * @param path A file path.
     * @return A self instance to chain API.
     */
    public Synchrotron sibling(String path) {
        one = one.resolveSibling(path);
        other = other.resolveSibling(path);

        // API definition
        return this;
    }

    /**
     * <p>
     * Resolve path by name.
     * </p>
     * 
     * @param path A file path.
     * @return A self instance to chain API.
     */
    public Synchrotron child(String path) {
        one = one.resolve(path);
        other = other.resolve(path);

        // API definition
        return this;
    }

    public Synchrotron exists(boolean one, boolean other) {
        if (one) {
            if (Files.notExists(this.one)) {
                fail("'" + this.one + "' must exist.");
            }
        } else {
            if (Files.exists(this.one)) {
                fail("'" + this.one + "' must not exist.");
            }
        }

        if (other) {
            if (Files.notExists(this.other)) {
                fail("'" + this.other + "' must exist.");
            }
        } else {
            if (Files.exists(this.other)) {
                fail("'" + this.other + "' must not exist.");
            }
        }

        // API definition
        return this;
    }

    public Synchrotron areSameDirectory() {
        assertPath(one, other, true, false, true);

        // API definition
        return this;
    }

    public Synchrotron areNotSameDirectory() {
        assertPath(one, other, true, false, false);

        // API definition
        return this;
    }

    public Synchrotron areSameFile() {
        assertPath(one, other, true, true, true);

        // API definition
        return this;
    }

    public Synchrotron areNotSameFile() {
        assertPath(one, other, true, true, false);

        // API definition
        return this;
    }

    /**
     * <p>
     * Helper method to check various file attributes.
     * </p>
     * 
     * @param one
     * @param other
     */
    private void assertPath(Path one, Path other, boolean exist, boolean file, boolean same) {
        try {
            if (!exist) {
                if (!Files.notExists(one)) {
                    fail("'" + one + "' must not exist.");
                }

                if (!Files.notExists(other)) {
                    fail("'" + other + "' must not exist.");
                }
                return; // end
            } else {
                if (!Files.exists(one)) {
                    fail("'" + one + "' must exist.");
                }

                if (!Files.exists(other)) {
                    fail("'" + other + "' must exist.");
                }
            }

            BasicFileAttributes oneAttributes = Files.readAttributes(one, BasicFileAttributes.class);
            BasicFileAttributes otherAttributes = Files.readAttributes(other, BasicFileAttributes.class);

            if (file) {
                if (!oneAttributes.isRegularFile()) {
                    fail("'" + one + "' must be file.");
                }
            } else {
                if (!oneAttributes.isDirectory()) {
                    fail("'" + one + "' must be directory.");
                }
            }

            // FileTime
            // FileTime oneTime = oneAttributes.creationTime();
            // FileTime otherTime = otherAttributes.creationTime();
            //
            // if (!oneTime.equals(otherTime)) {
            // fail("CreationTime: '" + one + "' is " + oneTime + ", but '" + other + "' is " +
            // otherTime + ".");
            // }
            //
            // oneTime = oneAttributes.lastAccessTime();
            // otherTime = otherAttributes.lastAccessTime();
            //
            // if (!oneTime.equals(otherTime)) {
            // fail("LastAccessTime: '" + one + "' is " + oneTime + ", but '" + other + "' is " +
            // otherTime + ".");
            // }

            FileTime oneTime = oneAttributes.lastModifiedTime();
            FileTime otherTime = otherAttributes.lastModifiedTime();

            if (!oneTime.equals(otherTime)) {
                fail("LastModifiedTime: '" + one + "' is " + oneTime + ", but '" + other + "' is " + otherTime + ".");
            }

            // Directory
            if (oneAttributes.isDirectory()) {
                if (!otherAttributes.isDirectory()) {
                    fail("'" + one + "' is directory, but '" + other + "' is not directory.");
                }
            } else {
                if (otherAttributes.isDirectory()) {
                    fail("'" + one + "' is not directory, but '" + other + "' is directory.");
                }
            }

            // File
            if (oneAttributes.isRegularFile()) {
                if (!otherAttributes.isRegularFile()) {
                    fail("'" + one + "' is file, but '" + other + "' is not file.");
                }

                if (oneAttributes.size() != otherAttributes.size()) {
                    fail("FileSize: '" + one + "' is " + oneAttributes.size() + ", but '" + other + "' is " + otherAttributes.size() + ".");
                }

                CRC32 oneHash = new CRC32();
                CRC32 otherHash = new CRC32();

                oneHash.update(Files.readAllBytes(one));
                otherHash.update(Files.readAllBytes(other));

                if (oneHash.getValue() != otherHash.getValue()) {
                    fail("'" + one + "' is different from '" + other + "'.");
                }
            } else {
                if (otherAttributes.isRegularFile()) {
                    fail("'" + one + "' is not file, but '" + other + "' is file.");
                }
            }

            // SymbolicLink
            if (oneAttributes.isSymbolicLink()) {
                if (!otherAttributes.isSymbolicLink()) {
                    fail("'" + one + "' is symbolic link, but '" + other + "' is not symbolic link.");
                }
            } else {
                if (otherAttributes.isSymbolicLink()) {
                    fail("'" + one + "' is not symbolic link, but '" + other + "' is symbolic link.");
                }
            }

            // Unknown
            if (oneAttributes.isOther()) {
                if (!otherAttributes.isOther()) {
                    fail("'" + one + "' is unknown, but '" + other + "' is not unknown.");
                }
            } else {
                if (otherAttributes.isOther()) {
                    fail("'" + one + "' is not unknown, but '" + other + "' is unknown.");
                }
            }
        } catch (IOException e) {
            throw I.quiet(e);
        } catch (AssertionError e) {
            if (same) {
                throw e;
            }
            return;
        }

        if (!same) {
            fail("'" + one + "' is equal to '" + other + "'.");
        }
    }
}
