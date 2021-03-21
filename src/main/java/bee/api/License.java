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

public interface License {

    /**
     * Retrieve the license name.
     * 
     * @return
     */
    String name();

    /**
     * Retrieve the license full name.
     * 
     * @return
     */
    String fullName();

    /**
     * Retrieve the license identifier.
     * 
     * @return
     */
    String identifier();

    /**
     * Retrieve the license uri.
     * 
     * @return
     */
    String uri();

    /**
     * Write lisence text.
     * 
     * @return
     */
    default List<String> text() {
        String year = new SimpleDateFormat("yyyy").format(new Date());
        String producer = I.make(Project.class).getProducer();

        List<String> license = new ArrayList();
        license.add("Copyright (C) " + year + " " + producer);
        license.add("");
        license.add("Licensed under the " + name() + " License (the \"License\");");
        license.add("you may not use this file except in compliance with the License.");
        license.add("You may obtain a copy of the License at");
        license.add("");
        license.add("         " + uri());

        return license;
    }
}