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
package com.github.mfl28.boundingboxeditor.ui.statusevents;

import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;

public class BoundingBoxPredictionSuccessfulEvent extends StatusEvent {
    /**
     * Creates a new status-event signifying the successful completion of a bounding box prediction.
     *
     * @param ioResult an {@link IOResult} object containing information about the import-operation
     */
    public BoundingBoxPredictionSuccessfulEvent(BoundingBoxPredictionResult ioResult) {
        super(createMessage(ioResult));
    }

    private static String createMessage(BoundingBoxPredictionResult ioResult) {
        int predictedBoundingBoxes = ioResult.getImageAnnotationData()
                                             .categoryNameToBoundingShapeCountMap()
                                             .values()
                                             .stream()
                                             .reduce(Integer::sum)
                                             .orElse(0);

        final int nrImages = ioResult.getNrSuccessfullyProcessedItems();
        final int nrDuplicates = ioResult.getNrSkippedDuplicates();
        final String imagesAndTime = nrImages + " image" + (nrImages != 1 ? "s" : "") + " in "
                + secondsFormat.format(ioResult.getTimeTakenInMilliseconds() / 1000.0) + " sec";

        if(predictedBoundingBoxes == 0) {
            return nrDuplicates == 0
                    ? "No bounding boxes predicted for " + imagesAndTime + "."
                    : "No new bounding boxes for " + imagesAndTime + ": "
                    + (nrDuplicates == 1 ? "the predicted box duplicates an existing shape."
                    : "all " + nrDuplicates + " predicted boxes duplicate existing shapes.");
        }

        return "Successfully predicted " + predictedBoundingBoxes + " bounding box"
                + (predictedBoundingBoxes != 1 ? "es" : "") + " for " + imagesAndTime + "."
                + (nrDuplicates != 0 ? " Skipped " + nrDuplicates + " duplicate" + (nrDuplicates != 1 ? "s" : "")
                + " of existing shapes." : "");
    }
}
