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
package bee.task;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;

import kiss.I;
import kiss.model.ClassUtil;
import bee.Bee;
import bee.UserInterface;
import bee.definition.Project;

/**
 * @version 2010/04/02 3:56:10
 */
public abstract class Task {

    /** The current processing project. */
    protected final Project project;

    /** The user interface. */
    protected final UserInterface ui;

    /**
     * <p>
     * Exposed constructor.
     * <p>
     */
    protected Task() {
        this.project = I.make(Project.class);
        this.ui = I.make(UserInterface.class);
    }

    @Command(description = "Display help message for all commands of this task.")
    public void help() {

        root: for (Entry<Method, List<Annotation>> entry : ClassUtil.getAnnotations(getClass()).entrySet()) {
            String name = entry.getKey().getName();

            if (!name.equals("help")) {
                for (Annotation annotation : entry.getValue()) {
                    if (annotation.annotationType() == Command.class) {
                        Command command = (Command) annotation;

                        // display usage description for this commnad
                        ui.talk(name);
                        ui.talk("    ");

                        continue root;
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Build other task.
     * </p>
     * 
     * @param taskClass
     * @return
     */
    protected <T extends Task> T require(Class<T> taskClass) {
        return I.make(Bee.class).createTask(taskClass);
    }
}
