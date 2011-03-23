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

/**
 * @version 2011/03/23 19:21:10
 */
public class Utility {

    /**
     * <p>
     * Retrieve an extension-like value from the specified expression.
     * </p>
     * 
     * @param name A target.
     * @return A extension-like value.
     */
    public static final String getExtension(String name) {
        // Check null
        if (name == null) {
            return "";
        }

        // Search last separator.
        int index = name.lastIndexOf('.');

        // API definition
        return index == -1 ? name : name.substring(index + 1);
    }
}
