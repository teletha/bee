/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.util;

import org.junit.jupiter.api.Test;

class CodeTest {

    @Test
    void java() {
        Code code = Code.java()
                .write("public class OK {")
                .write("  public OK() {")
                .write("    System.println('Hello!');")
                .write("  }")
                .write("}");

        assert code.text().equals("""
                public class OK {
                  public OK() {
                    System.println("Hello!");
                  }
                }""");
    }
}
