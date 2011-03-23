/*
 * Copyright (C) 2010 Nameless Production Committee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bee;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;

import javax.swing.UIManager;

/**
 * @version 2011/03/23 18:55:51
 */
public class Launcher {

    // initialization
    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            alert(e.getMessage());
        }
    }

    /**
     * <p>
     * Launch Bee.
     * </p>
     */
    public static final void main(String... args) {
        if (Files.exists(Platform.Bee)) {
            launch(args);
        } else {
            install();
        }
    }

    /**
     * <p>
     * Install Bee.
     * </p>
     */
    public static final void install() {
        System.out.println(Platform.Java);
    }

    /**
     * <p>
     * Launch Bee.
     * </p>
     */
    public static final void launch(String... command) {
        System.out.println(Platform.Bee);
    }

    /**
     * <p>
     * Show native message dialog.
     * </p>
     * 
     * @param message
     */
    public static final void alert(String message) {
        Dialog alert = new Dialog(new Frame(), "Bee");
        alert.setSize(message.length() * 6, 100);
        alert.add(new Label(message, Label.CENTER));
        alert.setLocationRelativeTo(null);
        alert.addWindowListener(new WindowAdapter() {

            /**
             * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
             */
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        alert.setVisible(true);
    }
}
