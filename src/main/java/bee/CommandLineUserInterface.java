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
import java.util.List;

import kiss.Extensible;
import kiss.I;
import kiss.model.Codec;
import kiss.model.Model;
import kiss.model.Property;

/**
 * @version 2010/11/23 23:24:52
 */
class CommandLineUserInterface implements UserInterface {

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
        System.out.print(message);

        if (defaultAnswer != null) {
            System.out.print(" [" + defaultAnswer + "]");
        }
        System.out.print(" : ");

        // selectable question

        try {
            String input = new BufferedReader(new InputStreamReader(System.in)).readLine();

            // Remove whitespaces.
            input = input == null ? "" : input.trim();

            // Validate user input.
            if (defaultAnswer == null) {
                if (input.length() == 0) {
                    talk("Your input is empty, plese retry.");

                    // Retry!
                    return ask(message, null);
                }

                // API definition
                return (T) input;
            } else {
                Codec<T> codec = I.find(Codec.class, defaultAnswer.getClass());

                if (codec == null) {
                    codec = (Codec<T>) Model.load(defaultAnswer.getClass()).codec;
                }

                return input.length() == 0 ? defaultAnswer : codec.decode(input);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param message
     * @param defaultAnswer
     * @param answerType
     * @return
     */
    private <T> T ask(String message, T defaultAnswer, Class<T> answerType) {
        System.out.print(message);

        if (defaultAnswer != null) {
            System.out.print(" [" + defaultAnswer + "]");
        }
        System.out.print(" : ");

        // selectable question
        if (Extensible.class.isAssignableFrom(answerType)) {
            return (T) select(I.find((Class<Extensible>) answerType));
        }

        try {
            String input = new BufferedReader(new InputStreamReader(System.in)).readLine();

            // Remove whitespaces.
            input = input == null ? "" : input.trim();

            // Validate user input.
            if (defaultAnswer == null) {
                if (input.length() == 0) {
                    talk("Your input is empty, plese retry.");

                    // Retry!
                    return ask(message, null);
                }

                // API definition
                return (T) input;
            } else {
                Codec<T> codec = I.find(Codec.class, defaultAnswer.getClass());

                if (codec == null) {
                    codec = (Codec<T>) Model.load(defaultAnswer.getClass()).codec;
                }

                return input.length() == 0 ? defaultAnswer : codec.decode(input);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T select(List<T> items) {
        int counter = 1;

        System.out.println("");

        for (T item : items) {
            System.out.println(counter + "  " + item.getClass().getSimpleName());
        }

        try {
            String input = new BufferedReader(new InputStreamReader(System.in)).readLine();

            // Remove whitespaces.
            input = input == null ? "" : input.trim();

            try {
                int select = Integer.valueOf(input);

                if (select < 1) {
                    System.out.print("Your input is less than 1, plese retry.");
                    overwrite("Your input is less than 100, plese retry.");

                    // Retry!
                    return select(items);
                }

                if (items.size() < select) {
                    talk("Your input is over, plese retry.");

                    // Retry!
                    return select(items);
                }
                return items.get(select - 1);
            } catch (NumberFormatException e) {
                talk("Your input is not number, plese retry.");

                // Retry!
                return select(items);
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    private void overwrite(String message) {
        System.out.print("\r");
        System.out.println(message);
    }

    /**
     * @see bee.UserInterface#ask(java.lang.Class)
     */
    @Override
    public <T> T ask(Class<T> clazz) {
        T instance = I.make(clazz);
        Model model = Model.load(clazz);

        for (Field field : clazz.getDeclaredFields()) {
            Property property = model.getProperty(field.getName());

            if (property != null) {
                Question question = field.getAnnotation(Question.class);

                if (question != null) {
                    boolean loop = true;

                    while (loop) {
                        Object input = ask(question.message(), model.get(instance, property), property.model.type);

                        if (input != null) {
                            try {
                                model.set(instance, property, input);
                            } catch (Throwable e) {
                                error(e.getLocalizedMessage());
                                continue;
                            }
                        }
                        loop = false;
                    }
                }
            }
        }

        return instance;
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
}
