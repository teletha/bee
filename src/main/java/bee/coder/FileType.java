/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.coder;

import java.util.List;

import kiss.Extensible;
import kiss.I;
import kiss.Managed;
import kiss.Singleton;

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
    HeaderStyle header();

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
        return new File(extension, StandardHeaderStyle.Unknown);
    }

    /**
     * Search {@link FileType} by extension.
     * 
     * @param file A target file.
     * @return An associated {@link FileType}.
     */
    static FileType of(psychopath.File file) {
        return of(file.extension());
    }

    /**
     * @version 2015/06/17 10:49:59
     */
    @Managed(value = Singleton.class)
    class File implements FileType {

        /** The extension. */
        private final String extension;

        /** The header type. */
        private final HeaderStyle header;

        /**
         * 
         */
        private File() {
            this("", StandardHeaderStyle.Unknown);
        }

        /**
         * @param extension
         * @param header
         */
        private File(String extension, HeaderStyle header) {
            this.extension = extension;
            this.header = header;
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
        public HeaderStyle header() {
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
            super("java", StandardHeaderStyle.SlashStar);
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
            super("groovy", StandardHeaderStyle.SlashStar);
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
            super("scala", StandardHeaderStyle.SlashStar);
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
            super("clj", StandardHeaderStyle.Semicolon);
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
            super("html", StandardHeaderStyle.XML);
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
            super("xhtml", StandardHeaderStyle.XML);
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
            super("css", StandardHeaderStyle.SlashStar);
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
            super("js", StandardHeaderStyle.SlashStar);
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
            super("ts", StandardHeaderStyle.SlashTriple);
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
            super("c", StandardHeaderStyle.SlashStar);
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
            super("cpp", StandardHeaderStyle.SlashStar);
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
            super("cs", StandardHeaderStyle.SlashStar);
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
            super("xml", StandardHeaderStyle.XML);
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
            super("xsd", StandardHeaderStyle.XML);
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
            super("dtd", StandardHeaderStyle.XML);
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
            super("pom", StandardHeaderStyle.XML);
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
            super("pl", StandardHeaderStyle.Sharp);
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
            super("py", StandardHeaderStyle.Sharp);
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
            super("rb", StandardHeaderStyle.Sharp);
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
            super("sh", StandardHeaderStyle.Sharp);
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
            super("bat", StandardHeaderStyle.Batch);
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
            super("cmd", StandardHeaderStyle.Batch);
        }
    }
}