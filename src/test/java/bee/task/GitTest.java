/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @version 2017/01/18 16:01:01
 */
public class GitTest extends TaskTestBase {

    @Test
    public void update() throws Exception {
        Git git = new Git();

        List<String> original = lines("ok");
        List<String> updated = git.update(original);
        List<String> originalPart = updated.subList(0, original.size());
        List<String> additionalPart = updated.subList(original.size() + 1, updated.size());
        assert listItem(original, originalPart);
        assert additionalPart.isEmpty() == false;

        List<String> latest = git.update(updated);
        assert listItem(originalPart, latest.subList(0, original.size()));
        assert listItem(additionalPart, latest.subList(original.size() + 1, latest.size()));
    }

    /**
     * <p>
     * Create lines.
     * </p>
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
     * <p>
     * Test item matching
     * </p>
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
