/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.ClosePath;

import java.util.List;

public class BoundingFreeHandShapeDrawer implements BoundingShapeDrawer {

    private final ImageView imageView;
    private final ToggleGroup toggleGroup;
    private final List<BoundingShapeViewable> boundingShapes;
    private final BooleanProperty autoSimplify;
    private final DoubleProperty simplifyRelativeDistanceTolerance;
    private boolean drawingInProgress = false;
    private BoundingFreehandShapeView boundingFreehandShapeView;

    public BoundingFreeHandShapeDrawer(ImageView imageView, ToggleGroup toggleGroup, List<BoundingShapeViewable> boundingShapes,
                                       BooleanProperty autoSimplify, DoubleProperty simplifyRelativeDistanceTolerance) {
        this.imageView = imageView;
        this.toggleGroup = toggleGroup;
        this.boundingShapes = boundingShapes;
        this.autoSimplify = autoSimplify;
        this.simplifyRelativeDistanceTolerance = simplifyRelativeDistanceTolerance;
    }

    @Override
    public void initializeShape(MouseEvent event, ObjectCategory objectCategory) {
        if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED) && event.getButton().equals(MouseButton.PRIMARY)) {
            Point2D parentCoordinates = imageView.localToParent(event.getX(), event.getY());

            boundingFreehandShapeView = new BoundingFreehandShapeView(objectCategory);
            boundingFreehandShapeView.setToggleGroup(toggleGroup);

            boundingShapes.add(boundingFreehandShapeView);

            boundingFreehandShapeView.autoScaleWithBounds(imageView.boundsInParentProperty());

            boundingFreehandShapeView.setVisible(true);
            toggleGroup.selectToggle(boundingFreehandShapeView);
            boundingFreehandShapeView.addMoveTo(parentCoordinates.getX(), parentCoordinates.getY());
            drawingInProgress = true;
        }
    }

    @Override
    public void updateShape(MouseEvent event) {
        if (event.getEventType().equals(MouseEvent.MOUSE_DRAGGED) && event.getButton().equals(MouseButton.PRIMARY)) {
            final Point2D clampedEventXY =
                    MathUtils.clampWithinBounds(event.getX(), event.getY(), imageView.getBoundsInLocal());

            Point2D parentCoordinates =
                    imageView.localToParent(clampedEventXY.getX(), clampedEventXY.getY());

            boundingFreehandShapeView.addLineTo(parentCoordinates.getX(), parentCoordinates.getY());
        }
    }

    @Override
    public void finalizeShape() {
        boundingFreehandShapeView.getElements().add(new ClosePath());

        BoundingPolygonView boundingPolygonView = new BoundingPolygonView(
                boundingFreehandShapeView.getViewData().getObjectCategory());

        final List<Double> pointsInImage = boundingFreehandShapeView.getPointsInImage();

        boundingPolygonView.setEditing(true);

        for(int i = 0; i < pointsInImage.size(); i += 2) {
            boundingPolygonView.appendNode(pointsInImage.get(i), pointsInImage.get(i + 1));
        }

        if(autoSimplify.get()) {
            boundingPolygonView.simplify(simplifyRelativeDistanceTolerance.get(),
                    boundingFreehandShapeView.getViewData().autoScaleBounds().getValue());
        }

        boundingPolygonView.setToggleGroup(toggleGroup);

        boundingShapes.remove(boundingFreehandShapeView);

        ObjectCategoryTreeItem parentTreeItem = (ObjectCategoryTreeItem) boundingFreehandShapeView.getViewData()
                .getTreeItem().getParent();
        parentTreeItem.detachBoundingShapeTreeItemChild(boundingFreehandShapeView.getViewData().getTreeItem());

        if(parentTreeItem.getChildren().isEmpty()) {
            parentTreeItem.getParent().getChildren().remove(parentTreeItem);
        }

        boundingShapes.add(boundingPolygonView);

        boundingPolygonView.autoScaleWithBounds(imageView.boundsInParentProperty());
        boundingPolygonView.setVisible(true);
        toggleGroup.selectToggle(boundingPolygonView);

        boundingPolygonView.setConstructing(false);
        boundingPolygonView.setEditing(false);

        drawingInProgress = false;
    }

    @Override
    public boolean isDrawingInProgress() {
        return drawingInProgress;
    }

    @Override
    public EditorImagePaneView.DrawingMode getDrawingMode() {
        return EditorImagePaneView.DrawingMode.FREEHAND;
    }
}
