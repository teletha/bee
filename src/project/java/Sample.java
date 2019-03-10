
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
import kiss.I;

public class Sample extends Task {

    @Command("main start")
    public void main() {
        require(A::main, B::main, C::main);

        ui.talk("FINISH MAIN");
    }

    /**
     * 
     */
    public static class HeavyTask extends Task {

        protected void wait(int count) {
            for (int i = 0; i < count; i++) {
                try {
                    Thread.sleep(500);
                    ui.talk(i + " at " + getClass().getSimpleName());
                } catch (InterruptedException e) {
                    throw I.quiet(e);
                }
            }
        }
    }

    /**
     * 
     */
    public static class A extends HeavyTask {
        @Command("A start")
        public void main() {
            wait(4);
        }
    }

    /**
     * 
     */
    public static class B extends HeavyTask {
        @Command("B start")
        public void main() {
            wait(8);
        }
    }

    /**
     * 
     */
    public static class C extends HeavyTask {
        @Command("C start")
        public void main() {
            wait(12);
        }
    }
}
