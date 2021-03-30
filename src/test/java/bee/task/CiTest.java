/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.task;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CiTest extends TaskTestBase {

    @Test
    public void update() {
        Ci ci = new Ci();

        List<String> original = lines("ok");
        List<String> updated = ci.update(original);
        List<String> originalPart = updated.subList(0, original.size());
        List<String> additionalPart = updated.subList(original.size() + 1, updated.size());
        assert listItem(original, originalPart);
        assert additionalPart.isEmpty() == false;

        List<String> latest = ci.update(updated);
        assert listItem(originalPart, latest.subList(0, original.size()));
        assert listItem(additionalPart, latest.subList(original.size() + 1, latest.size()));
    }

    /**
     * Create lines.
     * 
     * @param lines
     * @return
     */
    private List<String> lines(String... lines) {
        List<String> list = new ArrayList();

        for (String line : lines) {
            list.add(line);
        }
        return list;
    }

    /**
     * Test item matching
     * 
     * @param one
     * @param other
     * @return
     */
    private boolean listItem(List<String> one, List<String> other) {
        assert one.size() == other.size();

        for (int i = 0; i < one.size(); i++) {
            assert one.get(i).equals(other.get(i));
        }
        return true;
    }
}