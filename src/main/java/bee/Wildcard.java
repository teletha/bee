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

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @version 2011/01/08 1:02:24
 */
public class Wildcard {

    /** The matching types. */
    private final int[] types;

    /** The actual patterns. */
    private final int[][] patterns;

    /** The jump tables. */
    private final int[][] tables;

    /**
     * <p>
     * Translate the given glob like expression into an multi-dimensional int array representing the
     * pattern matchable by this class. This function treats two special <em>*</em> and <em>\</em>
     * characters.
     * </p>
     * <p>
     * Here is how the conversion algorithm works :
     * </p>
     * <ul>
     * <li>The astarisk '*' character is converted to wildcard character, meaning that zero or more
     * characters are to be matched.</li>
     * <li>The backslash '\' character is used as an escape sequence ('\*' is translated in '*', not
     * in wildcard). If an exact '\' character is to be matched the source string must contain a
     * '\\' sequence.</li>
     * </ul>
     * <p>
     * This method returns the list of compiled pattern (two dimentional int array). Here is the
     * compiled pattern format :
     * </p>
     * <dl>
     * <dt>int[0][0]</dt>
     * <dd>Matching type. (0 : some, 1 : head, 2 : tail, 3 : every, 4 : one)</dd>
     * <dt>int[0][1]</dt>
     * <dd>ASCII only flag. (0 : ascii only, 1 : contains non-ASCII)</dd>
     * <dt>int[1]</dt>
     * <dd>The int array of divided pattern. (e.g. [t, e, s, t])</dd>
     * <dt>int[2]</dt>
     * <dd>The int array (size is 96) of jump table if needed, otherwise null.</dd>
     * </dl>
     * 
     * @param value The string to translate.
     */
    public Wildcard(String value) {
        if (value == null || value.length() == 0 || value.equals("*")) {
            types = null;
            patterns = tables = null;
        } else {
            char[] buffer = value.toCharArray();

            int[] bit = new int[buffer.length];
            ArrayList<int[]> bits = new ArrayList();

            int offset = 0;
            int size = buffer.length - 1;
            boolean escape = false;
            boolean head = buffer[0] != '*';
            boolean tail = true;

            for (int i = 0; i <= size; i++) {
                char c = buffer[i];

                if (escape) {
                    escape = false;
                    bit[offset++] = c; // parse escaped character
                } else if (c == '\\') {
                    escape = true; // escape next character
                } else if (c != '*') {
                    bit[offset++] = c; // parse character
                } else if (i == size) {
                    tail = false; // skip wildcard at tail
                } else if (i != 0) {
                    bits.add(Arrays.copyOfRange(bit, 0, offset)); // retrieve as pattern
                    offset = 0; // reset
                }
            }
            bits.add(Arrays.copyOfRange(bit, 0, offset)); // retrieve as pattern

            // Make compiled pattern set.
            size = bits.size();

            this.types = new int[size];
            this.patterns = new int[size][];
            this.tables = new int[size][];

            // match at head
            if (head) types[0] |= 1;

            // match at tail
            if (tail) types[size - 1] |= 2;

            for (int i = 0; i < size; i++) {
                patterns[i] = bits.get(i);

                if (types[i] == 0) {
                    int length = patterns[i].length;

                    // make jump table for matching if needed
                    if (length == 1) {
                        types[i] = 4;
                    } else {
                        tables[i] = new int[96];
                        Arrays.fill(tables[i], length + 1);

                        for (int j = length - 1; 0 <= j; j--) {
                            int c = patterns[i][j];

                            if (32 <= c && c < 128) {
                                tables[i][c - 32] = j + 1;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Match a pattern against a string.
     * </p>
     * 
     * @param value The string to match.
     * @return <code>true</code> if a match.
     * @throws NullPointerException If value is <code>null</code>.
     */
    public boolean match(String value) {
        if (types != null) {
            char[] input = value.toCharArray();
            int inputPosition = 0;
            int inputSize = input.length;

            for (int x = 0; x < types.length; x++) {
                int[] pattern = patterns[x];
                int patternPosition = pattern.length;

                // Pattern must be shorter than input value.
                if (inputSize < patternPosition) {
                    return false;
                }

                // ====================================================
                // Switch Matching Type
                // ====================================================
                matching: switch (types[x]) {
                case 1: // ============= Match characters at head ============= //
                    while (inputPosition < patternPosition) {
                        if (input[inputPosition] != pattern[inputPosition++]) {
                            return false; // unmatching
                        }
                    }
                    continue;

                case 3: // ============= Match every characters ============= //
                    // Pattern must have same length as input value.
                    if (inputSize != patternPosition) return false; // unmatching
                    // no break
                case 2: // ============= Match characters at tail ============= //
                    // In general, tail matching is used most frequently in any situations. So we
                    // should
                    // optimize code for the tail matching.
                    inputPosition = inputSize;

                    while (0 < patternPosition) {
                        if (input[--inputPosition] != pattern[--patternPosition]) {
                            return false; // unmatching
                        }
                    }
                    return true; // matching

                case 4: // ============= Match only one character ============= //
                    int only = pattern[0]; // cache it

                    for (int i = inputSize - 1; inputPosition <= i; i--) {
                        if (input[i] == only) {
                            // matching, advance input position
                            inputPosition = i + 1;
                            break matching;
                        }
                    }
                    return false; // unmatching

                case 0: // ============= Match some characters ============= //
                    int start = inputSize - patternPosition;

                    // using quick search algorithm
                    search: while (inputPosition <= start) {
                        int current = 0;

                        while (current < patternPosition) {
                            if (input[start + current] != pattern[current++]) {
                                // unmatching, retreat start position
                                int next = input[start - 1];

                                if (32 <= next && next < 128) {
                                    start -= tables[x][next - 32];
                                } else {
                                    start--;
                                }
                                continue search;
                            }
                        }

                        // matching, advance input position
                        inputPosition = start + current;
                        break matching;
                    }
                    return false; // unmatching
                }
            }
        }

        // Match against all patterns.
        return true;
    }
}
