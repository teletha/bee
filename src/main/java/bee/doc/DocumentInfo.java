/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor9;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocRootTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTypeTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.IndexTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.SummaryTree;
import com.sun.source.doctree.SystemPropertyTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.doctree.VersionTree;
import com.sun.source.util.SimpleDocTreeVisitor;

import bee.doc.site.Styles;
import kiss.I;
import kiss.Variable;
import kiss.XML;
import kiss.Ⅱ;

public class DocumentInfo {

    /** The associated element. */
    protected final Element e;

    protected final Variable<XML> comment = Variable.empty();

    /** Tag info. */
    protected final List<Ⅱ<String, XML>> typeParameterTags = new ArrayList();

    /** Tag info. */
    protected final List<Ⅱ<String, XML>> paramTags = new ArrayList();

    /** Tag info. */
    protected final List<Ⅱ<String, XML>> throwsTags = new ArrayList();

    /** Tag info. */
    protected final List<XML> authorTags = new ArrayList();

    /** Tag info. */
    protected final List<XML> seeTags = new ArrayList();

    /** Tag info. */
    protected final List<XML> sinceTags = new ArrayList();

    /** Tag info. */
    protected final List<XML> versionTags = new ArrayList();

    /** Tag info. */
    protected final Variable<XML> returnTag = Variable.empty();

    private final TypeResolver resolver;

    protected DocumentInfo(Element e, TypeResolver resolver) {
        this.e = e;
        this.resolver = resolver;

        try {
            DocCommentTree docs = DocTool.DocUtils.getDocCommentTree(e);
            if (docs != null) {
                comment.set(xml(docs.getFullBody()));
                comment.to(x -> x.addClass(Styles.JavadocComment.className()));
                docs.getBlockTags().forEach(tag -> tag.accept(new TagScanner(), this));
            }
        } catch (Throwable error) {
            error.printStackTrace();
        }
    }

    /**
     * Create comment element.
     * 
     * @return
     */
    public final XML createComment() {
        return comment.isAbsent() ? null : comment.v.clone();
    }

    /**
     * Parse {@link TypeMirror} and build its XML expression.
     * 
     * @param type A target type.
     * @return New XML expression.
     */
    protected final XML parseTypeAsXML(TypeMirror type) {
        return new TypeXMLBuilder().parse(type).parent().children();
    }

