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

import java.nio.file.Path;

import bee.api.Command;
import bee.api.Task;
import bee.util.RESTClient;
import kiss.I;

/**
 * @version 2017/01/11 16:20:19
 */
public class Gnupg extends Task {

    @Command("Install GnuPG.")
    public void download() {
        Path file = I.locate("gnupg.tar.gz2");

        RESTClient client = new RESTClient();
        client.get("https://www.gnupg.org/ftp/gcrypt/gnupg/gnupg-2.1.17.tar.bz2", file).to(b -> {
            System.out.println(b.toAbsolutePath());
        });
    }

}
