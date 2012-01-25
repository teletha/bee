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
package bee.compiler.source01;

import bee.compiler.SourceAnnotation;

/**
 * Test Class
 * 
 * @version 2011/03/13 15:51:30
 */
@SourceAnnotation("Main")
public class MainClass {

    /**
     * Constructor
     */
    @SourceAnnotation
    private MainClass(Object parent) {
    }

    /**
     * Method
     * 
     * @param message
     */
    @SourceAnnotation("Method1")
    public void documented(String message) {
    }

    /*
     * Non Javadoc
     */
    @SourceAnnotation("Method2")
    public void not(String value, int type) {
    }
}
