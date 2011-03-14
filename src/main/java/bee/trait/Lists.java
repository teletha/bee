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

import java.util.List;

/**
 * @version 2011/03/14 19:22:09
 */
@Trait
public class Lists {

    /**
     * <p>
     * Join the list items by the specified character.
     * </p>
     * 
     * @param list A target list to join items.
     * @param separator A separator character.
     * @return A joined string.
     */
    public static final String join(List list, char separator) {
        StringBuilder builder = new StringBuilder();

        for (int start = 0, end = list.size(); start < end; start++) {
            builder.append(list.get(start));

            if (start < end - 1) {
                builder.append(separator);
            }
        }

        // API definition
        return builder.toString();
    }
}
