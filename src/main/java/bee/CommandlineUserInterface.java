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
import java.lang.reflect.Field;

import ezbean.I;
import ezbean.model.ClassUtil;
import ezbean.model.Codec;
import ezbean.model.Model;
import ezbean.model.Property;

/**
 * @version 2010/11/23 23:24:52
 */
class CommandlineUserInterface implements UserInterface {

    /**
     * @see bee.UserInterface#ask(java.lang.String)
     */
    @Override
    public String ask(String message) {
        System.out.print(message.concat(" : "));

        try {
            String value = new BufferedReader(new InputStreamReader(System.in)).readLine();

            // Remove whitespaces.
            value = value == null ? "" : value.trim();

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
     * @see bee.UserInterface#ask(java.lang.String, java.lang.Object)
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
     * @see bee.UserInterface#ask(java.lang.String, bee.Validator)
     */
    @Override
    public <T> T ask(String message, Validator<T> validator) {
        System.out.print(message.concat(" : "));

        try {
            String value = new BufferedReader(new InputStreamReader(System.in)).readLine();

            // Remove whitespaces.
            value = value == null ? "" : value.trim();

            // Validate user input.
            if (value.length() == 0) {
                talk("Your input is empty, plese retry.");

                // Retry!
                return ask(message, validator);
            }

            // Convert user input.
            T input = (T) I.transform(value, ClassUtil.getParameter(validator.getClass(), Validator.class)[0]);

            // Validate user input in detail.
            try {
                validator.validate(input);
            } catch (Exception e) {
                talk("Your input is invlid because " + e.getLocalizedMessage() + ", plese retry.");

                // Retry!
                return ask(message, validator);
            }

            // API definition
            return input;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see bee.UserInterface#ask(java.lang.String, java.lang.Object, bee.Validator)
     */
    @Override
    public <T> T ask(String question, T defaultAnswer, Validator<T> validator) {
        return null;
    }

    /**
     * @see bee.UserInterface#talk(String, Object...)
     */
    @Override
    public void talk(String message, Object... params) {
        System.out.format(message.concat("%n"), params);
    }

    /**
     * @see bee.UserNotifier#warn(java.lang.String, java.lang.Object[])
     */
    @Override
    public void warn(String message, Object... params) {
        System.out.format(message.concat("%n"), params);
    }

    /**
     * @see bee.UserNotifier#error(java.lang.String, java.lang.Object[])
     */
    @Override
    public void error(String message, Object... params) {
        System.out.format(message.concat("%n"), params);
    }

    /**
     * @see bee.UserInterface#ask(java.lang.Class)
     */
    @Override
    public <T> T ask(Class<T> question) {
        T t = I.make(question);
        Model<T> model = Model.load(question);

        for (Property property : model.properties) {
            try {
                Field field = question.getDeclaredField(property.name);

            } catch (Exception e) {
                // ignore
            }
        }

        return null;
    }
}
