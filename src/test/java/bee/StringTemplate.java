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

import java.io.IOException;

/**
 * @version 2010/11/24 12:21:58
 */
public class StringTemplate implements CharSequence, Appendable {

    public void $(Object... contents) {

    }

    /**
     * @see java.lang.Appendable#append(java.lang.CharSequence)
     */
    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return null;
    }

    /**
     * @see java.lang.Appendable#append(java.lang.CharSequence, int, int)
     */
    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        return null;
    }

    /**
     * @see java.lang.Appendable#append(char)
     */
    @Override
    public Appendable append(char c) throws IOException {
        return null;
    }

    /**
     * @see java.lang.CharSequence#length()
     */
    @Override
    public int length() {
        return 0;
    }

    /**
     * @see java.lang.CharSequence#charAt(int)
     */
    @Override
    public char charAt(int index) {
        return 0;
    }

    /**
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return null;
    }

}
