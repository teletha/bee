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

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Modifier;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree.Kind;

import kiss.I;

public class TypeResolver {

    /** built-in java.lang types */
    private static final Map<String, String> JavaLangTypes = collectJavaLangTypes();

    /**
     * Register all classes under the java.lang package as classes that can be resolved.
     */
    private static Map<String, String> collectJavaLangTypes() {
        Map<String, String> map = new HashMap();

        try {
            ModuleReference module = ModuleFinder.ofSystem().find("java.base").get();

            try (Stream<String> resources = module.open().list()) {
                resources.filter(name -> name.startsWith("java/lang/") && name.indexOf("/", 10) == -1 && name.endsWith(".class"))
                        .map(name -> name.replace('/', '.').substring(0, name.length() - 6))
                        .map(name -> {
                            try {
                                return Class.forName(name);
                            } catch (ClassNotFoundException e) {
                                return null;
                            }
                        })
                        .filter(clazz -> clazz != null && Modifier.isPublic(clazz.getModifiers()))
                        .forEach(clazz -> map.put(clazz.getSimpleName(), clazz.getCanonicalName()));
            }
            return map;
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /** PackageName-URL pair */
    private final Map<String, String> externals;

    /** Internal package mames */
    private final Set<String> internals;

    /** Imported types. */
    private final Map<String, String> importedTypes = new HashMap();

    /**
     * @param externals
     */
    TypeResolver(Map<String, String> externals, Set<String> internals, TypeElement clazz) {
        this.externals = externals == null ? Map.of() : externals;
        this.internals = internals == null ? Set.of() : internals;

        collectImportedTypes(clazz);
        collectMemberTypes(clazz);
    }

    /**
     * Collect the imported types.
     * 
     * @param clazz
     */
    private void collectImportedTypes(Element clazz) {
        I.signal(DocTool.DocUtils.getPath(clazz))
                .take(tree -> tree.getKind() == Kind.COMPILATION_UNIT)
                .as(CompilationUnitTree.class)
                .flatIterable(CompilationUnitTree::getImports)
                .to(tree -> {
                    if (tree.isStatic()) {

                    } else {
                        String fqcn = tree.getQualifiedIdentifier().toString();
                        importedTypes.put(fqcn.substring(fqcn.lastIndexOf(".") + 1), fqcn);
                    }
                });
    }

    /**
     * Collect the member types.
     * 
     * @param clazz
     */
    private void collectMemberTypes(TypeElement clazz) {
        String fqcn = clazz.getQualifiedName().toString();
        importedTypes.put(fqcn.substring(fqcn.lastIndexOf(".") + 1), fqcn);

        I.signal(clazz.getEnclosedElements())
                .take(e -> e.getKind() == ElementKind.CLASS)
                .as(TypeElement.class)
                .to(this::collectMemberTypes);
    }

    public String resolveTestcase(String className) {
        return null;
    }

    /**
     * Compute FQCN from the specified simple name.
     * 
     * @param className
     */
    public String resolveFQCN(String className) {
        String fqcn = importedTypes.get(className);
        if (fqcn == null) fqcn = JavaLangTypes.get(className);

        return fqcn == null ? className : fqcn;
    }

    /**
     * Return the URL of the document for the specified type.
     * 
     * @param type A target type to locate document.
     * @return
     */
    public final String resolveDocumentLocation(String type) {
        return resolveDocumentLocation(resolve(type));
    }

    /**
     * Return the URL of the document for the specified type.
     * 
     * @param type A target type to locate document.
     * @return
     */
    public final String resolveDocumentLocation(DeclaredType type) {
        return resolveDocumentLocation((TypeElement) type.asElement());
    }

    /**
     * Return the URL of the document for the specified type.
     * 
     * @param type A target type to locate document.
     * @return
     */
    public final String resolveDocumentLocation(TypeElement type) {
        return resolveDocumentLocation(resolve(type));
    }

    /**
     * Returns the URL of the document with the specified type name.
     * 
     * @param moduleName Module name. Null or empty string is ignored.
     * @param packageName Package name. Null or empty string is ignored.
     * @param enclosingName Enclosing type name. Null or empty string is ignored.
     * @param typeName Target type's simple name.
     * @return Resoleved URL.
     */
    private final String resolveDocumentLocation(ResolvedType type) {
        String externalURL = externals.get(type.packageName);

        if (externalURL != null) {
            StringBuilder builder = new StringBuilder(externalURL);
            if (type.moduleName.length() != 0) builder.append(type.moduleName).append('/');
            if (type.packageName.length() != 0) builder.append(type.packageName.replace('.', '/')).append('/');
            if (type.enclosingName.length() != 0) builder.append(type.enclosingName).append('.');
            builder.append(type.typeName).append(".html");

            return builder.toString();
        }

        if (internals.contains(type.packageName)) {
            StringBuilder builder = new StringBuilder("/types/");
            if (type.packageName.length() != 0) builder.append(type.packageName).append('.');
            if (type.enclosingName.length() != 0) builder.append(type.enclosingName).append('.');
            builder.append(type.typeName).append(".html");

            return builder.toString();
        }
        return null;
    }

    /**
     * Resolve from element.
     * 
     * @return Resoleved type.
     */
    private final ResolvedType resolve(TypeElement e) {
        ResolvedType resolved = new ResolvedType();
        resolved.typeName = e.getSimpleName().toString();

        // enclosing
        Deque<String> enclosings = new LinkedList();
        Element enclosing = e.getEnclosingElement();
        while (enclosing.getKind() != ElementKind.PACKAGE) {
            enclosings.addFirst(((TypeElement) enclosing).getSimpleName().toString());
            enclosing = enclosing.getEnclosingElement();
        }
        resolved.enclosingName = I.join(".", enclosings);

        // pacakage
        resolved.packageName = enclosing.toString();

        // module
        enclosing = enclosing.getEnclosingElement();

        if (enclosing instanceof ModuleElement) {
            ModuleElement module = (ModuleElement) enclosing;
            resolved.moduleName = module.getQualifiedName().toString();
        } else {
            resolved.moduleName = "";
        }

        return resolved;
    }

    private final ResolvedType resolve(String typeName) {
        if (typeName.indexOf('.') == -1) {
            typeName = resolveFQCN(typeName);
        }

        TypeElement type = Javadoc.ElementUtils.getTypeElement(typeName);

        if (type == null) {
            int index = typeName.lastIndexOf('.');

            ResolvedType resolved = new ResolvedType();
            if (index == -1) {
                resolved.typeName = typeName;
            } else {
                resolved.packageName = typeName.substring(0, index);
                resolved.typeName = typeName.substring(index + 1);
            }
            return resolved;
        } else {
            return resolve(type);
        }
    }

    /**
     * Completed resolved type.
     */
    private static class ResolvedType {

        private String typeName = "";

        private String enclosingName = "";

        private String packageName = "";

        private String moduleName = "";

        /**
         * If this type can identify the source, it will be converted to {@link TypeElement}.
         * Otherwise, empty is returned.
         * 
         * @return
         */
        private Optional<TypeElement> asElement() {
            return Optional.ofNullable(DocTool.ElementUtils.getTypeElement(packageName + "." + enclosingName + "." + typeName));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "ResolvedType [typeName=" + typeName + ", enclosingName=" + enclosingName + ", packageName=" + packageName + ", moduleName=" + moduleName + "]";
        }
    }
}
