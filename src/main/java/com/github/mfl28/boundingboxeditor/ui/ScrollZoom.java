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

/**
 * Computes how much a scroll event zooms the editor's image: in proportion to the scrolled distance, so that both
 * a mouse-wheel notch and a trackpad gesture (many small events) zoom evenly.
 */
final class ScrollZoom {
    // A mouse-wheel notch typically scrolls 3 lines, i.e. zooms by about 12 %.
    static final double ZOOM_FACTOR_PER_SCROLL_LINE = 1.04;
    static final double MAXIMUM_ZOOM_FACTOR_PER_EVENT = 1.5;
    private static final double DEFAULT_SCROLL_LINE_HEIGHT = 40.0 / 3;

    private ScrollZoom() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Computes the zoom factor of one scroll event.
     *
     * @param deltaY      the scrolled vertical distance in pixels (positive: zoom in)
     * @param multiplierY the pixels per scrolled line (0 if unknown)
     * @return the factor to multiply the image size with
     */
    static double computeZoomFactor(double deltaY, double multiplierY) {
        final double lineHeight = multiplierY > 0 ? multiplierY : DEFAULT_SCROLL_LINE_HEIGHT;
        final double zoomFactor = Math.pow(ZOOM_FACTOR_PER_SCROLL_LINE, deltaY / lineHeight);

        return Math.clamp(zoomFactor, 1 / MAXIMUM_ZOOM_FACTOR_PER_EVENT, MAXIMUM_ZOOM_FACTOR_PER_EVENT);
    }
}
