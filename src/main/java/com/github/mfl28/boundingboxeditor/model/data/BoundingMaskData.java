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

import com.github.mfl28.boundingboxeditor.ui.BoundingMaskView;
import com.github.mfl28.boundingboxeditor.ui.BoundingShapeViewable;

import java.util.List;
import java.util.Objects;

/**
 * Represents an object annotated with a pixel mask (instance segmentation). Unlike boxes and polygons, whose
 * coordinates are relative, the mask lies on the pixel grid of the image (see {@link MaskBitmap}).
 */
public final class BoundingMaskData extends BoundingShapeData {
    // Only the COCO and PNG mask formats contain masks; transient keeps it out of reflection-based serialization.
    private final transient MaskBitmap mask;

    public BoundingMaskData(ObjectCategory category, MaskBitmap mask, List<String> tags) {
        super(category, tags);
        this.mask = Objects.requireNonNull(mask);
    }

    public MaskBitmap getMask() {
        return mask;
    }

    /**
     * Returns the mask on the pixel grid of an image of the provided size, e.g. of the image the annotation
     * belongs to, if the mask was copied from an image of a different size.
     *
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     * @return the mask
     */
    public MaskBitmap getMaskForImageSize(int imageWidth, int imageHeight) {
        return mask.resizedTo(imageWidth, imageHeight);
    }

    @Override
    public BoundingShapeViewable toBoundingShapeView(double imageWidth, double imageHeight) {
        return BoundingMaskView.fromData(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mask);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(!(o instanceof BoundingMaskData that)) {
            return false;
        }

        return super.equals(o) && mask.equals(that.mask);
    }
}
