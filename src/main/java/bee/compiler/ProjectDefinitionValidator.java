/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.compiler;

import javax.lang.model.SourceVersion;

import bee.Bee;
import bee.UserInterface;
import bee.api.ProjectDefinition;
import bee.task.Command;

/**
 * @version 2012/04/19 15:03:54
 */
public class ProjectDefinitionValidator extends AnnotationValidator<ProjectDefinition> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ProjectDefinition annotation, Source source, AnnotationNotifier notifier) {
        String group = annotation.group().trim();

        if (group.length() == 0) {
            notifier.error("Group name is empty.");
            return;
        }

        for (String name : group.split("\\.")) {
            if (SourceVersion.isKeyword(name)) {
                notifier.error("Group name can't contains java keyword. [", name, "]");
                return;
            }
        }
        try {
            Bee bee = new Bee(source.getClassFile().getParent().getParent(), new UserInterface() {

                @Override
                protected void write(String message) {
                }

                @Override
                public void startCommand(String name, Command command) {
                }

                @Override
                public void endCommand(String name, Command command) {
                }
            });
            bee.execute("eclipse");
        } catch (Exception e) {
            notifier.error(e);
        }

    }
}
