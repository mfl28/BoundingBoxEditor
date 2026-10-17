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

import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class BoundingBoxPredictionSuccessfulEventTest {
    @Test
    void onPredictedBoxes_ShouldReportTheBoxesAndSkippedDuplicates() {
        assertEquals("Successfully predicted 3 bounding boxes for 2 images in 1.5 sec.", message(2, 3, 0));
        assertEquals("Successfully predicted 1 bounding box for 1 image in 1.5 sec. Skipped 1 duplicate of "
                             + "existing shapes.", message(1, 1, 1));
        assertEquals("Successfully predicted 3 bounding boxes for 2 images in 1.5 sec. Skipped 4 duplicates of "
                             + "existing shapes.", message(2, 3, 4));
    }

    @Test
    void onNoNewBoxes_ShouldNotReportASuccessfulPrediction() {
        assertEquals("No bounding boxes predicted for 1 image in 1.5 sec.", message(1, 0, 0));
        assertEquals("No new bounding boxes for 4 images in 1.5 sec: all 28 predicted boxes duplicate existing "
                             + "shapes.", message(4, 0, 28));
        assertEquals("No new bounding boxes for 1 image in 1.5 sec: the predicted box duplicates an existing "
                             + "shape.", message(1, 0, 1));
    }

    private static String message(int nrImages, int nrBoxes, int nrDuplicates) {
        final BoundingBoxPredictionResult result = new BoundingBoxPredictionResult(nrImages, List.of(),
                new ImageAnnotationData(List.of(), nrBoxes == 0 ? Map.of() : Map.of("dog", nrBoxes), Map.of()),
                nrDuplicates);
        result.setTimeTakenInMilliseconds(1500);
        return new BoundingBoxPredictionSuccessfulEvent(result).getEventMessage();
    }
}
