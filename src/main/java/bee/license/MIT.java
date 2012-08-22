/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.license;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 2012/01/27 0:55:50
 */
public class MIT implements License {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> text() {
        List<String> license = new ArrayList();
        license.add("Copyright (c) <YEAR> <NAME>");
        license.add("");
        license.add("Permission is hereby granted, free of charge, to any person obtaining a copy of");
        license.add("this software and associated documentation files (the \"Software\"), to deal in");
        license.add(" the Software without restriction, including without limitation the rights to use, copy,");
        license.add("modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,");
        license.add("and to permit persons to whom the Software is furnished to do so, subject to the");
        license.add("following conditions:");
        license.add("");
        license.add("The above copyright notice and this permission notice shall be included in all copies");
        license.add("or substantial portions of the Software.");
        license.add("");
        license.add("THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS");
        license.add("OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,");
        license.add(" FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL");
        license.add("THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR");
        license.add("OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,");
        license.add("ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR");
        license.add("OTHER DEALINGS IN THE SOFTWARE.");

        return license;
    }
}
