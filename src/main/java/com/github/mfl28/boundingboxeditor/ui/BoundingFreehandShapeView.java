/*
 * Copyright (C) 2021 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.model.data.BoundingFreehandShapeData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BoundingFreehandShapeView extends Path implements View, Toggle,
                                                               BoundingShapeDataConvertible, BoundingShapeViewable {
    private static final String BOUNDING_FREEHAND_SHAPE_ID = "bounding-freehand-shape";
    private static final double HIGHLIGHTED_FILL_OPACITY = 0.3;
    private static final double SELECTED_FILL_OPACITY = 0.5;
    private final BoundingShapeViewData boundingShapeViewData;

    private final DoubleProperty xMin = new SimpleDoubleProperty(Double.MAX_VALUE);
    private final DoubleProperty yMin = new SimpleDoubleProperty(Double.MAX_VALUE);
    private final DoubleProperty xMax = new SimpleDoubleProperty(0);
    private final DoubleProperty yMax = new SimpleDoubleProperty(0);
    private List<Double> pointsInImage = Collections.emptyList();

    public BoundingFreehandShapeView(ObjectCategory category) {
        this.boundingShapeViewData = new BoundingShapeViewData(this, category);

        setManaged(false);
        setFill(Color.TRANSPARENT);
        setId(BOUNDING_FREEHAND_SHAPE_ID);

        boundingShapeViewData.getNodeGroup().setManaged(false);

        setUpInternalListeners();
    }

    public static BoundingFreehandShapeView fromData(BoundingFreehandShapeData data, double imageWidth,
                                                     double imageHeight) {
        BoundingFreehandShapeView boundingFreehandShapeView = new BoundingFreehandShapeView(data.getCategory());
        boundingFreehandShapeView.pointsInImage = data.getAbsolutePathPoints(imageWidth, imageHeight);
        boundingFreehandShapeView.getTags().setAll(data.getTags());

        return boundingFreehandShapeView;
    }

    @Override
    public BoundingShapeData toBoundingShapeData() {
        return new BoundingFreehandShapeData(boundingShapeViewData.getObjectCategory(),
                                             boundingShapeViewData.getTags(), getRelativePointsInImageView());
    }

    @Override
    public BoundingShapeViewData getViewData() {
        return boundingShapeViewData;
    }

    @Override
    public void autoScaleWithBoundsAndInitialize(ReadOnlyObjectProperty<Bounds> autoScaleBounds, double imageWidth,
                                                 double imageHeight) {
        boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        initializeFromBoundsInImage(imageWidth, imageHeight);
        addAutoScaleListener();
    }

    @Override
    public Rectangle2D getRelativeOutlineRectangle() {
        final Bounds imageViewBounds = boundingShapeViewData.autoScaleBounds().getValue();

        double relativeXMin = (xMin.get() - imageViewBounds.getMinX()) / imageViewBounds.getWidth();
        double relativeYMin = (yMin.get() - imageViewBounds.getMinY()) / imageViewBounds.getHeight();
        double relativeWidth = (xMax.get() - xMin.get()) / imageViewBounds.getWidth();
        double relativeHeight = (yMax.get() - yMin.get()) / imageViewBounds.getHeight();

        return new Rectangle2D(relativeXMin, relativeYMin, relativeWidth, relativeHeight);
    }

    @Override
    public BoundingShapeTreeItem toTreeItem() {
        return new BoundingFreehandShapeTreeItem(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundingShapeViewData, pointsInImage);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingFreehandShapeView)) {
            return false;
        }

        BoundingFreehandShapeView other = (BoundingFreehandShapeView) obj;

        if(!Objects.equals(boundingShapeViewData, other.boundingShapeViewData) ||
                getElements().size() != other.getElements().size()) {
            return false;
        }

        return Objects.equals(getElements(), other.getElements());
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
        updateOutlineBoxFromCoordinates(x, y);
    }

    public void addLineTo(double x, double y) {
        getElements().add(new LineTo(x, y));
        updateOutlineBoxFromCoordinates(x, y);
    }

    /**
     * Returns the currently assigned tags.
     *
     * @return the tags
     */
    ObservableList<String> getTags() {
        return boundingShapeViewData.getTags();
    }

    List<Double> getRelativePointsInImageView() {
        final Bounds imageViewBounds = boundingShapeViewData.autoScaleBounds().getValue();

        List<Double> points = new ArrayList<>((getElements().size() - 1) * 2);

        for(PathElement pathElement : getElements()) {
            if(pathElement instanceof MoveTo) {
                MoveTo moveToElement = (MoveTo) pathElement;
                points.add((moveToElement.getX() - imageViewBounds.getMinX()) / imageViewBounds.getWidth());
                points.add((moveToElement.getY() - imageViewBounds.getMinY()) / imageViewBounds.getHeight());
            } else if(pathElement instanceof LineTo) {
                LineTo lineToElement = (LineTo) pathElement;

                points.add((lineToElement.getX() - imageViewBounds.getMinX()) / imageViewBounds.getWidth());
                points.add((lineToElement.getY() - imageViewBounds.getMinY()) / imageViewBounds.getHeight());
            }
        }

        return points;
    }

    void autoScaleWithBounds(ReadOnlyObjectProperty<Bounds> autoScaleBounds) {
        boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        addAutoScaleListener();
    }


    List<Double> getMinMaxScaledPoints(double width, double height) {
        final List<Double> points = new ArrayList<>((getElements().size() - 1) * 2);

        for(PathElement element : getElements()) {
            if(element instanceof LineTo) {
                LineTo lineTo = (LineTo) element;
                points.add((lineTo.getX() - xMin.get()) / (xMax.get() - xMin.get()) * width);
                points.add((lineTo.getY() - yMin.get()) / (yMax.get() - yMin.get()) * height);
            } else if(element instanceof MoveTo) {
                MoveTo moveTo = (MoveTo) element;
                points.add((moveTo.getX() - xMin.get()) / (xMax.get() - xMin.get()) * width);
                points.add((moveTo.getY() - yMin.get()) / (yMax.get() - yMin.get()) * height);
            }
        }

        return points;
    }

    private void initializeFromBoundsInImage(double imageWidth, double imageHeight) {
        Bounds confinementBoundsValue = boundingShapeViewData.autoScaleBounds().getValue();

        getElements().setAll(new MoveTo(pointsInImage.get(0) * confinementBoundsValue.getWidth() / imageWidth +
                                                confinementBoundsValue.getMinX(),
                                        pointsInImage.get(1) * confinementBoundsValue.getHeight() / imageHeight +
                                                confinementBoundsValue.getMinY()));

        for(int i = 2; i < pointsInImage.size(); i += 2) {
            getElements().add(new LineTo(pointsInImage.get(i) * confinementBoundsValue.getWidth() / imageWidth +
                                                 confinementBoundsValue.getMinX(),
                                         pointsInImage.get(i + 1) * confinementBoundsValue.getHeight() / imageHeight +
                                                 confinementBoundsValue.getMinY()));
        }

        getElements().add(new ClosePath());
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

        setOnMouseEntered(this::handleMouseEntered);
        setOnMouseExited(this::handleMouseExited);
        setOnMousePressed(this::handleMousePressed);


        boundingShapeViewData.getNodeGroup().viewOrderProperty().bind(
                Bindings.when(boundingShapeViewData.selectedProperty())
                        .then(0)
                        .otherwise(Bindings.min(xMax.subtract(xMin), yMax.subtract(yMin)))
        );
    }

    private void addAutoScaleListener() {
        boundingShapeViewData.autoScaleBounds().addListener((observable, oldValue, newValue) -> {
            double xScaleFactor = newValue.getWidth() / oldValue.getWidth();
            double yScaleFactor = newValue.getHeight() / oldValue.getHeight();

            for(PathElement pathElement : getElements()) {
                if(pathElement instanceof MoveTo) {
                    MoveTo moveToElement = (MoveTo) pathElement;
                    moveToElement.setX(newValue.getMinX() + (moveToElement.getX() - oldValue.getMinX()) * xScaleFactor);
                    moveToElement.setY(newValue.getMinY() + (moveToElement.getY() - oldValue.getMinY()) * yScaleFactor);
                } else if(pathElement instanceof LineTo) {
                    LineTo lineToElement = (LineTo) pathElement;
                    lineToElement.setX(newValue.getMinX() + (lineToElement.getX() - oldValue.getMinX()) * xScaleFactor);
                    lineToElement.setY(newValue.getMinY() + (lineToElement.getY() - oldValue.getMinY()) * yScaleFactor);
                }
            }

            updateOutlineBox();
        });
    }

    private void updateOutlineBox() {
        double newXMin = Double.MAX_VALUE;
        double newYMin = Double.MAX_VALUE;
        double newXMax = 0;
        double newYMax = 0;

        for(PathElement pathElement : getElements()) {
            if(pathElement instanceof LineTo) {
                LineTo lineToElement = (LineTo) pathElement;

                newXMin = Math.min(lineToElement.getX(), newXMin);
                newYMin = Math.min(lineToElement.getY(), newYMin);
                newXMax = Math.max(lineToElement.getX(), newXMax);
                newYMax = Math.max(lineToElement.getY(), newYMax);
            } else if(pathElement instanceof MoveTo) {
                MoveTo moveToElement = (MoveTo) pathElement;

                newXMin = Math.min(moveToElement.getX(), newXMin);
                newYMin = Math.min(moveToElement.getY(), newYMin);
                newXMax = Math.max(moveToElement.getX(), newXMax);
                newYMax = Math.max(moveToElement.getY(), newYMax);
            }
        }

        xMin.set(newXMin);
        xMax.set(newXMax);
        yMin.set(newYMin);
        yMax.set(newYMax);
    }

    private void updateOutlineBoxFromCoordinates(double x, double y) {
        xMin.set(Math.min(xMin.get(), x));
        xMax.set(Math.max(xMax.get(), x));
        yMin.set(Math.min(yMin.get(), y));
        yMax.set(Math.max(yMax.get(), y));
    }

    private void handleMousePressed(MouseEvent event) {
        if(!event.isControlDown()) {
            boundingShapeViewData.getToggleGroup().selectToggle(this);

            event.consume();
        }
    }

    private void handleMouseExited(MouseEvent event) {
        if(!isSelected()) {
            boundingShapeViewData.setHighlighted(false);
        }
    }

    private void handleMouseEntered(MouseEvent event) {
        if(!isSelected()) {
            boundingShapeViewData.setHighlighted(true);
        }

        event.consume();
    }
}
