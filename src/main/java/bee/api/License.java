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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kiss.I;

public class License {

    /** Builtin */
    public static final License Apache = new License("Apache", "2.0", "Apache License, Version $", "https://opensource.org/licenses/Apache-2.0");

    /** Builtin */
    public static final License BSD = new License("BSD", "3-Clause ", "The $ BSD License", "https://opensource.org/licenses/BSD-2-Clause");

    /** Builtin */
    public static final License EPL = new License("EPL", "1.0", "Eclipse Public License $", "https://opensource.org/licenses/EPL-1.0");

    /** Builtin */
    public static final License GPL = new License("GPL", "3.0", "GNU General Public License version 3", "https://opensource.org/licenses/GPL-3.0");

    /** Builtin */
    public static final License LGPL = new License("LGPL", "3.0", "GNU Lesser General Public License version $", "https://opensource.org/licenses/LGPL-3.0");

    /** Builtin */
    public static final License MIT = new License("MIT", "", "The MIT License", "https://opensource.org/licenses/MIT");

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
    public List<String> text() {
        String year = new SimpleDateFormat("yyyy").format(new Date());
        String producer = I.make(Project.class).getProducer();

        List<String> license = new ArrayList();
        license.add("Copyright (C) " + year + " " + producer);
        license.add("");
        license.add("Licensed under the " + name + " License (the \"License\");");
        license.add("you may not use this file except in compliance with the License.");
        license.add("You may obtain a copy of the License at");
        license.add("");
        license.add("         " + uri);

        return license;
    }

    /**
     * List up all builtin {@link License}s.
     * 
     * @return
     */
    public static List<License> builtins() {
        return List.of(Apache, BSD, EPL, GPL, LGPL, MIT, MPL);
    }
}