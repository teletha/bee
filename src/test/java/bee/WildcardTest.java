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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @version 2010/12/23 1:25:49
 */
public class WildcardTest {

    @Test
    public void none() throws Exception {
        assertTrue(match("test.java", "test.java"));
    }

    @Test
    public void wildcard() throws Exception {
        assertTrue(match("test.java", "*"));
    }

    @Test
    public void wildcardRight() throws Exception {
        assertTrue(match("benchmarktest.java", "*.java"));
        assertFalse(match("short", "*patternIsLongerThanInputValue"));
        assertFalse(match("Value", "*patternIsLongerThanInputValue"));
    }

    @Test
    public void wildcardLeft() throws Exception {
        assertTrue(match("test.java", "test.*"));
        assertFalse(match("short", "patternIsLongerThanInputValue*"));
        assertFalse(match("pattern", "patternIsLongerThanInputValue*"));
    }

    @Test
    public void wildcardBoth() throws Exception {
        assertTrue(match("test.java", "test*java"));
    }

    @Test
    public void wildcardNone() throws Exception {
        assertTrue(match("test.java", "test*.java"));
    }

    @Test
    public void ambiguous() throws Exception {
        assertTrue(match("test.java", "t*a"));
    }

    @Test
    public void multiple() throws Exception {
        assertTrue(match("test.java", "t*s*.j*a"));
    }

    @Test
    public void dontRetreat() throws Exception {
        assertFalse(match("MatchAtNextPosition", "*ext*ext*"));
    }

    @Test
    public void dontRetreatOne() throws Exception {
        assertFalse(match("MatchAtNextPosition", "*e*e*"));
        assertTrue(match("MatchAtNextPosition", "*e*x*"));
    }

    @Test
    public void dontRetreatMix() throws Exception {
        assertFalse(match("MatchAtNextPosition", "*e*ext*"));
        assertTrue(match("MatchAtNextPosition", "*e*xt*"));
    }

    @Test
    public void escapeChar() throws Exception {
        assertTrue(match("test.java", "\\test.java"));
    }

    @Test
    public void escapeAstarisk() throws Exception {
        assertTrue(match("i*i", "i\\*i"));
    }

    @Test
    public void escapeEscape() throws Exception {
        assertTrue(match("i\\i", "i\\\\i"));
    }

    @Test
    public void escapeComplex() throws Exception {
        assertFalse(match("complex", "comple\\*"));
        assertTrue(match("comple*", "comple\\*"));
        assertFalse(match("complex", "comple\\\\*"));
        assertTrue(match("comple\\x", "comple\\\\*"));
        assertFalse(match("comple\\\\", "comple\\\\\\*"));
        assertTrue(match("comple\\*", "comple\\\\\\*"));
    }

    @Test
    public void more() throws Exception {
        assertFalse(match("test.java", "test.javaa"));
        assertFalse(match("test.java", "ttest.java"));
    }

    @Test
    public void less() throws Exception {
        assertFalse(match("test.java", "test.jav"));
        assertFalse(match("test.java", "est.java"));
    }

    @Test
    public void miss() throws Exception {
        assertFalse(match("test.java", "test.jawa"));
    }

    @Test
    public void missWildcardRight() throws Exception {
        assertFalse(match("benchmarktest.java", "*.txt"));
    }

    @Test
    public void missHead() throws Exception {
        assertFalse(match("head doesn't match", "d*h"));
    }

    @Test
    public void missTail() throws Exception {
        assertFalse(match("tail doesn't match", "t*m"));
    }

    @Test
    public void noneAsciiWildcardSingleCharcter() throws Exception {
        assertTrue(match("あいうえお", "あ*お"));
    }

    @Test
    public void noneAsciiWildcardMultipleCharcters() throws Exception {
        assertTrue(match("あいうえお", "あい*お"));
    }

    @Test
    public void noneAsciiMultipleWildcards() throws Exception {
        assertTrue(match("天上天下唯我独尊", "天上*下*独尊"));
    }

    @Test
    public void noneAsciiMiss() throws Exception {
        assertFalse(match("天上天下唯我独尊", "天*使"));
    }

    @Test
    public void noneAsciiAmbiguous() throws Exception {
        assertTrue(match("天使ちゃんまじ天使", "天*使"));
        assertTrue(match("天使ちゃんまじ天使", "*まじ天*使"));
        assertTrue(match("天使ちゃんまじ天使", "天*使ちゃん*"));
    }

    /**
     * Helper method to match.
     * 
     * @param value
     * @param pattern
     * @return
     */
    private boolean match(String value, String pattern) {
        return new Wildcard(pattern).match(value);
        // return WildcardHelper.match(value, WildcardHelper.compilePattern(pattern));
    }
}
