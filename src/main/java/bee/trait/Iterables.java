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
package bee.trait;

import java.util.Iterator;

import ezbean.I;

/**
 * @version 2011/03/15 12:00:36
 */
@Trait
public class Iterables {

    /**
     * <p>
     * Join the {@link Iterable} items by the specified character.
     * </p>
     * 
     * @param iterable A target {@link Iterable} list to join items.
     * @param separator A separator character.
     * @return A joined string.
     */
    public static final String join(Iterable iterable, char separator) {
        StringBuilder builder = new StringBuilder();
        Iterator iterator = iterable.iterator();

        if (iterator.hasNext()) {
            builder.append(I.transform(iterator.next(), String.class));

            while (iterator.hasNext()) {
                builder.append(separator);
                builder.append(I.transform(iterator.next(), String.class));
            }
        }

        // API definition
        return builder.toString();
    }
}
