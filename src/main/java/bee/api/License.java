/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kiss.I;

/**
 * @version 2015/06/16 12:11:16
 */
public interface License {

    /**
     * <p>
     * Retrieve the license name.
     * </p>
     * 
     * @return
     */
    String name();

    /**
     * <p>
     * Retrieve the license uri.
     * </p>
     * 
     * @return
     */
    String uri();

    /**
     * <p>
     * Write lisence text.
     * </p>
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
