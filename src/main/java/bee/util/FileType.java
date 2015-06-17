/*
 * Copyright (C) 2015 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import java.nio.file.Path;
import java.util.List;

import bee.util.HeaderType.Batch;
import bee.util.HeaderType.Script;
import bee.util.HeaderType.Semicolon;
import bee.util.HeaderType.SlashStar;
import bee.util.HeaderType.TripleSlash;
import bee.util.HeaderType.Unknown;
import kiss.Extensible;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;

/**
 * @version 2015/06/17 11:12:33
 */
public interface FileType extends Extensible {

    /**
     * Retrieve the extension of this document.
     * 
     * @return
     */
    String extension();

    /**
     * Retrieve the header type of this document.
     * 
     * @return
     */
    HeaderType header();

    /**
     * Search {@link FileType} by extension.
     * 
     * @param extension A extension.
     * @return An associated {@link FileType}.
     */
    static FileType of(String extension) {
        List<FileType> types = I.find(FileType.class);

        for (FileType type : types) {
            if (type.extension().equalsIgnoreCase(extension)) {
                return type;
            }
        }
        return new File(extension, Unknown.class);
    }

    /**
     * Search {@link FileType} by extension.
     * 
     * @param path A path to file.
     * @return An associated {@link FileType}.
     */
    static FileType of(Path path) {
        return of(Paths.getExtension(path));
    }

    /**
     * @version 2015/06/17 10:49:59
     */
    @Manageable(lifestyle = Singleton.class)
    class File implements FileType {

        /** The extension. */
        private final String extension;

        /** The header type. */
        private final HeaderType header;

        /**
         * 
         */
        private File() {
            this("", Unknown.class);
        }

        /**
         * @param extension
         * @param header
         */
        private File(String extension, Class<? extends HeaderType> header) {
            this.extension = extension;
            this.header = I.make(header);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String extension() {
            return extension;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HeaderType header() {
            return header;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "File[" + extension + "]";
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Java extends File {

        /**
         * Hide constructor.
         */
        private Java() {
            super("java", SlashStar.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Groovy extends File {

        /**
         * Hide constructor.
         */
        private Groovy() {
            super("groovy", SlashStar.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Scala extends File {

        /**
         * Hide constructor.
         */
        private Scala() {
            super("scala", SlashStar.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Clojure extends File {

        /**
         * Hide constructor.
         */
        private Clojure() {
            super("clj", Semicolon.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class HTML extends File {

        /**
         * Hide constructor.
         */
        private HTML() {
            super("html", HeaderType.XML.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class XHTML extends File {

        /**
         * Hide constructor.
         */
        private XHTML() {
            super("xhtml", HeaderType.XML.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class CSS extends File {

        /**
         * Hide constructor.
         */
        private CSS() {
            super("css", SlashStar.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class JavaScript extends File {

        /**
         * Hide constructor.
         */
        private JavaScript() {
            super("js", SlashStar.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class TypeScript extends File {

        /**
         * Hide constructor.
         */
        private TypeScript() {
            super("ts", TripleSlash.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class C extends File {

        /**
         * Hide constructor.
         */
        private C() {
            super("c", SlashStar.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Cpp extends File {

        /**
         * Hide constructor.
         */
        private Cpp() {
            super("cpp", SlashStar.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class CSharp extends File {

        /**
         * Hide constructor.
         */
        private CSharp() {
            super("cs", SlashStar.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class XML extends File {

        /**
         * Hide constructor.
         */
        private XML() {
            super("xml", HeaderType.XML.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class XMLSchema extends File {

        /**
         * Hide constructor.
         */
        private XMLSchema() {
            super("xsd", HeaderType.XML.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class DocumentTypeDefinition extends File {

        /**
         * Hide constructor.
         */
        private DocumentTypeDefinition() {
            super("dtd", HeaderType.XML.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class POM extends File {

        /**
         * Hide constructor.
         */
        private POM() {
            super("pom", HeaderType.XML.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Perl extends File {

        /**
         * Hide constructor.
         */
        private Perl() {
            super("pl", Script.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class PHP extends File {

        /**
         * Hide constructor.
         */
        private PHP() {
            super("php", HeaderType.PHP.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Python extends File {

        /**
         * Hide constructor.
         */
        private Python() {
            super("py", Script.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Ruby extends File {

        /**
         * Hide constructor.
         */
        private Ruby() {
            super("rb", Script.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class Shell extends File {

        /**
         * Hide constructor.
         */
        private Shell() {
            super("sh", Script.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class WindowsBatch extends File {

        /**
         * Hide constructor.
         */
        private WindowsBatch() {
            super("bat", Batch.class);
        }
    }

    /**
     * @version 2015/06/17 10:49:10
     */
    class WindowsShell extends File {

        /**
         * Hide constructor.
         */
        private WindowsShell() {
            super("cmd", Batch.class);
        }
    }
}
