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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bee.api.Command;
import kiss.I;
import kiss.JSON;

/**
 * Generates the {@link bee.api.JavaVersion} enum from the Adoptium (Temurin) REST API. The list of
 * released versions, the Long Term Support flag and the initial GA release date are all taken from
 * Adoptium, so the enum stays in sync with the actually available JDKs.
 */
public interface JavaVersionTask extends bee.Task {

    /** The endpoint which lists the available releases. */
    String AVAILABLE = "https://api.adoptium.net/v3/info/available_releases";

    /**
     * Regenerate {@code src/main/java/bee/api/JavaVersion.java} from the Adoptium REST API.
     */
    @Command(defaults = true, value = "Generate the JavaVersion enum from the Adoptium API.")
    default void generate() {
        JSON info = I.json(AVAILABLE);

        List<Integer> releases = new ArrayList(info.find(int.class, "available_releases", "*"));
        releases.sort(Comparator.naturalOrder());
        List<Integer> lts = info.find(int.class, "available_lts_releases", "*");
        Map<Integer, String> dates = releaseDates(releases);

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
        lines.add(" * The released Java versions. Unlike {@link javax.lang.model.SourceVersion}, which can not describe a");
        lines.add(" * version newer than the JDK which compiles the project, this enum declares every Java version so a");
        lines.add(" * project may target a newer Java version than the one used by the IDE or the build. Each entry also");
        lines.add(" * carries the Long Term Support flag and the initial GA release date.");
        lines.add(" * <p>");
        lines.add(" * This file is generated from the Adoptium (Temurin) REST API by the {@code JavaVersionTask}. Do not edit");
        lines.add(" * it by hand, run {@code bee JavaVersionTask:generate} instead.");
        lines.add(" */");
        lines.add("public enum JavaVersion {");
        lines.add("");

        // The historical versions before Java 8 are not distributed by Adoptium, so they are fixed
        // here. The name uses the old 1.x form.
        List<int[]> historical = List.of(new int[] {1, 1996, 1, 23}, new int[] {2, 1997, 2, 19}, new int[] {3, 1998, 12, 8}, new int[] {
                4, 2000, 5, 8}, new int[] {5, 2004, 9, 30}, new int[] {6, 2006, 12, 11}, new int[] {7, 2011, 7, 28});
        for (int[] version : historical) {
            lines.add("    /** Java 1." + (version[0] - 1) + " */");
            lines.add("    JAVA_" + version[0] + "(" + version[0] + ", false, \"" + String.format("%04d-%02d-%02d", version[1], version[2], version[3]) + "\"),");
            lines.add("");
        }

        for (int i = 0; i < releases.size(); i++) {
            int version = releases.get(i);
            String date = dates.get(version);

            lines.add("    /** Java " + version + " */");
            lines.add("    JAVA_" + version + "(" + version + ", " + lts.contains(version) + ", \"" + date + "\")"
                    + (i == releases.size() - 1 ? ";" : ","));
            lines.add("");
        }

        lines.add("    /** The feature version. */");
        lines.add("    public final int feature;");
        lines.add("");
        lines.add("    /** Whether this is a Long Term Support release or not. */");
        lines.add("    public final boolean lts;");
        lines.add("");
        lines.add("    /** The initial GA release date. */");
        lines.add("    public final LocalDate release;");
        lines.add("");
        lines.add("    /**");
        lines.add("     * Java version definition.");
        lines.add("     * ");
        lines.add("     * @param feature A feature version.");
        lines.add("     * @param lts Whether this is a Long Term Support release.");
        lines.add("     * @param release An initial GA release date in the ISO format.");
        lines.add("     */");
        lines.add("    private JavaVersion(int feature, boolean lts, String release) {");
        lines.add("        this.feature = feature;");
        lines.add("        this.lts = lts;");
        lines.add("        this.release = LocalDate.parse(release);");
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
        lines.add("     * Resolve the version from a text such as <code>21</code>, <code>1.8</code> or <code>Java 21</code>.");
        lines.add("     * ");
        lines.add("     * @param text A version text.");
        lines.add("     * @return A Java version, or <code>null</code> when it can not be resolved.");
        lines.add("     */");
        lines.add("    public static JavaVersion parse(String text) {");
        lines.add("        if (text == null) {");
        lines.add("            return null;");
        lines.add("        }");
        lines.add("");
        lines.add("        String value = text.trim().replaceAll(\"(?i)java\\\\s*\", \"\").replace(\"1.\", \"\");");
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
        lines.add("}");

        makeFile("src/main/java/bee/api/JavaVersion.java", lines);
    }

    /**
     * Resolve the initial GA release date of each version.
     * 
     * @param releases A list of versions.
     * @return A map from version to release date.
     */
    private Map<Integer, String> releaseDates(List<Integer> releases) {
        Map<Integer, String> dates = new HashMap();

        I.signal(releases).flatMap(version -> I.http(releaseUrl(version), JSON.class).map(json -> I.pair(version, json)))
                .waitForTerminate()
                .skipError()
                .to(pair -> {
                    String timestamp = pair.ⅱ.get("0").text("timestamp");
                    dates.put(pair.ⅰ, timestamp != null && 10 <= timestamp.length() ? timestamp.substring(0, 10) : "");
                });

        return dates;
    }

    /**
     * Build the URL of the initial GA release of the specified version.
     * 
     * @param version A JDK version.
     * @return A release API URL.
     */
    private String releaseUrl(int version) {
        return "https://api.adoptium.net/v3/assets/feature_releases/" + version
                + "/ga?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux"
                + "&page=0&page_size=1&project=jdk&vendor=eclipse&sort_order=ASC";
    }
}
