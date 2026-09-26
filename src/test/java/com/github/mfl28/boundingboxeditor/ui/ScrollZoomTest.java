/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.ui;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class ScrollZoomTest {
    private static final double LINE_HEIGHT = 40.0 / 3;

    @Test
    void onScroll_ShouldZoomInProportionToTheScrolledDistance() {
        // A mouse-wheel notch (3 lines) zooms by the same amount as the same distance in many small steps.
        final double oneNotch = ScrollZoom.computeZoomFactor(3 * LINE_HEIGHT, LINE_HEIGHT);
        final double tenSmallSteps = Math.pow(ScrollZoom.computeZoomFactor(0.3 * LINE_HEIGHT, LINE_HEIGHT), 10);

        assertEquals(Math.pow(ScrollZoom.ZOOM_FACTOR_PER_SCROLL_LINE, 3), oneNotch, 1e-9);
        assertEquals(oneNotch, tenSmallSteps, 1e-9);
        // Scrolling back by the same distance restores the size.
        assertEquals(1.0, oneNotch * ScrollZoom.computeZoomFactor(-3 * LINE_HEIGHT, LINE_HEIGHT), 1e-9);
        assertEquals(1.0, ScrollZoom.computeZoomFactor(0, LINE_HEIGHT), 1e-9);
    }

    @Test
    void onScroll_ShouldLimitTheZoomOfASingleEvent() {
        assertEquals(ScrollZoom.MAXIMUM_ZOOM_FACTOR_PER_EVENT, ScrollZoom.computeZoomFactor(1e4, LINE_HEIGHT), 1e-9);
        assertEquals(1 / ScrollZoom.MAXIMUM_ZOOM_FACTOR_PER_EVENT, ScrollZoom.computeZoomFactor(-1e4, LINE_HEIGHT),
                     1e-9);
    }

    @Test
    void onScroll_WhenLineHeightUnknown_ShouldUseTheDefault() {
        assertEquals(ScrollZoom.computeZoomFactor(40, LINE_HEIGHT), ScrollZoom.computeZoomFactor(40, 0), 1e-9);
    }
}
