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

import static bee.Bee.*;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import kiss.I;
import kiss.model.ClassUtil;
import bee.util.JarArchiver;

/**
 * @version 2011/03/23 18:55:51
 */
public class BeeInstaller {

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
        try {
            Path bee = JavaHome.resolve("lib/bee.jar");
            Path current = ClassUtil.getArchive(BeeInstaller.class);

            if (Files.isDirectory(current)) {
                // The current directory is class files store.
                // We should pack them as jar file.
                // This process is mainly used by Bee developers.
                JarArchiver jar = new JarArchiver();
                jar.add(current);

                for (String location : System.getProperty("java.class.path").split(File.pathSeparator)) {
                    Path path = I.locate(location);

                    if (Files.isRegularFile(path) && !path.startsWith(JavaHome)) {
                        jar.add(path);
                    }
                }
                jar.pack(bee);
            } else if (Files.getLastModifiedTime(bee).toMillis() != Files.getLastModifiedTime(current).toMillis()) {
                // The current bee.jar is newer.
                // We should copy it to JDK directory.
                // This process is mainly used by Bee users while install phase.
                I.copy(current, bee);
            }

            // create bat file
            List<String> bat = new ArrayList();

            if (Bee.getFileName().toString().endsWith(".bat")) {
                // windows
                bat.add("@echo off");
                bat.add("java -cp \"" + bee.toString() + "\" " + Bee.class.getName() + " %*");
            } else {
                // linux
                // TODO
            }
            Files.write(Bee, bat, I.$encoding);
        } catch (Exception e) {
            throw I.quiet(e);
        }
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
