/*
 * Copyright (C) 2026 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee;

import static bee.TaskOperations.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import bee.api.Command;
import kiss.I;
import kiss.JSON;

/**
 * Generates the {@link bee.api.JavaVersion} enum from the Adoptium (Temurin) REST API. The list of
 * released versions, the Long Term Support flag, the initial GA release date and the Early Access
 * availability are all taken from Adoptium, so the enum stays in sync with the downloadable JDKs.
 * Only Java 11 and later are listed because Adoptium does not provide the older versions anymore.
 */
public interface JavaVersionTask extends bee.Task {

    /** The endpoint which lists the available releases. */
    String AVAILABLE = "https://api.adoptium.net/v3/info/available_releases";

    /** The minimum Java version to list. */
    int MINIMUM = 11;

    /**
     * Regenerate {@code src/main/java/bee/api/JavaVersion.java} from the Adoptium REST API.
     */
    @Command(defaults = true, value = "Generate the JavaVersion enum from the Adoptium API.")
    default void generate() {
        JSON info = I.json(AVAILABLE);

        List<Integer> ga = info.find(int.class, "available_releases", "*").stream().filter(v -> MINIMUM <= v).toList();
        List<Integer> lts = info.find(int.class, "available_lts_releases", "*");

        // The tip version is not released yet, so it is only available as an Early Access build.
        int tip = info.get(int.class, "tip_version");

        // Collect every version to list, including the tip when it is not released yet.
        TreeSet<Integer> versions = new TreeSet(ga);
        versions.add(tip);

        List<String> lines = new ArrayList();
        lines.add("/*");
        lines.add(" * Copyright (C) 2026 The BEE Development Team");
        lines.add(" *");
        lines.add(" * Licensed under the MIT License (the \"License\");");
        lines.add(" * you may not use this file except in compliance with the License.");
        lines.add(" * You may obtain a copy of the License at");
        lines.add(" *");
        lines.add(" *          https://opensource.org/licenses/MIT");
        lines.add(" */");
        lines.add("package bee.api;");
        lines.add("");
        lines.add("import java.time.LocalDate;");
        lines.add("import java.time.format.DateTimeFormatter;");
        lines.add("import java.util.Arrays;");
        lines.add("");
        lines.add("/**");
        lines.add(" * The Java versions known by Bee. Unlike {@link javax.lang.model.SourceVersion}, which can not describe");
        lines.add(" * a version newer than the JDK which compiles the project, this enum declares every Java version so a");
        lines.add(" * project may target a newer Java version than the one used by the IDE or the build. Each entry carries");
        lines.add(" * the Long Term Support flag, the release date and whether the version is only available as an Early");
        lines.add(" * Access build.");
        lines.add(" * <p>");
        lines.add(" * This file is generated from the Adoptium (Temurin) REST API by the {@code JavaVersionTask}. Do not edit");
        lines.add(" * it by hand, run {@code bee JavaVersionTask:generate} instead.");
        lines.add(" */");
        lines.add("public enum JavaVersion {");
        lines.add("");

        List<Integer> ordered = new ArrayList(versions);
        for (int i = 0; i < ordered.size(); i++) {
            int version = ordered.get(i);
            boolean earlyAccess = !ga.contains(version);
            String date = releaseDate(version, earlyAccess);
            String comment = "Java " + version + (earlyAccess ? " (Early Access)" : "");

            lines.add("    /** " + comment + " */");
            lines.add("    JAVA_" + version + "(" + version + ", " + lts.contains(version) + ", \"" + date + "\", " + earlyAccess + ")"
                    + (i == ordered.size() - 1 ? ";" : ","));
            lines.add("");
        }

        lines.add("    /** The feature version. */");
        lines.add("    public final int feature;");
        lines.add("");
        lines.add("    /** Whether this is a Long Term Support release or not. */");
        lines.add("    public final boolean lts;");
        lines.add("");
        lines.add("    /** The initial GA release date, or the first Early Access build date. */");
        lines.add("    public final LocalDate release;");
        lines.add("");
        lines.add("    /** Whether this version is only available as an Early Access build or not. */");
        lines.add("    public final boolean earlyAccess;");
        lines.add("");
        lines.add("    /**");
        lines.add("     * Java version definition.");
        lines.add("     * ");
        lines.add("     * @param feature A feature version.");
        lines.add("     * @param lts Whether this is a Long Term Support release.");
        lines.add("     * @param release A release date in the ISO format.");
        lines.add("     * @param earlyAccess Whether this version is only available as an Early Access build.");
        lines.add("     */");
        lines.add("    private JavaVersion(int feature, boolean lts, String release, boolean earlyAccess) {");
        lines.add("        this.feature = feature;");
        lines.add("        this.lts = lts;");
        lines.add("        this.release = LocalDate.parse(release);");
        lines.add("        this.earlyAccess = earlyAccess;");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * Format the release date as <code>yyyy/MM/dd</code>.");
        lines.add("     * ");
        lines.add("     * @return A formatted release date.");
        lines.add("     */");
        lines.add("    public String getReleaseDate() {");
        lines.add("        return release.format(DateTimeFormatter.ofPattern(\"yyyy/MM/dd\"));");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * The display name such as <code>Java 21</code>.");
        lines.add("     * ");
        lines.add("     * @return A display name.");
        lines.add("     */");
        lines.add("    public String getDisplayName() {");
        lines.add("        return \"Java \" + feature;");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * The Adoptium release type of this version, either <code>ga</code> or <code>ea</code>.");
        lines.add("     * ");
        lines.add("     * @return A release type.");
        lines.add("     */");
        lines.add("    public String getReleaseType() {");
        lines.add("        return earlyAccess ? \"ea\" : \"ga\";");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * Resolve the version from a feature version.");
        lines.add("     * ");
        lines.add("     * @param feature A feature version.");
        lines.add("     * @return A Java version, or <code>null</code> when it is unknown.");
        lines.add("     */");
        lines.add("    public static JavaVersion of(int feature) {");
        lines.add("        for (JavaVersion version : values()) {");
        lines.add("            if (version.feature == feature) {");
        lines.add("                return version;");
        lines.add("            }");
        lines.add("        }");
        lines.add("        return null;");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * Resolve the version from a text such as <code>21</code> or <code>Java 25</code>.");
        lines.add("     * ");
        lines.add("     * @param text A version text.");
        lines.add("     * @return A Java version, or <code>null</code> when it can not be resolved.");
        lines.add("     */");
        lines.add("    public static JavaVersion parse(String text) {");
        lines.add("        if (text == null) {");
        lines.add("            return null;");
        lines.add("        }");
        lines.add("");
        lines.add("        String value = text.trim().replaceAll(\"(?i)java\\\\s*\", \"\").replaceAll(\"(?i)-?(ea|ga)$\", \"\");");
        lines.add("        try {");
        lines.add("            return of(Integer.parseInt(value));");
        lines.add("        } catch (NumberFormatException e) {");
        lines.add("            return null;");
        lines.add("        }");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * The latest Java version known by Bee.");
        lines.add("     * ");
        lines.add("     * @return A latest Java version.");
        lines.add("     */");
        lines.add("    public static JavaVersion latest() {");
        lines.add("        return values()[values().length - 1];");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * The Java version of the JDK which is running this process.");
        lines.add("     * ");
        lines.add("     * @return A current Java version.");
        lines.add("     */");
        lines.add("    public static JavaVersion current() {");
        lines.add("        return of(Runtime.version().feature());");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * All the Long Term Support versions.");
        lines.add("     * ");
        lines.add("     * @return A list of LTS versions.");
        lines.add("     */");
        lines.add("    public static JavaVersion[] lts() {");
        lines.add("        return Arrays.stream(values()).filter(version -> version.lts).toArray(JavaVersion[]::new);");
        lines.add("    }");
        lines.add("");
        lines.add("    /**");
        lines.add("     * All the Early Access versions.");
        lines.add("     * ");
        lines.add("     * @return A list of Early Access versions.");
        lines.add("     */");
        lines.add("    public static JavaVersion[] earlyAccess() {");
        lines.add("        return Arrays.stream(values()).filter(version -> version.earlyAccess).toArray(JavaVersion[]::new);");
        lines.add("    }");
        lines.add("}");

        makeFile("src/main/java/bee/api/JavaVersion.java", lines);
    }

