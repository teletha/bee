/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import static bee.Platform.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import bee.api.Command;
import bee.api.Task;
import bee.util.RESTClient;
import kiss.I;

/**
 * @version 2017/01/10 12:21:48
 */
public class Git extends Task {

    @Command("Generate .gitignore file.")
    public void gitignore() {
        StringJoiner uri = new StringJoiner(",", "https://www.gitignore.io/api/", "");
        uri.add("Java");
        uri.add("Maven");

        // OS
        if (isWindows()) uri.add("Windows");
        if (isLinux()) uri.add("Linux");

        // IDE
        for (IDESupport ide : I.find(IDESupport.class)) {
            if (ide.exist(project)) {
                uri.add(ide.name());
            }
        }

        new RESTClient().get(uri.toString()).map(rule -> ".*" + EOL + "!/.gitignore" + EOL + rule).to(rule -> {
            makeFile(project.getRoot().resolve(".gitignore"), rule);
        });
    }

    List<String> update(List<String> lines) {
        List<String> updated = new ArrayList();

        return lines;
    }
}
