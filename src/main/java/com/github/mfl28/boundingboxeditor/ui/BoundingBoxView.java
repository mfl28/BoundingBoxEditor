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
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents the visual (UI)-component of a bounding-box. To the app-user, instances of
 * this class serve as fully resizable and movable rectangles which are used to specify the bounds
 * of objects in an image. Implements the {@link Toggle} interface to allow a single-selection
 * mechanism.
 *
 * @see Rectangle
 * @see View
 */
public class BoundingBoxView extends Rectangle implements
                                               View, Toggle, BoundingShapeDataConvertible, BoundingShapeViewable {
    private static final double HIGHLIGHTED_FILL_OPACITY = 0.3;
    private static final double SELECTED_FILL_OPACITY = 0.5;
    private static final String BOUNDING_BOX_VIEW_ID = "bounding-rectangle";
    private final BoundingShapeViewData boundingShapeViewData;
    private final DragAnchor dragAnchor = new DragAnchor();
    private final DragAnchor constructionAnchorLocal = new DragAnchor();
    private Bounds boundsInImage;

    /**
     * Creates a new bounding-box UI-element, which takes the shape of a rectangle that is resizable
     * and movable by the user.
     *
     * @param objectCategory the category this bounding-box will be assigned to
     *                       into the data component represented by the {@link BoundingBoxData} class
     */
    public BoundingBoxView(ObjectCategory objectCategory) {
        this.boundingShapeViewData = new BoundingShapeViewData(this, objectCategory);

        setManaged(false);
        setFill(Color.TRANSPARENT);
        setId(BOUNDING_BOX_VIEW_ID);

        boundingShapeViewData.getNodeGroup().setManaged(false);
        boundingShapeViewData.getNodeGroup().getChildren().addAll(createResizeHandles());

        addMoveFunctionality();
        setUpInternalListeners();
    }

    /**
     * Creates a new {@link BoundingBoxView} object from stored bounding-box data. This function is called
     * when bounding-box data stored in the model-component should be transformed to the visual bounding-box
     * component which is displayed to the user.
     *
     * @param boundingBoxData the stored {@link BoundingBoxData} object used to construct the new {@link BoundingBoxView} object
     * @return the new {@link BoundingBoxView} object
     */
    public static BoundingBoxView fromData(BoundingBoxData boundingBoxData, double imageWidth, double imageHeight) {
        BoundingBoxView boundingBox = new BoundingBoxView(boundingBoxData.getCategory());
        boundingBox.setBoundsInImage(boundingBoxData.getAbsoluteBoundsInImage(imageWidth, imageHeight));
        boundingBox.getTags().setAll(boundingBoxData.getTags());
        return boundingBox;
    }

    public DragAnchor getConstructionAnchorLocal() {
        return constructionAnchorLocal;
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

    @Override
    public int hashCode() {
        return Objects.hash(boundingShapeViewData, getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingBoxView other)) {
            return false;
        }

        return Objects.equals(boundingShapeViewData, other.boundingShapeViewData)
                && MathUtils.doubleAlmostEqual(getX(), other.getX()) &&
                MathUtils.doubleAlmostEqual(getY(), other.getY())
                && MathUtils.doubleAlmostEqual(getWidth(), other.getWidth()) &&
                MathUtils.doubleAlmostEqual(getHeight(), other.getHeight());
    }

    /**
     * Returns the associated {@link ObjectCategory} object.
     *
     * @return the {@link ObjectCategory} object
     */
    public ObjectCategory getObjectCategory() {
        return boundingShapeViewData.getObjectCategory();
    }

    /**
     * Extracts a {@link BoundingBoxData} object used to store the 'blueprint' of this {@link BoundingBoxView}
     * object and returns it.
     *
     * @return the {@link  BoundingBoxData} object
     */
    @Override
    public BoundingShapeData toBoundingShapeData() {
        return new BoundingBoxData(boundingShapeViewData.getObjectCategory(),
                                   getRelativeBoundsInImageView(), boundingShapeViewData.getTags());
    }

    @Override
    public BoundingShapeViewData getViewData() {
        return boundingShapeViewData;
    }

    /**
     * Anchors the {@link BoundingBoxView} object to and automatically scales it with the provided {@link Bounds}-property.
     * Initializes the {@link BoundingBoxView} with the correctly scaled size relative to the current size of the
     * value of the autoScaleBounds-property.
     *
     * @param autoScaleBounds the bounds-property to scale with
     */
    @Override
    public void autoScaleWithBoundsAndInitialize(ReadOnlyObjectProperty<Bounds> autoScaleBounds, double imageWidth,
                                                 double imageHeight) {
        this.boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        initializeFromBoundsInImage(imageWidth, imageHeight);
        addAutoScaleListener();
    }

    @Override
    public Rectangle2D getRelativeOutlineRectangle() {
        final Bounds relativeBounds = getRelativeBoundsInImageView();
        return new Rectangle2D(relativeBounds.getMinX(), relativeBounds.getMinY(), relativeBounds.getWidth(),
                               relativeBounds.getHeight());
    }

    @Override
    public BoundingShapeTreeItem toTreeItem() {
        return new BoundingBoxTreeItem(this);
    }

    /**
     * Sets up the {@link BoundingBoxView} object's coordinates and size from a provided initializer-rectangle.
     *
     * @param initializer the initializer {@link Rectangle} object
     */
    void setCoordinatesAndSizeFromInitializer(Rectangle initializer) {
        setX(initializer.getX());
        setY(initializer.getY());
        setWidth(initializer.getWidth());
        setHeight(initializer.getHeight());
    }

    /**
     * Returns the currently assigned tags.
     *
     * @return the tags
     */
    ObservableList<String> getTags() {
        return boundingShapeViewData.getTags();
    }

    /**
     * Anchors the {@link BoundingBoxView} object to and automatically scales it with the
     * provided {@link Bounds}-property.
     *
     * @param autoScaleBounds the auto-scale-bounds property used to anchor this {@link BoundingBoxView} object
     */
    void autoScaleWithBounds(ReadOnlyObjectProperty<Bounds> autoScaleBounds) {
        this.boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        addAutoScaleListener();
    }

    private List<ResizeHandle> createResizeHandles() {
        return Arrays.stream(CompassPoint.values())
                     .map(ResizeHandle::new)
                     .toList();
    }

    private void addMoveFunctionality() {
        setOnMouseEntered(event -> {
            setCursor(Cursor.MOVE);

            if(!boundingShapeViewData.isSelected()) {
                boundingShapeViewData.getHighlighted().set(true);
            }

            event.consume();
        });

        setOnMouseExited(event -> {
            if(!boundingShapeViewData.isSelected()) {
                boundingShapeViewData.getHighlighted().set(false);
            }
        });

        setOnMousePressed(event -> {
            if(!event.isShortcutDown()) {
                boundingShapeViewData.getToggleGroup().selectToggle(this);

                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    dragAnchor.setCoordinates(event.getX() - getX(), event.getY() - getY());
                }

                event.consume();
            }
        });

        setOnMouseDragged(event -> {
            if(!event.isShortcutDown()) {
                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    Point2D newXY = new Point2D(event.getX() - dragAnchor.getX(), event.getY() - dragAnchor.getY());
                    Point2D newXYConfined = MathUtils.clampWithinBounds(newXY, constructCurrentMoveBounds());

                    setX(newXYConfined.getX());
                    setY(newXYConfined.getY());
                }
                event.consume();
            }
        });
    }

    private void setUpInternalListeners() {
        boundingShapeViewData.getNodeGroup().viewOrderProperty().bind(
                Bindings.when(boundingShapeViewData.selectedProperty())
                        .then(0)
                        .otherwise(Bindings.min(widthProperty(), heightProperty()))
        );

        fillProperty().bind(Bindings.when(boundingShapeViewData.selectedProperty())
                                    .then(Bindings.createObjectBinding(
                                            () -> Color.web(strokeProperty().get().toString(), SELECTED_FILL_OPACITY),
                                            strokeProperty()))
                                    .otherwise(Bindings.when(boundingShapeViewData.getHighlighted())
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

    private Bounds constructCurrentMoveBounds() {
        final Bounds confinementBoundsValue = boundingShapeViewData.autoScaleBounds().getValue();
        return new BoundingBox(confinementBoundsValue.getMinX(), confinementBoundsValue.getMinY(),
                               confinementBoundsValue.getWidth() - getWidth(),
                               confinementBoundsValue.getHeight() - getHeight());
    }

    private Bounds getRelativeBoundsInImageView() {
        final Bounds imageViewBounds = boundingShapeViewData.autoScaleBounds().getValue();

        double xMin = (getX() - imageViewBounds.getMinX()) / imageViewBounds.getWidth();
        double yMin = (getY() - imageViewBounds.getMinY()) / imageViewBounds.getHeight();
        double width = getWidth() / imageViewBounds.getWidth();
        double height = getHeight() / imageViewBounds.getHeight();

        return new BoundingBox(xMin, yMin, width, height);
    }

    private void addAutoScaleListener() {
        boundingShapeViewData.autoScaleBounds().addListener((observable, oldValue, newValue) -> {
            setWidth(getWidth() * newValue.getWidth() / oldValue.getWidth());
            setHeight(getHeight() * newValue.getHeight() / oldValue.getHeight());

            setX(newValue.getMinX() + (getX() - oldValue.getMinX()) * newValue.getWidth() / oldValue.getWidth());
            setY(newValue.getMinY() + (getY() - oldValue.getMinY()) * newValue.getHeight() / oldValue.getHeight());

            constructionAnchorLocal.setCoordinates(
                    constructionAnchorLocal.getX() * newValue.getWidth() / oldValue.getWidth(),
                    constructionAnchorLocal.getY() * newValue.getHeight() / oldValue.getHeight()
                    );
        });
    }

    private void setBoundsInImage(Bounds boundsInImage) {
        this.boundsInImage = boundsInImage;
    }

    private void initializeFromBoundsInImage(double imageWidth, double imageHeight) {
        Bounds confinementBoundsValue = boundingShapeViewData.autoScaleBounds().getValue();

        setX(boundsInImage.getMinX() * confinementBoundsValue.getWidth() / imageWidth +
                     confinementBoundsValue.getMinX());
        setY(boundsInImage.getMinY() * confinementBoundsValue.getHeight() / imageHeight +
                     confinementBoundsValue.getMinY());
        setWidth(boundsInImage.getWidth() * confinementBoundsValue.getWidth() / imageWidth);
        setHeight(boundsInImage.getHeight() * confinementBoundsValue.getHeight() / imageHeight);
    }

    private enum CompassPoint {NW, N, NE, E, SE, S, SW, W}

    private class ResizeHandle extends Rectangle {
        private static final double SIDE_LENGTH = 9.5;

        private final CompassPoint compassPoint;
        private final DragAnchor dragAnchor = new DragAnchor();

        ResizeHandle(CompassPoint compassPoint) {
            super(SIDE_LENGTH, SIDE_LENGTH);
            this.compassPoint = compassPoint;

            bindToParentRectangle();
            addResizeFunctionality();
        }

        private double getRectangleMaxX() {
            return BoundingBoxView.this.getX() + BoundingBoxView.this.getWidth();
        }

        private double getRectangleMaxY() {
            return BoundingBoxView.this.getY() + BoundingBoxView.this.getHeight();
        }

        private void bindToParentRectangle() {
            final BoundingBoxView rectangle = BoundingBoxView.this;
            final DoubleProperty rectangleX = rectangle.xProperty();
            final DoubleProperty rectangleY = rectangle.yProperty();
            final DoubleProperty rectangleW = rectangle.widthProperty();
            final DoubleProperty rectangleH = rectangle.heightProperty();

            fillProperty().bind(rectangle.strokeProperty());
            managedProperty().bind(rectangle.managedProperty());
            visibleProperty().bind(rectangle.visibleProperty().and(rectangle.selectedProperty()));

            switch(compassPoint) {
                case NW -> {
                    xProperty().bind(rectangleX.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.subtract(SIDE_LENGTH / 2));
                }
                case N -> {
                    xProperty().bind(rectangleX.add(rectangleW.subtract(SIDE_LENGTH).divide(2)));
                    yProperty().bind(rectangleY.subtract(SIDE_LENGTH / 2));
                }
                case NE -> {
                    xProperty().bind(rectangleX.add(rectangleW).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.subtract(SIDE_LENGTH / 2));
                }
                case E -> {
                    xProperty().bind(rectangleX.add(rectangleW).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.add(rectangleH.subtract(SIDE_LENGTH).divide(2)));
                }
                case SE -> {
                    xProperty().bind(rectangleX.add(rectangleW).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.add(rectangleH).subtract(SIDE_LENGTH / 2));
                }
                case S -> {
                    xProperty().bind(rectangleX.add(rectangleW.subtract(SIDE_LENGTH).divide(2)));
                    yProperty().bind(rectangleY.add(rectangleH).subtract(SIDE_LENGTH / 2));
                }
                case SW -> {
                    xProperty().bind(rectangleX.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.add(rectangleH).subtract(SIDE_LENGTH / 2));
                }
                case W -> {
                    xProperty().bind(rectangleX.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.add(rectangleH.subtract(SIDE_LENGTH).divide(2)));
                }
            }
        }

        private void addResizeFunctionality() {
            setOnMouseEntered(event -> {
                setCursor(Cursor.cursor(compassPoint.toString() + "_RESIZE"));
                event.consume();
            });

            setOnMousePressed(event -> {
                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    dragAnchor.setFromMouseEvent(event);
                }
                event.consume();
            });

            switch(compassPoint) {
                case NW -> setOnMouseDragged(this::handleMouseDraggedNW);
                case N -> setOnMouseDragged(this::handleMouseDraggedN);
                case NE -> setOnMouseDragged(this::handleMouseDraggedNE);
                case E -> setOnMouseDragged(this::handleMouseDraggedE);
                case SE -> setOnMouseDragged(this::handleMouseDraggedSE);
                case S -> setOnMouseDragged(this::handleMouseDraggedS);
                case SW -> setOnMouseDragged(this::handleMouseDraggedSW);
                case W -> setOnMouseDragged(this::handleMouseDraggedW);
            }
        }

        private void handleMouseDraggedNW(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.boundingShapeViewData.autoScaleBounds().getValue();
                final Bounds bounds = new BoundingBox(parentBounds.getMinX(), parentBounds.getMinY(),
                                                      getRectangleMaxX() - parentBounds.getMinX(),
                                                      getRectangleMaxY() - parentBounds.getMinY());

                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setX(clampedEventXY.getX());
                BoundingBoxView.this.setY(clampedEventXY.getY());
                BoundingBoxView.this.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
                BoundingBoxView.this.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
            }

            event.consume();
        }

        private void handleMouseDraggedN(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.boundingShapeViewData.autoScaleBounds().getValue();
                final Bounds bounds = new BoundingBox(BoundingBoxView.this.getX(), parentBounds.getMinY(),
                                                      BoundingBoxView.this.getWidth(),
                                                      getRectangleMaxY() - parentBounds.getMinY());

                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setY(clampedEventXY.getY());
                BoundingBoxView.this.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
            }

            event.consume();
        }

        private void handleMouseDraggedNE(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.boundingShapeViewData.autoScaleBounds().getValue();
                final Bounds bounds = new BoundingBox(BoundingBoxView.this.getX(), parentBounds.getMinY(),
                                                      parentBounds.getMaxX() - BoundingBoxView.this.getX(),
                                                      getRectangleMaxY() - parentBounds.getMinY());

                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setY(clampedEventXY.getY());
                BoundingBoxView.this.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                BoundingBoxView.this.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
            }

            event.consume();
        }

        private void handleMouseDraggedE(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.boundingShapeViewData.autoScaleBounds().getValue();
                final Bounds bounds = new BoundingBox(BoundingBoxView.this.getX(), BoundingBoxView.this.getY(),
                                                      parentBounds.getMaxX() - BoundingBoxView.this.getX(),
                                                      BoundingBoxView.this.getHeight());
                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
            }

            event.consume();
        }

        private void handleMouseDraggedSE(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.boundingShapeViewData.autoScaleBounds().getValue();
                final Bounds bounds = new BoundingBox(BoundingBoxView.this.getX(), BoundingBoxView.this.getY(),
                                                      parentBounds.getMaxX() - BoundingBoxView.this.getX(),
                                                      parentBounds.getMaxY() - BoundingBoxView.this.getY());

                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                BoundingBoxView.this.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
            }

            event.consume();
        }

        private void handleMouseDraggedS(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.boundingShapeViewData.autoScaleBounds().getValue();
                final Bounds bounds = new BoundingBox(BoundingBoxView.this.getX(), BoundingBoxView.this.getY(),
                                                      BoundingBoxView.this.getWidth(),
                                                      parentBounds.getMaxY() - BoundingBoxView.this.getY());

                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
            }

            event.consume();
        }

        private void handleMouseDraggedSW(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.boundingShapeViewData.autoScaleBounds().getValue();
                final Bounds bounds = new BoundingBox(parentBounds.getMinX(), BoundingBoxView.this.getY(),
                                                      getRectangleMaxX() - parentBounds.getMinX(),
                                                      parentBounds.getMaxY() - BoundingBoxView.this.getY());

                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setX(clampedEventXY.getX());
                BoundingBoxView.this.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
                BoundingBoxView.this.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
            }

            event.consume();
        }

        private void handleMouseDraggedW(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.boundingShapeViewData.autoScaleBounds().getValue();
                final Bounds bounds = new BoundingBox(parentBounds.getMinX(), BoundingBoxView.this.getY(),
                                                      getRectangleMaxX() - parentBounds.getMinX(),
                                                      BoundingBoxView.this.getHeight());

                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setX(clampedEventXY.getX());
                BoundingBoxView.this.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
            }

            event.consume();
        }
    }
}
