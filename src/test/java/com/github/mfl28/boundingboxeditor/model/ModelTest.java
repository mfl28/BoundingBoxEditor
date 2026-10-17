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
package com.github.mfl28.boundingboxeditor.model;

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("unit")
class ModelTest {
    private final ObjectCategory category = new ObjectCategory("dog", Color.RED);

    @Test
    void onUpdateImageAnnotations_WhenTheImageHasShapesFromTheView_ShouldAddTheNewShapes() {
        final Model model = new Model();
        model.setImageFiles(List.of(new File("a.jpg")));
        final BoundingShapeData drawnBox = box(0.1);
        // The shapes taken from the view are an unmodifiable list.
        model.updateCurrentBoundingShapeData(List.of(drawnBox));
        model.setSaved(true);

        final ImageAnnotation predicted = new ImageAnnotation(
                new ImageMetaData("a.jpg", "folder", "file:/folder/a.jpg", 200, 100, 3));
        final BoundingShapeData predictedBox = box(0.5);
        predicted.getBoundingShapeData().add(predictedBox);

        model.updateImageAnnotations(List.of(predicted), IOResult.OperationType.BOUNDING_BOX_PREDICTION);

        assertEquals(List.of(drawnBox, predictedBox),
                     model.getImageFileNameToAnnotationMap().get("a.jpg").getBoundingShapeData());
        assertFalse(model.isSaved());
    }

    private BoundingShapeData box(double minX) {
        return new BoundingBoxData(category, minX, 0.1, minX + 0.2, 0.3, new ArrayList<>());
    }
}
