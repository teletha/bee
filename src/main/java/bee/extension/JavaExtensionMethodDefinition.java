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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import bee.Platform;
import kiss.I;

/**
 * @version 2016/12/13 15:34:13
 */
class JavaExtensionMethodDefinition {

    /** The target class to extend. */
    final Class targetClass;

    /** The archive of the target class. */
    final Path archive;

    /** The extension class. */
    final Class extensionClass;

    /** The extension method. */
    final Method method;

    /**
     * <p>
     * Build definition.
     * </p>
     * 
     * @param targetClass
     * @param archive
     * @param extensionClass
     * @param method
     */
    JavaExtensionMethodDefinition(Method method) {
        this.targetClass = method.getParameterTypes()[0];
        this.extensionClass = method.getDeclaringClass();
        this.method = method;

        Path path = I.locate(targetClass);
        this.archive = path == null ? Platform.JavaRuntime : path;
    }

    /**
     * <p>
     * Compute method signature.
     * </p>
     * 
     * @return
     */
    String signature() {
        TypeDeclarations declarations = new TypeDeclarations(method);

        if (declarations.types.isEmpty()) {
            return null;
        }

        return new StringBuilder().append(declarations)
                .append(new Params(method, declarations))
                .append(new SignatureWriter(declarations).write(method.getGenericReturnType()))
                .append(new Throws(method, declarations))
                .toString();
    }

    /**
     * @version 2016/12/13 13:19:03
     */
    private static class TypeDeclarations {

        List<Type> types = new ArrayList();

        List<Type> extensionClassTypes = new ArrayList();

        List<TypeVariable> originalClassTypes = new ArrayList();

        /**
         * 
         */
        TypeDeclarations(Method method) {
            types = Arrays.asList(method.getTypeParameters());

            Type target = method.getGenericParameterTypes()[0];
            if (target instanceof ParameterizedType) {
                ParameterizedType param = (ParameterizedType) target;
                extensionClassTypes = Arrays.asList(param.getActualTypeArguments());
                originalClassTypes = Arrays.asList(((Class) param.getRawType()).getTypeParameters());
            }
        }

        /**
         * <p>
         * Compute original variable name.
         * </p>
         * 
         * @param variable
         * @return
         */
        private String original(TypeVariable variable) {
            int index = extensionClassTypes.indexOf(variable);

            return index == -1 ? variable.getName() : originalClassTypes.get(index).getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            List<Type> declarations = I.signal(types).skip(extensionClassTypes).toList();

            return declarations.isEmpty() ? "" : new SignatureWriter(null).write(declarations, "", "<", ">");
        }
    }

    /**
     * @version 2016/12/13 13:19:03
     */
    private static class Params {

        TypeDeclarations declarations;

        List<Type> types = new ArrayList();

        /**
         * 
         */
        Params(Method method, TypeDeclarations declarations) {
            this.declarations = declarations;

            Type[] types = method.getGenericParameterTypes();

            for (int i = 1; i < types.length; i++) {
                this.types.add(types[i]);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return new SignatureWriter(declarations).write(types, "", "(", ")");
        }
    }

    /**
     * @version 2016/12/13 13:19:03
     */
    private static class Throws {

        TypeDeclarations declarations;

        List<Type> types;

        /**
         * 
         */
        Throws(Method method, TypeDeclarations declarations) {
            this.types = Arrays.asList(method.getGenericExceptionTypes());
            this.declarations = declarations;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return types.isEmpty() ? "" : new SignatureWriter(declarations).write(types, "^", "^", "");
        }
    }

    /**
     * @version 2016/12/13 13:57:10
     */
    private static class SignatureWriter {

        private final TypeDeclarations declarations;

        protected SignatureWriter(TypeDeclarations declarations) {
            this.declarations = declarations;
        }

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(Type[] types) {
            return write(types, "", "", "");
        }

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(List<Type> types, String separator, String prefix, String suffix) {
            return write(types.toArray(new Type[types.size()]), separator, prefix, suffix);
        }

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(Type[] types, String separator, String prefix, String suffix) {
            StringJoiner joiner = new StringJoiner(separator, prefix, suffix);
            for (Type type : types) {
                joiner.add(write(type));
            }
            return joiner.toString();
        }

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(Type type) {
            if (type instanceof TypeVariable) {
                return write((TypeVariable) type);
            } else if (type instanceof ParameterizedType) {
                return write((ParameterizedType) type);
            } else if (type instanceof WildcardType) {
                return write((WildcardType) type);
            } else if (type instanceof GenericArrayType) {
                return write((GenericArrayType) type);
            } else {
                return write((Class) type);
            }
        }

        private boolean forceTypeOnly = false;

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(TypeVariable variable) {
            if (forceTypeOnly || (declarations != null && declarations.types.contains(variable))) {
                return "T" + (declarations == null ? variable.getName() : declarations.original(variable)) + ";";
            } else {
                try {
                    forceTypeOnly = true;
                    return variable.getName() + ":" + write(variable.getBounds());
                } finally {
                    forceTypeOnly = false;
                }
            }
        }

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(ParameterizedType type) {
            String raw = write(type.getRawType());
            return raw.substring(0, raw.length() - 1) + write(type.getActualTypeArguments(), "", "<", ">") + ";";
        }

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(GenericArrayType type) {
            return "[" + write(type.getGenericComponentType());
        }

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(WildcardType type) {
            Type[] lower = type.getLowerBounds();

            if (lower.length != 0) {
                return "-" + write(lower);
            } else {
                Type[] upper = type.getUpperBounds();

                if (upper.length == 1 && upper[0] == Object.class) {
                    return "*";
                } else {
                    return "+" + write(type.getUpperBounds());
                }
            }
        }

        /**
         * <p>
         * Stringlization helper.
         * </p>
         * 
         * @param type
         * @return
         */
        private String write(Class type) {
            String descriptor = org.objectweb.asm.Type.getType(type).getDescriptor();

            return (declarations == null && type.isInterface() ? ":" : "") + descriptor;
        }
    }
}
