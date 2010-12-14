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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import bee.ui.UserInterface;
import ezbean.I;
import ezbean.model.Codec;
import ezbean.model.Model;

/**
 * @version 2010/11/23 23:24:52
 */
public class CommandlineUserInterface implements UserInterface {

    /**
     * @see bee.ui.UserInterface#ask(java.lang.String)
     */
    @Override
    public String ask(String message) {
        System.out.print(message.concat(" : "));

        try {
            String value = new BufferedReader(new InputStreamReader(System.in)).readLine();

            // Normalize to avoid NPE.
            if (value == null) {
                value = "";
            }

            // Remove whitespaces.
            value = value.trim();

            // Validate user input.
            if (value.length() == 0) {
                talk("Your input is empty, plese retry.");

                // Retry!
                return ask(message);
            }

            // API definition
            return value;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see bee.ui.UserInterface#ask(java.lang.String, java.lang.Object)
     */
    @Override
    public <T> T ask(String message, T defaultAnswer) {
        System.out.print(message + " [" + defaultAnswer + "] : ");

        try {
            String value = new BufferedReader(new InputStreamReader(System.in)).readLine();

            // Normalize to avoid NPE.
            if (value == null) {
                value = "";
            }

            // Remove whitespaces.
            value = value.trim();

            // API definition
            Codec<T> codec = I.find(Codec.class, defaultAnswer.getClass());

            if (codec == null) {
                codec = (Codec<T>) Model.load(defaultAnswer.getClass()).getCodec();
            }

            return value.length() == 0 ? defaultAnswer : codec.decode(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see bee.ui.UserInterface#talk(String, Object...)
     */
    @Override
    public void talk(String message, Object... params) {
        System.out.format(message.concat("%n"), params);
    }
}
