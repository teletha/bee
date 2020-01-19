/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc.site;

import java.util.List;

import bee.doc.ClassInfo;
import bee.doc.ExecutableInfo;
import bee.doc.FieldInfo;
import bee.doc.MemberInfo;
import bee.doc.MethodInfo;
import bee.doc.builder.HTML;
import stylist.Style;
import stylist.StyleDSL;

/**
 * 
 */
class SubNavigationView extends HTML {

    private final ClassInfo info;

    /**
     * @param info
     */
    public SubNavigationView(ClassInfo info) {
        this.info = info;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void declare() {
        members("Constructors", info.constructors());
        members("Static Fields", info.staticFields());
        members("Fields", info.nonStaticFields());
        members("Static Methods", info.staticMethods());
        members("Methods", info.nonStaticMethods());
    }

    private void members(String title, List<? extends MemberInfo> members) {
        if (members.size() != 0) {
            $("h5", Styles.Title, text(title));
            $("ul", foŕ(members, m -> {
                $("li", () -> {
                    $(m.createModifier());
                    $(m.createName());

                    if (m instanceof ExecutableInfo) {
                        ExecutableInfo e = (ExecutableInfo) m;
                        $(e.createParameter());
                    }

                    if (m instanceof MethodInfo) {
                        $("i", Styles.Return, ((MethodInfo) m).createReturnType());
                    }

                    if (m instanceof FieldInfo) {
                        $("i", Styles.Return, ((FieldInfo) m).createType());
                    }
                });
            }));
        }
    }

    /**
     * Style definition.
     */
    private interface Styles extends StyleDSL, BaseStyle {

        Style Title = () -> {
            margin.top(0.9, rem);
            font.weight.bold().size(1, rem);
        };

        Style Return = () -> {
            font.color(palette.secondary.lighten(-30));

            $.before(() -> {
                content.text(":");
                padding.horizontal(0.3, em);
            });
        };
    }
}