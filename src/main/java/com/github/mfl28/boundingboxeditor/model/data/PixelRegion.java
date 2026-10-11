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
package com.github.mfl28.boundingboxeditor.model.data;

/**
 * A rectangle of pixels, e.g. the part of a mask that may have changed.
 *
 * @param minX the first column
 * @param minY the first row
 * @param maxX the column after the last one
 * @param maxY the row after the last one
 */
public record PixelRegion(int minX, int minY, int maxX, int maxY) {
    /**
     * Returns the smallest region that contains this one and another one.
     *
     * @param other the other region
     * @return the union
     */
    public PixelRegion union(PixelRegion other) {
        return new PixelRegion(Math.min(minX, other.minX), Math.min(minY, other.minY), Math.max(maxX, other.maxX),
                               Math.max(maxY, other.maxY));
    }
}
