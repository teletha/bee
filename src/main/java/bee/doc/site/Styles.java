/*
 * Copyright (C) 2019 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.doc.site;

import stylist.AbstractStyleDSL;
import stylist.Style;
import stylist.property.Background.BackgroundImage;
import stylist.value.Color;

/**
 * 
 */
public class Styles extends AbstractStyleDSL implements BaseStyle {

    // =====================================================
    // HTML Elements
    // =====================================================

    public static Style HTML = Style.named("html", () -> {
        font.size(14, px);
        scroll.smooth().padding.top(HeaderHeight);
        text.wordBreak.breakAll();

        $.select("*[class|=el]", () -> {
            font.size(1, rem);
        });
    });

    public static Style HTMLItalic = Style.named("i", () -> {
        font.style.normal();
    });

    public static Style HTMLAnchor = Style.named("a", () -> {
        font.color(Color.Inherit);
        text.decoration.none();
        cursor.pointer();

        $.hover(() -> {
            text.decoration.underline();
            text.decorationColor.color(palette.font.opacify(-0.5));
            text.underlineOffset.length(4, px);
            text.underlinePosition.under();
        });
    });

    public static Style JavadocComment = () -> {
        $.select("p", () -> {
            block();
        });

        $.select("dl", () -> {
            block();
        });

        $.select("ul", () -> {
            block();
            listStyle.inside();
        });

        $.select("ol", () -> {
            block();
            listStyle.inside();
        });

        $.select("pre", () -> {
            block();
            font.family(fonts.base);
            padding.size(0.4, rem);
            border.radius(4, px);
        });

        $.select("blockquote", () -> {
            block();
        });
    };

    private static void block() {
        margin.bottom(0.8, rem);
    }

    /**
     * Define block-like.
     * 
     * @param color
     */
    public static void block(Color color, boolean paintBackground) {
        margin.left(0, px);
        padding.vertical(BaseStyle.BlockVerticalGap).horizontal(BaseStyle.BlockHorizontalGap);
        border.left.width(BaseStyle.BlockBorderWidth).solid().color(color);
        line.height(LineHeight);
        font.family(fonts.base);
        if (paintBackground) background.color(color.opacify(-0.8d));
    }

    /**
     * Define block-like.
     * 
     * @param color
     */
    public static void block2(Color color, boolean paintBackground) {
        padding.vertical(BaseStyle.BlockVerticalGap).horizontal(BaseStyle.BlockHorizontalGap);
        border.left.width(BaseStyle.BlockBorderWidth).solid().color(color);
        border.radius(2, px);
        line.height(LineHeight);
        font.family(fonts.base);
        position.relative();
        if (paintBackground) background.color(color.opacify(-0.8d));

        $.before(() -> {
            position.absolute();
            content.text("Signature");
            font.color(color.opacify(-0.9d)).size(1.6, rem);
        });
    }

    public static final Style SignatureParameterPart = () -> {
        font.color(palette.font.lighten(18));
    };

    public static final Style HTMLClassParameters = Style.named(".parameters", () -> {
        $.before(() -> {
            content.text("<");
        });
        $.after(() -> {
            content.text(">");
        });
    });

    public static final Style HTMLClassExtends = Style.named(".extends", () -> {
        $.before(() -> {
            content.text(" extends ");
        });
    });

    public static final Style HTMLClassSuper = Style.named(".super", () -> {
        $.before(() -> {
            content.text(" super ");
        });
    });

    public static final Style HTMLClassArray = Style.named("i[array=fix]", () -> {
        $.after(() -> {
            content.text("[]");
        });
    });

    public static final Style HTMLClassVarParam = Style.named("i[array=var]", () -> {
        $.after(() -> {
            content.text("...");
        });
    });

    public static final Style HTMLClassPackage = Style.named(".package", () -> {
        font.weight.bold();
    });

    public static final Style HTMLClassTypeInterface = Style.named(".Interface", () -> {
        buildMark("I", Color.rgb(128, 88, 165), false, true);
    });

    public static final Style HTMLClassTypeFunctionalInterface = Style.named(".Functional", () -> {
        buildMark("F", Color.rgb(128, 88, 165), false, true);
    });

    public static final Style HTMLClassTypeAbstractClass = Style.named(".AbstractClass", () -> {
        buildMark("C", Color.rgb(50, 135, 92), false, true);
    });

    public static final Style HTMLClassTypeClass = Style.named(".Class", () -> {
        buildMark("C", Color.rgb(50, 135, 92), true, true);
    });

    public static final Style HTMLClassTypeEnum = Style.named(".Enum", () -> {
        buildMark("E", Color.hsl(0, 29, 49), true, false);
    });

    public static final Style HTMLClassTypeAnnotation = Style.named(".Annotation", () -> {
        buildMark("A", palette.primary, true, false);
    });

    public static final Style HTMLClassTypeException = Style.named(".Exception", () -> {
        buildMark("T", palette.accent, true, false);
    });

    private static void buildMark(String mark, Color color, boolean fill, boolean circle) {
        $.before(() -> {
            display.inlineBlock().width(15, px).height(15, px);
            border.color(color).solid().width(1, px);
            margin.right(6, px);
            content.text(mark);
            text.align.center();
            line.height(13, px);
            font.size(10, px);

            if (circle) {
                border.radius(50, percent);
            } else {
                border.radius(3, px);
            }

            if (fill) {
                font.color(Color.White);
                background.color(color);
            } else {
                font.color(color);
            }
        });
    }

