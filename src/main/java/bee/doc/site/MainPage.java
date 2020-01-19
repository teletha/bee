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

import bee.doc.ClassInfo;
import bee.doc.Javadoc;
import bee.doc.builder.HTML;
import stylist.Style;
import stylist.StyleDSL;
import stylist.property.Background.BackgroundImage;
import stylist.value.Color;
import stylist.value.Numeric;

/**
 * 
 */
public class MainPage extends HTML {

    protected final ClassInfo info;

    protected final Javadoc javadoc;

    /**
     * @param info
     */
    public MainPage(Javadoc javadoc, ClassInfo info) {
        this.info = info;
        this.javadoc = javadoc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void declare() {
        $("html", () -> {
            $("head", () -> {
                $("meta", attr("charset", "UTF-8"));
                $("title", text(javadoc.productName() + " API"));
                script("https://unpkg.com/vue/dist/vue.js");
                script("https://unpkg.com/vue-router/dist/vue-router.js");
                stylesheet("https://cdn.jsdelivr.net/npm/pretty-checkbox@3.0/dist/pretty-checkbox.min.css");
                script("https://cdn.jsdelivr.net/npm/pretty-checkbox-vue@1.1/dist/pretty-checkbox-vue.min.js");
                stylesheet("https://unpkg.com/vue-select@3.4.0/dist/vue-select.css");
                script("https://unpkg.com/vue-select@3.4.0");
                script("https://cdn.jsdelivr.net/gh/google/code-prettify@master/loader/run_prettify.js");
                stylesheet("/main.css");
            });
            $("body", Styles.workbench, () -> {
                // =============================
                // Top Navigation
                // =============================
                $("header", Styles.HeaderArea, () -> {
                    $("h1", Styles.HeaderTitle, text(javadoc.productName() + " API"));
                });

                $("main", Styles.MainArea, () -> {
                    // =============================
                    // Left Side Navigation
                    // =============================
                    $("nav", Styles.TypeNavigation, () -> {
                        $("div");
                    });

                    // =============================
                    // Main Contents
                    // =============================
                    $("article", Styles.Contents, () -> {
                        $("router-view");
                        if (info != null) {
                            $(new ContentsView(info));
                        }
                    });

                    // =============================
                    // Right Side Navigation
                    // =============================
                    $("aside", Styles.SubNavigation, () -> {
                        $("div", Styles.SubNavigationStickyBlock, () -> {
                            if (info != null) {
                                $(new SubNavigationView(info));
                            }
                        });
                    });
                });

                script("root.js", javadoc.data);
                script("main.js");
            });
        });
    }

    /**
     * Style definition.
     */
    private interface Styles extends StyleDSL, BaseStyle {

        Numeric LeftNavigationWidth = Numeric.of(15, vw);

        Style workbench = () -> {
            background.color(Color.rgb(235, 246, 247)).image(BackgroundImage.drawSlash(Color.rgb(220, 222, 225, 0.7), 3)).repeat();
            font.size(FontSize).family(fonts.base).color(palette.font);
            line.height(LineHeight);
            display.width(100, vw);
        };

        Style HeaderArea = () -> {
            background.color(Color.Inherit).image(BackgroundImage.inherit()).repeat();
            position.sticky().top(0, rem);
            display.width(MaxWidth).height(HeaderHeight).zIndex(10).flex();
            margin.auto();
            border.bottom.color(palette.primary).width(1, px).solid();
        };

        Style HeaderTitle = () -> {
            font.size(2.5, rem).family(fonts.title).weight.normal().color(palette.primary);
            flexItem.alignSelf.center();
        };

        Style MainArea = () -> {
            display.width(MaxWidth).flex().direction.row();
            margin.auto();
        };

        Style TypeNavigation = () -> {
            flexItem.basis(LeftNavigationWidth).shrink(0);

            $.child(() -> {
                position.sticky().top(HeaderHeight);

                $.child(() -> {
                    margin.top(BaseStyle.BlockVerticalGap);
                });
            });

            $.select(".el-select", () -> {
                display.width(100, percent);
            });

            $.select(".el-checkbox", () -> {
                display.block();
            });
        };

        Style Contents = () -> {
            flexItem.grow(1);
            margin.left(5, rem).right(1.5, rem);
        };

        Style SubNavigation = () -> {
            flexItem.basis(RightNavigationWidth).shrink(0);
        };

        Style SubNavigationStickyBlock = () -> {
            position.sticky().top(HeaderHeight);
            display.block().height(Numeric.of(80, vh).subtract(HeaderHeight)).maxWidth(RightNavigationWidth);
            overflow.auto().scrollbar.thin();
            text.whiteSpace.nowrap();

            $.hover(() -> {
                overflow.y.auto();
            });

            $.child().child(() -> {
                padding.vertical(0.15, em);
            });
        };
    }
}