/*
 * Copyright (C) 2015 Nameless Production Committee
 * 
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.task;

import static bee.Platform.*;
import static javax.tools.Diagnostic.Kind.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.StandardLocation;

import kiss.Extensible;
import kiss.I;
import bee.task.AnnotationProcessor.ProjectInfo;

/**
 * @version 2012/11/11 15:36:14
 */
public abstract class AnnotationValidator<A extends Annotation> implements Extensible {

    /** The current processing element. */
    private Element element;

    /** The root tree. */
    private TypeElement root;

    /** The actual notifier. */
    private Messager messager;

    /**
     * <p>
     * The file util.
     * </p>
     * <p>
     * Eclipse supports only {@value StandardLocation#CLASS_OUTPUT} and
     * {@value StandardLocation#SOURCE_OUTPUT}.
     * </p>
     */
    private Filer filer;

    /** The element util. */
    private Elements util;

    /** The type util. */
    private Types types;

    /** The project related data. */
    private ProjectInfo info;

    /**
     * <p>
     * Setup validator.
     * </p>
     * 
     * @param element
     * @param filer
     * @param util
     * @param info
     */
    final void initialize(Element element, Messager messager, Filer filer, Elements util, Types types, ProjectInfo info) {
        this.element = element;
        this.messager = messager;
        this.filer = filer;
        this.util = util;
        this.types = types;
        this.info = info;

        root: while (true) {
            switch (element.getKind()) {
            case CLASS:
            case ANNOTATION_TYPE:
            case ENUM:
            case INTERFACE:
                break root;

            default:
                element = element.getEnclosingElement();
                break;
            }
        }
        this.root = (TypeElement) element;
    }

    /**
     * <p>
     * Validate an annotation value.
     * </p>
     * 
     * @param annotation An target annotation to validate.
     */
    protected abstract void validate(A annotation);

    /**
     * <p>
     * Returns the fully qualified class name of the annotated element.
     * </p>
     * 
     * @return
     */
    protected final String getClassName() {
        return root.toString();
    }

    /**
     * <p>
     * Detecte the root element is sub class of the specified class or not.
     * </p>
     * 
     * @param type
     * @return
     */
    protected final boolean isSubClassOf(Class type) {
        return types.isSubtype(root.asType(), util.getTypeElement(type.getName()).asType());
    }

    /**
     * <p>
     * Compute source file path.
     * </p>
     * 
     * @return A path to source file.
     */
    protected final Path getSourceFile() {
        for (Path directory : info.getSources()) {
            Path source = directory.resolve(getClassName().replace('.', '/').concat(".java"));

            if (Files.exists(source)) {
                return source;
            }
        }

        // If this exception will be thrown, it is bug of this program. So we must rethrow the
        // wrapped error in here.
        throw new Error();
    }

    /**
     * <p>
     * Returns the text of the documentation ("Javadoc") comment of an element.
     * </p>
     * <p>
     * A documentation comment of an element is a comment that begins with "/**" , ends with a
     * separate "* /", and immediately precedes the element, ignoring white space. Therefore, a
     * documentation comment contains at least three"*" characters. The text returned for the
     * documentation comment is a processed form of the comment as it appears in source code. The
     * leading "/**" and trailing "* /" are removed. For lines of the comment starting after the
     * initial "/**", leading white space characters are discarded as are any consecutive "*"
     * characters appearing after the white space or starting the line. The processed lines are then
     * concatenated together (including line terminators) and returned.
     * </p>
     * 
     * @return A documentation comment of the source code, or empty string if there is none
     */
    protected final String getDocument() {
        String doc = util.getDocComment(element);

        return doc == null ? "" : doc.trim();
    }

    /**
     * <p>
     * Talk to user.
     * </p>
     * 
     * @param messages Your message.
     */
    protected final void notice(Object... messages) {
        messager.printMessage(NOTE, build(messages), element);
    }

    /**
     * <p>
     * Warn to user.
     * </p>
     * 
     * @param messages Your warning message.
     */
    protected final void warn(Object... messages) {
        messager.printMessage(WARNING, build(messages), element);
    }

    /**
     * <p>
     * Declare a state of emergency.
     * </p>
     * 
     * @param message Your emergency message.
     */
    protected final void error(Object... messages) {
        messager.printMessage(ERROR, build(messages), element);
    }

    /**
     * <p>
     * Helper method to build message.
     * </p>
     * 
     * @param messages Your messages.
     * @return A combined message.
     */
    private String build(Object... messages) {
        StringBuilder builder = new StringBuilder();
        build(builder, messages);

        int length = builder.length();

        if (length != 0 && builder.charAt(length - 1) != '\r') {
            builder.append(EOL);
        }
        return builder.toString();
    }

    /**
     * <p>
     * Helper method to build message.
     * </p>
     * 
     * @param builder A message builder.
     * @param messages Your messages.
     */
    private void build(StringBuilder builder, Object... messages) {
        for (Object message : messages) {
            if (message == null) {
                builder.append("null");
            } else {
                Class type = message.getClass();

                if (type.isArray()) {
                    buildArray(builder, type.getComponentType(), message);
                } else if (CharSequence.class.isAssignableFrom(type)) {
                    builder.append((CharSequence) message);
                } else if (Throwable.class.isAssignableFrom(type)) {
                    buildError(builder, (Throwable) message);
                } else {
                    builder.append(I.transform(message, String.class));
                }
            }
        }
    }

    /**
     * <p>
     * Helper method to build message from various array type.
     * </p>
     * 
     * @param builder A message builder.
     * @param type A array type.
     * @param array A message array.
     */
    private void buildArray(StringBuilder builder, Class type, Object array) {
        if (type == int.class) {
            builder.append(Arrays.toString((int[]) array));
        } else if (type == long.class) {
            builder.append(Arrays.toString((long[]) array));
        } else if (type == float.class) {
            builder.append(Arrays.toString((float[]) array));
        } else if (type == double.class) {
            builder.append(Arrays.toString((double[]) array));
        } else if (type == boolean.class) {
            builder.append(Arrays.toString((boolean[]) array));
        } else if (type == char.class) {
            builder.append(Arrays.toString((char[]) array));
        } else if (type == byte.class) {
            builder.append(Arrays.toString((byte[]) array));
        } else if (type == short.class) {
            builder.append(Arrays.toString((short[]) array));
        } else {
            build(builder, (Object[]) array);
        }
    }

    /**
     * <p>
     * Build error message.
     * </p>
     * 
     * @param builder A message builder.
     * @param throwable An error message.
     */
    private void buildError(StringBuilder builder, Throwable throwable) {
        StringWriter writer = new StringWriter();

        throwable.printStackTrace(new PrintWriter(writer));

        builder.append(writer.toString());
    }
}
