/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public final class ModelUtil {

    /**
     * Find the top-level {@link TypeElement} (not member class).
     * 
     * @param e
     * @return
     */
    public static TypeElement getTopLevelTypeElement(Element e) {
        Element parent = e.getEnclosingElement();

        while (parent != null && parent.getKind() != ElementKind.PACKAGE) {
            e = parent;
            parent = e.getEnclosingElement();
        }
        return (TypeElement) e;
    }

    /**
     * Collect all types.
     * 
     * @param type
     * @return
     */
    public static Set<TypeMirror>[] getAllTypes(Element type) {
        Set<TypeMirror> supers = new LinkedHashSet();
        Set<TypeMirror> interfaces = new TreeSet<>(Comparator
                .<TypeMirror, String> comparing(t -> ((TypeElement) DocTool.TypeUtils.asElement(t)).getSimpleName().toString()));
        collect(type.asType(), supers, interfaces);

        return new Set[] {supers, interfaces};
    }

    /**
     * Collect all types.
     * 
     * @param type
     * @param superTypes
     * @param interfaceTypes
     */
    private static void collect(TypeMirror type, Set<TypeMirror> superTypes, Set<TypeMirror> interfaceTypes) {
        for (TypeMirror up : DocTool.TypeUtils.directSupertypes(type)) {
            if (up.toString().equals("java.lang.Object")) {
                continue;
            }

            Element e = DocTool.TypeUtils.asElement(up);
            if (e.getKind() == ElementKind.INTERFACE) {
                interfaceTypes.add(up);
            } else {
                superTypes.add(up);
            }
            collect(up, superTypes, interfaceTypes);
        }
    }
}