    /**
     * Test visibility of the specified {@link Element}.
     * 
     * @param e
     * @return
     */
    protected final boolean isVisible(Element e) {
        Set<Modifier> modifiers = e.getModifiers();
        return modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED);
    }

    /**
     * Find comment.
     * 
     * @param name
     * @return
     */
    protected final XML findParamTagBy(String name) {
        for (Ⅱ<String, XML> param : paramTags) {
            if (param.ⅰ.equals(name)) {
                return param.ⅱ;
            }
        }
        return null;
    }

    /**
     * Find comment.
     * 
     * @param name
     * @return
     */
    protected final XML findThrowsTagBy(String name) {
        for (Ⅱ<String, XML> param : throwsTags) {
            if (param.ⅰ.equals(name)) {
                return param.ⅱ;
            }
        }
        return null;
    }

    /**
     * Find comment.
     * 
     * @param name
     * @return
     */
    protected final XML findTypeVariableTagBy(String name) {
        for (Ⅱ<String, XML> param : typeParameterTags) {
            if (param.ⅰ.equals(name)) {
                return param.ⅱ;
            }
        }
        return null;
    }

    /**
     * @param docs Documents.
     * @return
     */
    private XML xml(List<? extends DocTree> docs) {
        XML x = new DocumentXMLBuilder().parse(docs).build();
        return x;
    }

    /**
     * Create empty node.
     * 
     * @return
     */
    private XML emptyXML() {
        return null;
    }

    private static final Pattern TestPattern = Pattern.compile("(\\S+Test)#(\\S+)\\(.*\\)");

    /**
     * 
     */
    private class TagScanner extends SimpleDocTreeVisitor<DocumentInfo, DocumentInfo> {

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentInfo visitAuthor(AuthorTree node, DocumentInfo p) {
            authorTags.add(xml(node.getName()));
            return p;
        }

        /**
         * @inheritDoc
         */
        @Override
        public DocumentInfo visitParam(ParamTree node, DocumentInfo p) {
            Ⅱ<String, XML> pair = I.pair(node.getName().toString(), xml(node.getDescription()));

            if (node.isTypeParameter()) {
                typeParameterTags.add(pair);
            } else {
                paramTags.add(pair);
            }
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentInfo visitReturn(ReturnTree node, DocumentInfo p) {
            returnTag.set(xml(node.getDescription()));
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentInfo visitSee(SeeTree node, DocumentInfo p) {
            XML reference = xml(node.getReference());

            Matcher matcher = TestPattern.matcher(reference.text());
            if (matcher.matches()) {
                String testClassName = matcher.group(1);
                String testMethodName = matcher.group(2);

                String testClassPath = DocTool.ElementUtils.getPackageOf(e)
                        .getQualifiedName()
                        .toString()
                        .replace('.', '/') + "/" + testClassName + ".java";
                System.out.println(testClassPath);
            } else {
                seeTags.add(reference);
            }
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentInfo visitSince(SinceTree node, DocumentInfo p) {
            sinceTags.add(xml(node.getBody()));
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentInfo visitThrows(ThrowsTree node, DocumentInfo p) {
            throwsTags.add(I.pair(node.getExceptionName().toString(), xml(node.getDescription())));
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentInfo visitVersion(VersionTree node, DocumentInfo p) {
            versionTags.add(xml(node.getBody()));
            return p;
        }
    }

    /**
     * 
     */
    private class DocumentXMLBuilder extends SimpleDocTreeVisitor<DocumentXMLBuilder, DocumentXMLBuilder> {

        private StringBuilder text = new StringBuilder();

        /**
         * Parse documetation.
         * 
         * @param docs
         * @return
         */
        private DocumentXMLBuilder parse(List<? extends DocTree> docs) {
            for (DocTree doc : docs) {
                doc.accept(this, this);
            }
            return this;
        }

        /**
         * Build XML fragmentation.
         * 
         * @return
         */
        private XML build() {
            try {
                if (text.length() == 0) {
                    return emptyXML();
                } else {
                    // Since Javadoc text is rarely correct HTML, switch by inserting dock type
                    // declarations to use the tag soup parser instead of the XML parser.
                    text.insert(0, "<!DOCTYPE span><span>").append("</span>");

                    // sanitize script and css
                    XML xml = I.xml(text);
                    xml.find("link").remove();
                    xml.find("pre").addClass("prettyprint");

                    return xml;
                }
            } catch (Exception e) {
                throw new Error(e.getMessage() + " [" + text.toString() + "]", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitAttribute(AttributeTree node, DocumentXMLBuilder p) {
            text.append(' ').append(node.getName()).append("=\"");
            node.getValue().forEach(n -> n.accept(this, this));
            text.append("\"");
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitComment(CommentTree node, DocumentXMLBuilder p) {
            return super.visitComment(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitDocRoot(DocRootTree node, DocumentXMLBuilder p) {
            return super.visitDocRoot(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitDocType(DocTypeTree node, DocumentXMLBuilder p) {
            return super.visitDocType(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitEndElement(EndElementTree node, DocumentXMLBuilder p) {
            text.append("</").append(node.getName()).append('>');
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitEntity(EntityTree node, DocumentXMLBuilder p) {
            return super.visitEntity(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitErroneous(ErroneousTree node, DocumentXMLBuilder p) {
            return super.visitErroneous(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitIdentifier(IdentifierTree node, DocumentXMLBuilder p) {
            return super.visitIdentifier(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitIndex(IndexTree node, DocumentXMLBuilder p) {
            return super.visitIndex(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitInheritDoc(InheritDocTree node, DocumentXMLBuilder p) {
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitLink(LinkTree node, DocumentXMLBuilder p) {
            String reference = node.getReference().toString();
            String label = reference;
            String memberName = "";

            int index = reference.indexOf("#");
            if (index == 0) {
                memberName = reference;
                reference = resolver.resolveDocumentLocation(ModelUtil.getTopLevelTypeElement(e));
            } else if (index != -1) {
                memberName = reference.substring(index);
                reference = resolver.resolveDocumentLocation(reference.substring(0, index));
            } else {
                reference = resolver.resolveDocumentLocation(reference);
            }

            if (reference == null) {
                text.append(label);
            } else {
                text.append("<a href='").append(reference).append(memberName).append("'>").append(label).append("</a>");
            }
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitLiteral(LiteralTree node, DocumentXMLBuilder p) {
            text.append(escape(node.getBody().getBody()));
            return p;
        }

        /**
         * Escape text for XML.
         * 
         * @param text
         * @return
         */
        private String escape(String text) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                case '<':
                    buffer.append("&lt;");
                    break;
                case '>':
                    buffer.append("&gt;");
                    break;
                case '\"':
                    buffer.append("&quot;");
                    break;
                case '&':
                    buffer.append("&amp;");
                    break;
                case '\'':
                    buffer.append("&apos;");
                    break;
                default:
                    if (c > 0x7e) {
                        buffer.append("&#" + ((int) c) + ";");
                    } else
                        buffer.append(c);
                }
            }
            return buffer.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitReference(ReferenceTree node, DocumentXMLBuilder p) {
            text.append(node.getSignature());
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitStartElement(StartElementTree node, DocumentXMLBuilder p) {
            text.append("<").append(node.getName());
            node.getAttributes().forEach(attr -> attr.accept(this, this));
            text.append(node.isSelfClosing() ? "/>" : ">");
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitSummary(SummaryTree node, DocumentXMLBuilder p) {
            return super.visitSummary(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitSystemProperty(SystemPropertyTree node, DocumentXMLBuilder p) {
            return super.visitSystemProperty(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitText(TextTree node, DocumentXMLBuilder p) {
            text.append(node.getBody());
            return p;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitUnknownInlineTag(UnknownInlineTagTree node, DocumentXMLBuilder p) {
            return super.visitUnknownInlineTag(node, p);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DocumentXMLBuilder visitValue(ValueTree node, DocumentXMLBuilder p) {
            return super.visitValue(node, p);
        }
    }

    /**
     * 
     */
    private class TypeXMLBuilder extends SimpleTypeVisitor9<XML, XML> {

        /**
         * Parse documetation.
         * 
         * @param docs
         * @return
         */
        private XML parse(TypeMirror type) {
            XML root = I.xml("<i/>");
            type.accept(this, root);
            return root;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitIntersection(IntersectionType t, XML xml) {
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitUnion(UnionType t, XML xml) {
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitPrimitive(PrimitiveType primitive, XML xml) {
            xml.text(primitive.toString());
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitNull(NullType t, XML xml) {
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitArray(ArrayType array, XML xml) {
            xml.attr("array", "fix");
            array.getComponentType().accept(this, xml);
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitDeclared(DeclaredType declared, XML xml) {
            // link to type
            TypeElement type = (TypeElement) declared.asElement();
            String name = type.getSimpleName().toString();
            String uri = resolver.resolveDocumentLocation(type);

            if (uri != null) {
                xml.append(I.xml("a").attr("href", uri).text(name));
            } else {
                xml.text(type.getQualifiedName().toString());
            }

            // type parameter
            List<? extends TypeMirror> paramTypes = declared.getTypeArguments();
            if (paramTypes.isEmpty() == false) {
                XML parameters = I.xml("<i class='parameters'/>");
                for (int i = 0, size = paramTypes.size(); i < size; i++) {
                    parameters.append(parseTypeAsXML(paramTypes.get(i)));

                    if (i + 1 != size) {
                        parameters.append(", ");
                    }
                }
                xml.append(parameters);
            }

            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitError(ErrorType t, XML xml) {
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitTypeVariable(TypeVariable variable, XML xml) {
            xml.text(variable.toString());
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitWildcard(WildcardType wildcard, XML xml) {
            TypeMirror bounded = wildcard.getExtendsBound();
            if (bounded != null) {
                xml.text("?");
                xml.after("<i class='extends'/>").next().append(parseTypeAsXML(bounded));
                return xml;
            }

            bounded = wildcard.getSuperBound();
            if (bounded != null) {
                xml.text("?");
                xml.after("<i class='super'/>").next().append(parseTypeAsXML(bounded));
                return xml;
            }

            xml.text("?");
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitExecutable(ExecutableType t, XML xml) {
            return xml;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public XML visitNoType(NoType no, XML xml) {
            switch (no.getKind()) {
            case VOID:
                xml.text("void");
                break;

            default:
            }
            return xml;
        }
    }
}