    /**
     * Resolve the release date of the specified version. For a GA version this is the first GA release
     * date, and for an Early Access version the first Early Access build date.
     * 
     * @param version A JDK version.
     * @param earlyAccess Whether the version is only available as an Early Access build.
     * @return A release date in the ISO format.
     */
    private String releaseDate(int version, boolean earlyAccess) {
        String timestamp = firstRelease(version, earlyAccess ? "ea" : "ga");
        return timestamp != null && 10 <= timestamp.length() ? timestamp.substring(0, 10) : "";
    }

    /**
     * Fetch the timestamp of the earliest release of the specified version and type.
     * 
     * @param version A JDK version.
     * @param type A release type, either <code>ga</code> or <code>ea</code>.
     * @return A timestamp, or <code>null</code>.
     */
    private String firstRelease(int version, String type) {
        try {
            JSON releases = I.json(releaseUrl(version, type));
            return releases.get("0").text("timestamp");
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Build the URL of the earliest release of the specified version and type.
     * 
     * @param version A JDK version.
     * @param type A release type, either <code>ga</code> or <code>ea</code>.
     * @return A release API URL.
     */
    private String releaseUrl(int version, String type) {
        return "https://api.adoptium.net/v3/assets/feature_releases/" + version + "/" + type
                + "?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux"
                + "&page=0&page_size=1&project=jdk&vendor=eclipse&sort_order=ASC";
    }
}
