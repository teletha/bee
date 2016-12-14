/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.extension;

import java.lang.annotation.RetentionPolicy;
import java.util.EnumMap;
import java.util.function.Function;

/**
 * @version 2016/12/13 12:20:50
 */
public class GenericUser {

    public static void main(String[] args) {
        Function<String, Integer> f = v -> v.length();
        System.out.println(f.with2("abcd").get());

        EnumMap map = new EnumMap(RetentionPolicy.class);
        map.put(RetentionPolicy.RUNTIME, "OK");
        map.dump();
    }
}
