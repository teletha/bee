/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import kiss.I;

public class License {

    /** Builtin */
    public static final License AGPL = new License("AGPL", "3.0", "GNU Affero General Public License version 3", "https://opensource.org/licenses/AGPL-3.0");

    /** Builtin */
    public static final License Apache = new License("Apache", "2.0", "Apache License, Version $", "https://opensource.org/licenses/Apache-2.0");

    /** Builtin */
    public static final License BSD = new License("BSD", "3-Clause ", "The $ BSD License", "https://opensource.org/licenses/BSD-3-Clause");

    /** Builtin */
    public static final License EPL = new License("EPL", "1.0", "Eclipse Public License $", "https://opensource.org/licenses/EPL-1.0");

    /** Builtin */
    public static final License GPL = new License("GPL", "3.0", "GNU General Public License version 3", "https://opensource.org/licenses/GPL-3.0");

    /** Builtin */
    public static final License LGPL = new License("LGPL", "3.0", "GNU Lesser General Public License version $", "https://opensource.org/licenses/LGPL-3.0");

    /** Builtin */
    public static final License MIT = new License("MIT", "", "MIT License", "https://opensource.org/licenses/MIT");

    /** Builtin */
    public static final License MPL = new License("MPL", "2.0", "Mozilla Public License $", "https://opensource.org/licenses/MPL-2.0");

    /** The license name. */
    public final String name;

    /** The license identifier. */
    public final String id;

    /** The license full name. */
    public final String full;

    /** The license uri. */
    public final String uri;

    /**
     * Create {@link License}.
     * 
     * @param version
     * @param full
     * @param uri
     */
    public License(String name, String version, String full, String uri) {
        this.name = name;
        this.id = name + (version.isEmpty() ? "" : "-" + version);
        this.full = full.replace("$", version);
        this.uri = uri;
    }

    /**
     * Write lisence text.
     * 
     * @return
     */
    public List<String> text(boolean simple) {
        List<String> text = new ArrayList();
        Project project = I.make(Project.class);

        if (project.licensedBy.isEmpty()) {
            text.add("Copyright (C) " + Year.now().getValue() + " The " + project.getProduct().toUpperCase() + " Development Team");
        } else {
            for (int i = 0; i < project.licensedFrom.size(); i++) {
                int from = project.licensedFrom.get(i);
                int to = project.licensedTo.get(i);
                String by = project.licensedBy.get(i);
                if (by.isEmpty()) {
                    by = "The " + project.getProduct().toUpperCase() + " Development Team";
                }

                if (from == to) {
                    text.add("Copyright (C) " + from + " " + by);
                } else {
                    text.add("Copyright (C) " + from + "-" + to + " " + by);
                }
            }
        }

        if (simple) {
            text.add("");
            text.add("Licensed under the " + name + " License (the \"License\");");
            text.add("you may not use this file except in compliance with the License.");
            text.add("You may obtain a copy of the License at");
            text.add("");
            text.add("         " + uri);
        } else {
            String[] body = I.json("https://api.github.com/licenses/" + name).text("body").split("\\R");
            text.add("");
            text.add(full);
            text.addAll(I.list(body).subList(3, body.length));
        }

        return text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return id;
    }

    /**
     * List up all builtin {@link License}s.
     * 
     * @return
     */
    public static List<License> builtins() {
        return List.of(AGPL, Apache, BSD, EPL, GPL, LGPL, MIT, MPL);
    }
}