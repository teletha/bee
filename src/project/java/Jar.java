import kiss.I;

/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */

/**
 * @version 2012/04/12 0:08:30
 */
public class Jar extends bee.task.Jar {

    /**
     * {@inheritDoc}
     */
    @Override
    public void source() {
        for (int i = 0; i < 20; i++) {
            System.out.println(i);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }
        }

        System.out.println(ui.ask("test"));
    }
}
