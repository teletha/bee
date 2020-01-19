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

import static stylist.StyleDSL.*;

import stylist.value.ColorPalette;
import stylist.value.Font;
import stylist.value.FontPalette;
import stylist.value.Numeric;

public interface BaseStyle {

    ColorPalette palette = ColorPalette.with.primary($.rgb(22, 94, 131))
            .secondary($.rgb(250, 210, 50))
            .accent($.rgb(221, 81, 76))
            .background($.rgb(241, 250, 238))
            .font($.rgb(69, 69, 79));

    Font Roboto = Font.fromGoogle("Roboto");

    Font RobotoMono = Font.fromGoogle("Roboto Mono");

    FontPalette fonts = FontPalette.with.base(Roboto).title(Font.fromGoogle("Ubuntu")).monoBySystem();

    Numeric FontSize = Numeric.of(12, px);

    Numeric MaxWidth = Numeric.of(85, vw);

    Numeric SmallGap = Numeric.of(1, px);

    Numeric BlockVerticalGap = Numeric.of(0.5, rem);

    Numeric BlockBorderWidth = Numeric.of(3, px);

    Numeric BlockHorizontalGap = Numeric.of(10, px);

    Numeric HeaderHeight = Numeric.of(80, px);

    Numeric RightNavigationWidth = Numeric.of(20, vw);

    double LineHeight = 1.5;

}
