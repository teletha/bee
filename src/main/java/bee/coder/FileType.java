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

public interface FileType extends Extensible {

    FileType C = new File("c", StandardHeaderStyle.SlashStar);

    FileType Clojure = new File("clj", StandardHeaderStyle.Semicolon);

    FileType Cpp = new File("cpp", StandardHeaderStyle.SlashStar);

    FileType CSharp = new File("cs", StandardHeaderStyle.SlashStar);

    FileType CSS = new File("css", StandardHeaderStyle.SlashStar);

    FileType DocumentTypeDefinition = new File("dtd", StandardHeaderStyle.XML);

    FileType Go = new File("go", StandardHeaderStyle.SlashStar);

    FileType Groovy = new File("groovy", StandardHeaderStyle.SlashStar);

    FileType HTML = new File("html", StandardHeaderStyle.XML);

    FileType Java = new File("java", StandardHeaderStyle.SlashStar);

    FileType JavaScript = new File("js", StandardHeaderStyle.SlashStar);

    FileType Kotlin = new File("kt", StandardHeaderStyle.SlashStar);

    FileType Perl = new File("pl", StandardHeaderStyle.Sharp);

    FileType POM = new File("pom", StandardHeaderStyle.XML);

    FileType Ruby = new File("rb", StandardHeaderStyle.Sharp);

    FileType Rust = new File("rs", StandardHeaderStyle.SlashStar);

    FileType Python = new File("py", StandardHeaderStyle.Sharp);

    FileType Scala = new File("scala", StandardHeaderStyle.SlashStar);

    FileType Shell = new File("sh", StandardHeaderStyle.Sharp);

    FileType TypeScript = new File("ts", StandardHeaderStyle.SlashTriple);

    FileType WindowsBatch = new File("bat", StandardHeaderStyle.Batch);

    FileType WindowsShell = new File("cmd", StandardHeaderStyle.Batch);

    FileType XML = new File("xml", StandardHeaderStyle.XML);

    FileType XMLSchema = new File("xsd", StandardHeaderStyle.XML);

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

        FileType builtin = File.BUILTIN.get(extension);
        if (builtin != null) {
            return builtin;
        }

        return new File(extension, StandardHeaderStyle.Unknown, false);
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
}