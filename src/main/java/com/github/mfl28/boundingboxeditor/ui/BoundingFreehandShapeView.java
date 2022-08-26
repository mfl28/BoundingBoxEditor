/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BoundingFreehandShapeView extends Path implements View, Toggle,
        BoundingShapeViewable {
    private static final String BOUNDING_FREEHAND_SHAPE_ID = "bounding-freehand-shape";
    private static final double HIGHLIGHTED_FILL_OPACITY = 0.3;
    private static final double SELECTED_FILL_OPACITY = 0.5;
    private final BoundingShapeViewData boundingShapeViewData;

    public BoundingFreehandShapeView(ObjectCategory category) {
        this.boundingShapeViewData = new BoundingShapeViewData(this, category);

        setManaged(false);
        setFill(Color.TRANSPARENT);
        setId(BOUNDING_FREEHAND_SHAPE_ID);

        boundingShapeViewData.getNodeGroup().setManaged(false);
        boundingShapeViewData.getNodeGroup().setViewOrder(0);

        setUpInternalListeners();
    }

    public List<Double> getPointsInImage() {
        List<Double> points = new ArrayList<>((getElements().size() - 1) * 2);

        for(PathElement pathElement : getElements()) {
            if(pathElement instanceof MoveTo moveToElement) {
                points.add(moveToElement.getX());
                points.add(moveToElement.getY());
            } else if(pathElement instanceof LineTo lineToElement) {
                points.add(lineToElement.getX());
                points.add(lineToElement.getY());
            }
        }

        return points;
    }

    @Override
    public BoundingShapeViewData getViewData() {
        return boundingShapeViewData;
    }

    @Override
    public void autoScaleWithBoundsAndInitialize(ReadOnlyObjectProperty<Bounds> autoScaleBounds, double imageWidth,
                                                 double imageHeight) {
        autoScaleWithBounds(autoScaleBounds);
    }

    @Override
    public Rectangle2D getRelativeOutlineRectangle() {
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundingShapeViewData, getElements());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingFreehandShapeView other)) {
            return false;
        }

        if(!Objects.equals(boundingShapeViewData, other.boundingShapeViewData) ||
                getElements().size() != other.getElements().size()) {
            return false;
        }

        return Objects.equals(getElements(), other.getElements());
    }

    @Override
    public BoundingShapeTreeItem toTreeItem() {
        return new BoundingPolygonTreeItem(this);
    }

    @Override
    public ToggleGroup getToggleGroup() {
        return boundingShapeViewData.getToggleGroup();
    }

    @Override
    public void setToggleGroup(ToggleGroup toggleGroup) {
        boundingShapeViewData.setToggleGroup(toggleGroup);
    }

    @Override
    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return boundingShapeViewData.toggleGroupProperty();
    }

    @Override
    public boolean isSelected() {
        return boundingShapeViewData.isSelected();
    }

    @Override
    public void setSelected(boolean selected) {
        boundingShapeViewData.setSelected(selected);
    }

    @Override
    public BooleanProperty selectedProperty() {
        return boundingShapeViewData.selectedProperty();
    }

    public void addMoveTo(double x, double y) {
        getElements().add(new MoveTo(x, y));
    }

    public void addLineTo(double x, double y) {
        getElements().add(new LineTo(x, y));
    }

    void autoScaleWithBounds(ReadOnlyObjectProperty<Bounds> autoScaleBounds) {
        boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        addAutoScaleListener();
    }

    private void setUpInternalListeners() {
        fillProperty().bind(Bindings.when(selectedProperty())
                .then(Bindings.createObjectBinding(
                        () -> Color.web(strokeProperty().get().toString(), SELECTED_FILL_OPACITY),
                        strokeProperty()))
                .otherwise(Bindings.when(boundingShapeViewData.highlightedProperty())
                        .then(Bindings.createObjectBinding(() -> Color
                                .web(strokeProperty().get().toString(),
                                        HIGHLIGHTED_FILL_OPACITY), strokeProperty()))
                        .otherwise(Color.TRANSPARENT)));

        boundingShapeViewData.getSelected().addListener((observable, oldValue, newValue) -> {
            if(Boolean.TRUE.equals(newValue)) {
                boundingShapeViewData.getHighlighted().set(false);
            }
        });
    }

    private void addAutoScaleListener() {
        boundingShapeViewData.autoScaleBounds().addListener((observable, oldValue, newValue) -> {
            double xScaleFactor = newValue.getWidth() / oldValue.getWidth();
            double yScaleFactor = newValue.getHeight() / oldValue.getHeight();

            for(PathElement pathElement : getElements()) {
                if(pathElement instanceof MoveTo moveToElement) {
                    moveToElement.setX(newValue.getMinX() + (moveToElement.getX() - oldValue.getMinX()) * xScaleFactor);
                    moveToElement.setY(newValue.getMinY() + (moveToElement.getY() - oldValue.getMinY()) * yScaleFactor);
                } else if(pathElement instanceof LineTo lineToElement) {
                    lineToElement.setX(newValue.getMinX() + (lineToElement.getX() - oldValue.getMinX()) * xScaleFactor);
                    lineToElement.setY(newValue.getMinY() + (lineToElement.getY() - oldValue.getMinY()) * yScaleFactor);
                }
            }
        });
    }
}