    public static final Style HTMLClassModifierPublic = Style.named(".PUBLIC", () -> {
        setMarkColor(Color.rgb(50, 135, 92));
    });

    public static final Style HTMLClassModifierProtected = Style.named(".PROTECTED", () -> {
        setMarkColor(palette.secondary);
    });

    public static final Style HTMLClassModifierPackagePrivate = Style.named(".PACKAGEPRIVATE", () -> {
        setMarkColor(palette.primary);
    });

    public static final Style HTMLClassModifierPrivate = Style.named(".PRIVATE", () -> {
        setMarkColor(palette.accent);
    });

    /** The circle icon. */
    private static final String circleStroked = "\\e836";

    /** The circle icon. */
    private static final String circleFilled = "\\e061"; // cirle filled big e3fa small e061

    /** The circle icon. */
    private static final String circlePointed = "\\e837"; // cirle filled big e3fa small e061

    /**
     * Assign mark color.
     * 
     * @param color
     */
    private static void setMarkColor(Color color) {
        position.relative();

        $.before(() -> {
            content.text("");
            display.inlineBlock().width(0.5, rem).height(0.5, rem);
            border.radius(50, percent);
            background.color(color);
            margin.right(0.6, rem);

            $.with(".OVERRIDE", () -> {
                // content.text("O");
            });
            //
            $.with(".FIELD", () -> {
                content.text(circleStroked);
            });
        });
    }

    public static final Style HTMLClassModifierStatic = Style.named(".STATIC", () -> {
        overlayAlphabetLeftTop("S");
    });

    public static final Style HTMLClassModifierDefault = Style.named(".DEFAULT", () -> {
        overlayAlphabetRightTop("D");
    });

    public static final Style HTMLClassModifierOverride = Style.named(".OVERRIDE", () -> {
    });

    public static final Style HTMLClassModifierSynchronized = Style.named(".SYNCHRONIZED", () -> {
        overlayIconRightBottom("\\e8ae");
    });

    public static final Style HTMLClassModifierTransient = Style.named(".TRANSIENT", () -> {
        overlayIconRightBottom("\\e14c");
    });

    public static final Style HTMLClassModifierAbstract = Style.named(".ABSTRACT", () -> {
        overlayAlphabetRightTop("A");
    });

    public static final Style HTMLClassModifierFinal = Style.named(".FINAL", () -> {
        overlayAlphabetRightTop("F");
    });

    public static final Style HTMLClassModifierVolatile = Style.named(".VOLATILE", () -> {
        overlayAlphabetRightTop("V");
    });

    /**
     * Create alphabetical mark.
     * 
     * @param mark
     */
    private static void overlayAlphabetRightTop(String mark) {
        position.relative();
        $.before(() -> {
            font.color(palette.primary).size(0.6, rem).family(RobotoMono);
            content.text(mark);
            position.absolute().top(-0.2, rem).left(0.7, rem);
        });
    }

    /**
     * Create alphabetical mark.
     * 
     * @param mark
     */
    private static void overlayAlphabetLeftTop(String mark) {
        position.relative();
        $.after(() -> {
            font.color(palette.accent).size(0.6, rem).family(RobotoMono);
            content.text(mark);
            position.absolute().top(-0.2, rem).left(0.3, rem);
        });
    }

    /**
     * Create icon mark.
     * 
     * @param mark
     */
    private static void overlayIconRightBottom(String mark) {
        position.relative();
        $.after(() -> {
            font.color(palette.primary).size(0.7, rem).family(fonts.icon);
            content.text(mark);
            position.absolute().top(0.5, rem).left(0.4, rem);
        });
    }

    public static final Style SelectBox = Style.named(".vs__dropdown-toggle", () -> {
        background.color(Color.White);
    });

    public static final Style CheckBox = Style.named(".pretty", () -> {
        display.block();
    });

    public static final Style InputBox = Style.named("#SearchByName", () -> {
        display.block().width(100, percent);
        background.color(Color.White);
        margin.vertical(8, px);
        padding.vertical(6, px).horizontal(8, px);
        border.color(Color.rgb(60, 60, 60, 0.26)).width(1, px).solid().radius(4, px);
    });

    public static final Style Tree = Style.named(".tree", () -> {
        overflow.hidden().scrollbar.thin();
        display.height(60, vh);
        background.color(Color.Inherit);

        $.hover(() -> {
            overflow.y.auto();
        });

        $.select("dt", () -> {
            font.weight.bold();
            cursor.pointer();
            padding.vertical(0.2, rem).horizontal(0.5, rem);
            border.radius(2, px);

            $.hover(() -> {
                background.color(Color.White).image(BackgroundImage.inherit()).repeat();
            });
        });

        $.select("dd", () -> {
            padding.vertical(0.1, rem).left(1, rem);
            cursor.pointer();
            border.radius(2, px);

            $.hover(() -> {
                background.color(Color.White).image(BackgroundImage.inherit()).repeat();
            });
        });
    });
}