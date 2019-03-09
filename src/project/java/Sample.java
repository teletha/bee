
/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
import bee.api.Command;
import bee.api.Task;
import bee.task.Compile;

public class Sample extends Task {

    @Command("main start")
    public void main() {
        require(Compile::source, Compile::test);
        require2(A::main, B::main);
    }

    /**
     * 
     */
    public static class A extends Task {
        @Command("A start")
        public void main() {

        }
    }

    /**
     * 
     */
    public static class B extends Task {
        @Command("B start")
        public void main() {

        }
    }
}
