/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.io.results;

import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;

import java.util.List;

public class BoundingBoxPredictionResult extends IOResult {
    private final ImageAnnotationData imageAnnotationData;

    /**
     * Creates a new bounding box prediction result.
     *
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     *                                     about where and which errors occurred.
     * @param imageAnnotationData          the predicted annotation data
     */
    public BoundingBoxPredictionResult(int nrSuccessfullyProcessedItems,
                                       List<IOErrorInfoEntry> errorTableEntries,
                                       ImageAnnotationData imageAnnotationData) {
        super(OperationType.BOUNDING_BOX_PREDICTION, nrSuccessfullyProcessedItems, errorTableEntries);
        this.imageAnnotationData = imageAnnotationData;
    }

    public ImageAnnotationData getImageAnnotationData() {
        return imageAnnotationData;
    }
}
