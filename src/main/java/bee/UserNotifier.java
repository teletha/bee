/*
 * Copyright (C) 2011 Nameless Production Committee.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import kiss.I;

/**
 * <p>
 * Notifiable user interface.
 * </p>
 * 
 * @version 2011/03/23 16:46:09
 */
public abstract class UserNotifier {

    /**
     * <p>
     * Talk to user with decoration like title.
     * </p>
     * 
     * @param title
     */
    public void title(CharSequence title) {
        talk("----------------------------------------------------------------------");
        talk(title);
        talk("----------------------------------------------------------------------");
    }

    /**
     * <p>
     * Talk to user.
     * </p>
     * 
     * @param messages Your message.
     */
    public void talk(Object... messages) {
        write(build(messages));
    }

    /**
     * <p>
     * Warn to user.
     * </p>
     * 
     * @param messages Your warning message.
     */
    public void warn(Object... messages) {
        talk("[WARN]", messages);
    }

    /**
     * <p>
     * Declare a state of emergency.
     * </p>
     * 
     * @param message Your emergency message.
     */
    public void error(Object... messages) {
        talk("[ERROR]", messages);
    }

    /**
     * <p>
     * Write message to user.
     * </p>
     * 
     * @param message
     */
    protected abstract void write(String message);

    /**
     * <p>
     * Helper method to build message.
     * </p>
     * 
     * @param messages Your messages.
     * @return A combined message.
     */
    protected String build(Object... messages) {
        StringBuilder builder = new StringBuilder();
        build(builder, messages);

        if (builder.charAt(builder.length() - 1) != '\r') {
            builder.append(Platform.EOL);
        }

        return builder.toString();
    }

    /**
     * <p>
     * Helper method to build message.
     * </p>
     * 
     * @param builder A message builder.
     * @param messages Your messages.
     */
    private void build(StringBuilder builder, Object... messages) {
        for (Object message : messages) {
            if (message == null) {
                builder.append("null");
            } else {
                Class type = message.getClass();

                if (type.isArray()) {
                    buildArray(builder, type.getComponentType(), message);
                } else if (CharSequence.class.isAssignableFrom(type)) {
                    builder.append((CharSequence) message);
                } else if (Throwable.class.isAssignableFrom(type)) {
                    buildError(builder, (Throwable) message);
                } else {
                    builder.append(I.transform(message, String.class));
                }
            }
        }
    }

    /**
     * <p>
     * Helper method to build message from various array type.
     * </p>
     * 
     * @param builder A message builder.
     * @param type A array type.
     * @param array A message array.
     */
    private void buildArray(StringBuilder builder, Class type, Object array) {
        if (type == int.class) {
            builder.append(Arrays.toString((int[]) array));
        } else if (type == long.class) {
            builder.append(Arrays.toString((long[]) array));
        } else if (type == float.class) {
            builder.append(Arrays.toString((float[]) array));
        } else if (type == double.class) {
            builder.append(Arrays.toString((double[]) array));
        } else if (type == boolean.class) {
            builder.append(Arrays.toString((boolean[]) array));
        } else if (type == char.class) {
            builder.append(Arrays.toString((char[]) array));
        } else if (type == byte.class) {
            builder.append(Arrays.toString((byte[]) array));
        } else if (type == short.class) {
            builder.append(Arrays.toString((short[]) array));
        } else {
            build(builder, (Object[]) array);
        }
    }

    /**
     * <p>
     * Build error message.
     * </p>
     * 
     * @param builder A message builder.
     * @param throwable An error message.
     */
    private void buildError(StringBuilder builder, Throwable throwable) {
        StringWriter writer = new StringWriter();

        throwable.printStackTrace(new PrintWriter(writer));

        builder.append(writer.toString());
    }
}
