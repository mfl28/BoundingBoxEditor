/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.model.data.BoundingPolygonData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

/**
 * Represents the visual (UI)-component of a bounding-polygon.
 * Implements the {@link Toggle} interface to allow a single-selection
 * mechanism.
 *
 * @see Polygon
 * @see View
 */
public class BoundingPolygonView extends Polygon implements
                                                 View, Toggle, BoundingShapeDataConvertible, BoundingShapeViewable {
    private static final double HIGHLIGHTED_FILL_OPACITY = 0.3;
    private static final double SELECTED_FILL_OPACITY = 0.5;
    private static final String BOUNDING_POLYGON_ID = "bounding-polygon";
    private static final String EDITING_PSEUDO_CLASS_NAME = "editing";
    private static final PseudoClass EDITING_PSEUDO_CLASS = PseudoClass.getPseudoClass(EDITING_PSEUDO_CLASS_NAME);

    private final BoundingShapeViewData boundingShapeViewData;

    private final BooleanProperty editing = createEditingProperty();
    private final BooleanProperty constructing = new SimpleBooleanProperty(false);
    private final DoubleProperty width = new SimpleDoubleProperty();
    private final DoubleProperty height = new SimpleDoubleProperty();
    private final ObservableList<VertexHandle> vertexHandles = FXCollections.observableArrayList();
    private final ObservableSet<Integer> editingIndices = FXCollections.observableSet(new LinkedHashSet<>());
    private List<Double> pointsInImage = Collections.emptyList();

    /**
     * Creates a new bounding-shape UI-element, which takes the shape of a rectangle that is resizable
     * and movable by the user.
     *
     * @param objectCategory the category this bounding-shape will be assigned to
     */
    public BoundingPolygonView(ObjectCategory objectCategory) {
        this.boundingShapeViewData = new BoundingShapeViewData(this, objectCategory);

        setManaged(false);
        setFill(Color.TRANSPARENT);
        setId(BOUNDING_POLYGON_ID);
        setStrokeWidth(2.0);

        boundingShapeViewData.getNodeGroup().setManaged(false);
        pseudoClassStateChanged(EDITING_PSEUDO_CLASS, false);
        setUpInternalListeners();
    }

    /**
     * Creates a new {@link BoundingPolygonView} object from stored bounding-shape data. This function is called
     * when bounding-shape data stored in the model-component should be transformed to the visual bounding-shape
     * component which is displayed to the user.
     *
     * @param boundingPolygonData the stored {@link BoundingPolygonData} object used to construct the new {@link BoundingPolygonView} object
     * @return the new {@link BoundingPolygonView} object
     */
    public static BoundingPolygonView fromData(BoundingPolygonData boundingPolygonData, double imageWidth,
                                               double imageHeight) {
        BoundingPolygonView boundingPolygon = new BoundingPolygonView(boundingPolygonData.getCategory());
        boundingPolygon.pointsInImage = boundingPolygonData.getAbsolutePointsInImage(imageWidth, imageHeight);
        boundingPolygon.getTags().setAll(boundingPolygonData.getTags());
        return boundingPolygon;
    }

    public void appendNode(double x, double y) {
        vertexHandles.add(new VertexHandle(x, y, getPoints().size()));
    }

    public ObjectCategory getObjectCategory() {
        return boundingShapeViewData.getObjectCategory();
    }

    public boolean isConstructing() {
        return constructing.get();
    }

    public void setConstructing(boolean constructing) {
        this.constructing.set(constructing);
    }

    public BooleanProperty constructingProperty() {
        return constructing;
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
        return Objects.hash(boundingShapeViewData, pointsInImage);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingPolygonView)) {
            return false;
        }

        BoundingPolygonView other = (BoundingPolygonView) obj;

        if(!Objects.equals(boundingShapeViewData, other.boundingShapeViewData) ||
                getPoints().size() != other.getPoints().size()) {
            return false;
        }

        for(int i = 0; i != getPoints().size(); ++i) {
            if(!MathUtils.doubleAlmostEqual(getPoints().get(i), other.getPoints().get(i))) {
                return false;
            }
        }

        return true;
    }

    public boolean isEditing() {
        return editing.get();
    }

    public void setEditing(boolean editing) {
        this.editing.set(editing);
    }

    public BooleanProperty editingProperty() {
        return editing;
    }

    /**
     * Extracts a {@link BoundingPolygonData} object used to store the 'blueprint' of this {@link BoundingPolygonView}
     * object and returns it.
     *
     * @return the {@link  BoundingPolygonData} object
     */
    @Override
    public BoundingShapeData toBoundingShapeData() {
        return new BoundingPolygonData(boundingShapeViewData.getObjectCategory(),
                                       getRelativePointsInImageView(), boundingShapeViewData.getTags());
    }

    @Override
    public BoundingShapeViewData getViewData() {
        return boundingShapeViewData;
    }

    /**
     * Anchors the {@link BoundingPolygonView} object to and automatically scales it with the provided {@link Bounds}-property.
     * Initializes the {@link BoundingPolygonView} with the correctly scaled size relative to the current size of the
     * value of the autoScaleBounds-property.
     *
     * @param autoScaleBounds the bounds-property to scale with
     */
    @Override
    public void autoScaleWithBoundsAndInitialize(ReadOnlyObjectProperty<Bounds> autoScaleBounds, double imageWidth,
                                                 double imageHeight) {
        boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        initializeFromBoundsInImage(imageWidth, imageHeight);
        addAutoScaleListener();
    }

    @Override
    public BoundingShapeTreeItem toTreeItem() {
        return new BoundingPolygonTreeItem(this);
    }

    /**
     * Anchors the {@link BoundingPolygonView} object to and automatically scales it with the
     * provided {@link Bounds}-property.
     *
     * @param autoScaleBounds the auto-scale-bounds property used to anchor this {@link BoundingPolygonView} object
     */
    void autoScaleWithBounds(ReadOnlyObjectProperty<Bounds> autoScaleBounds) {
        boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        addAutoScaleListener();
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

        List<Double> points = new ArrayList<>(getPoints().size());

        for(VertexHandle vertexHandle : vertexHandles) {
            points.add((vertexHandle.getCenterX() - imageViewBounds.getMinX()) / imageViewBounds.getWidth());
            points.add((vertexHandle.getCenterY() - imageViewBounds.getMinY()) / imageViewBounds.getHeight());
        }

        return points;
    }

    ObservableList<VertexHandle> getVertexHandles() {
        return vertexHandles;
    }

    ObservableSet<Integer> getEditingIndices() {
        return editingIndices;
    }

    void refine() {
        splice();
        setEditing(false);
    }

    void removeEditingVertices() {
        long numToRemove = editingIndices.size();

        if(vertexHandles.size() - numToRemove >= 2) {
            vertexHandles.removeIf(VertexHandle::isEditing);
        }

        setEditing(false);
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

        vertexHandles.addListener((ListChangeListener<VertexHandle>) c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    for(VertexHandle vertexHandle : c.getAddedSubList()) {
                        getPoints().add(vertexHandle.pointIndex.get(), vertexHandle.getCenterX());
                        getPoints().add(vertexHandle.pointIndex.get() + 1, vertexHandle.getCenterY());

                        for(int i = vertexHandle.pointIndex.get() / 2 + 1; i < vertexHandles.size(); ++i) {
                            vertexHandles.get(i).pointIndex.set(vertexHandles.get(i).pointIndex.get() + 2);
                        }
                    }

                    boundingShapeViewData.getNodeGroup().getChildren().addAll(c.getAddedSubList());
                    updateWidthAndHeight();
                }

                if(c.wasRemoved()) {
                    for(VertexHandle vertexHandle : c.getRemoved()) {
                        getPoints().remove(vertexHandle.pointIndex.get());
                        getPoints().remove(vertexHandle.pointIndex.get());

                        for(int i = vertexHandle.pointIndex.get() / 2; i < vertexHandles.size(); ++i) {
                            vertexHandles.get(i).pointIndex.set(vertexHandles.get(i).pointIndex.get() - 2);
                        }
                    }

                    boundingShapeViewData.getNodeGroup().getChildren().removeAll(c.getRemoved());
                    updateWidthAndHeight();
                }

                if(isConstructing() && !vertexHandles.isEmpty()) {
                    vertexHandles.forEach(vertexHandle -> vertexHandle.setEditing(false));
                    vertexHandles.get(0).setEditing(true);
                    vertexHandles.get(vertexHandles.size() - 1).setEditing(true);
                }
            }
        });

        setOnMouseEntered(event -> {
            if(!isSelected()) {
                boundingShapeViewData.setHighlighted(true);
            }

            event.consume();
        });

        setOnMouseExited(event -> {
            if(!isSelected()) {
                boundingShapeViewData.setHighlighted(false);
            }
        });

        setOnMousePressed(event -> {
            if(!event.isControlDown()) {
                boundingShapeViewData.getToggleGroup().selectToggle(this);

                if(event.isShiftDown() && event.getButton().equals(MouseButton.MIDDLE)) {
                    refine();
                }

                event.consume();
            }
        });

        boundingShapeViewData.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(Boolean.TRUE.equals(newValue)) {
                boundingShapeViewData.setHighlighted(false);
            } else {
                setConstructing(false);
                setEditing(false);
            }
        });

        editing.addListener((observable, oldValue, newValue) -> {
            if(Boolean.FALSE.equals(newValue)) {
                vertexHandles.forEach(vertexHandle -> vertexHandle.setEditing(false));
                editingIndices.clear();
            }
        });

        constructing.addListener((observable, oldValue, newValue) -> {
            if(Boolean.FALSE.equals(newValue)) {
                setMouseTransparent(false);
                setEditing(false);
            }
        });

        visibleProperty().addListener((observable, oldValue, newValue) -> {
            if(Boolean.FALSE.equals(newValue)) {
                setConstructing(false);
                setEditing(false);
            }
        });

        boundingShapeViewData.getNodeGroup().viewOrderProperty().bind(
                Bindings.when(boundingShapeViewData.selectedProperty())
                        .then(0)
                        .otherwise(Bindings.min(width, height))
        );
    }

    private void updateWidthAndHeight() {
        double xMin = Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double xMax = 0;
        double yMax = 0;

        List<Double> points = getPoints();

        for(int i = 0; i < points.size(); i += 2) {
            xMin = Math.min(points.get(i), xMin);
            yMin = Math.min(points.get(i + 1), yMin);
            xMax = Math.max(points.get(i), xMax);
            yMax = Math.max(points.get(i + 1), yMax);
        }

        width.set(Math.abs(xMax - xMin));
        height.set(Math.abs(yMax - yMin));
    }

    private void initializeFromBoundsInImage(double imageWidth, double imageHeight) {
        Bounds confinementBoundsValue = boundingShapeViewData.autoScaleBounds().getValue();

        vertexHandles.clear();

        for(int i = 0; i < pointsInImage.size(); i += 2) {
            vertexHandles.add(new VertexHandle(pointsInImage.get(i) * confinementBoundsValue.getWidth() / imageWidth +
                                                       confinementBoundsValue.getMinX(),
                                               pointsInImage.get(i + 1) * confinementBoundsValue.getHeight() /
                                                       imageHeight + confinementBoundsValue.getMinY(), i));
        }
    }

    private void addAutoScaleListener() {
        boundingShapeViewData.autoScaleBounds().addListener((observable, oldValue, newValue) -> {
            double xScaleFactor = newValue.getWidth() / oldValue.getWidth();
            double yScaleFactor = newValue.getHeight() / oldValue.getHeight();

            for(VertexHandle vertexHandle : vertexHandles) {
                vertexHandle.setCenterX(
                        newValue.getMinX() + (vertexHandle.getCenterX() - oldValue.getMinX()) * xScaleFactor);
                vertexHandle.setCenterY(
                        newValue.getMinY() + (vertexHandle.getCenterY() - oldValue.getMinY()) * yScaleFactor);
            }
        });
    }

    private BooleanProperty createEditingProperty() {
        return new BooleanPropertyBase(false) {
            @Override
            public Object getBean() {
                return BoundingPolygonView.this;
            }

            @Override
            public String getName() {
                return EDITING_PSEUDO_CLASS_NAME;
            }

            @Override
            protected void invalidated() {
                pseudoClassStateChanged(EDITING_PSEUDO_CLASS, get());
            }
        };
    }

    private void splice() {
        if(editingIndices.size() < 2 || isConstructing()) {
            // Nothing to do
            return;
        }

        int spliceRootIndex = (int) editingIndices.toArray()[0];
        int lowIndex = Math.floorMod(spliceRootIndex - 2, getPoints().size());
        int highIndex = Math.floorMod(spliceRootIndex + 2, getPoints().size());

        List<ImmutablePair<Integer, Integer>> edges = new ArrayList<>();

        if(lowIndex == highIndex) {
            edges.add(new ImmutablePair<>(spliceRootIndex, lowIndex));
        } else {
            int numEdges = vertexHandles.size();

            while(numEdges > 0 && (editingIndices.contains(lowIndex) || editingIndices.contains(highIndex))) {
                if(editingIndices.contains(lowIndex)) {
                    edges.add(0, new ImmutablePair<>(lowIndex, Math.floorMod(lowIndex + 2, getPoints().size())));
                    lowIndex = Math.floorMod(lowIndex - 2, getPoints().size());
                } else if(editingIndices.contains(highIndex)) {
                    edges.add(new ImmutablePair<>(Math.floorMod(highIndex - 2, getPoints().size()), highIndex));
                    highIndex = Math.floorMod(highIndex + 2, getPoints().size());
                }
                numEdges -= 1;
            }
        }

        int indexShift = 0;
        int indexShiftThreshold = getPoints().size();

        for(ImmutablePair<Integer, Integer> pair : edges) {
            int startIndex = pair.left + (pair.left > indexShiftThreshold ? indexShift : 0);
            int endIndex = pair.right + (pair.right > indexShiftThreshold ? indexShift : 0);

            indexShiftThreshold = Math.min(indexShiftThreshold, startIndex);

            insertVertexHandleBetween(startIndex, endIndex);

            if(endIndex == 0) {
                indexShift = 0;
            } else {
                indexShift += 2;
            }
        }
    }

    private void insertVertexHandleBetween(int startIndex, int endIndex) {
        List<Double> points = getPoints();

        double midpointX = (points.get(startIndex) + points.get(endIndex)) / 2.0;
        double midpointY = (points.get(startIndex + 1) + points.get(endIndex + 1)) / 2.0;

        vertexHandles.add(startIndex / 2 + 1, new VertexHandle(midpointX, midpointY, startIndex + 2));
    }

    class VertexHandle extends Circle {
        private static final double RADIUS = 6.5;
        private static final String VERTEX_HANDLE_ID = "vertex-handle";
        private static final String EDITING_PSEUDO_CLASS_NAME = "editing";
        private static final double BRIGHTNESS_BLACK_SWITCH_THRESHOLD = 0.75;
        private final PseudoClass editingPseudoClass =
                PseudoClass.getPseudoClass(VertexHandle.EDITING_PSEUDO_CLASS_NAME);
        private final BooleanProperty editing = createEditingProperty();
        private final DragAnchor dragAnchor = new DragAnchor();
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final IntegerProperty pointIndex;

        VertexHandle(double pointX, double pointY, int pointIndex) {
            super(pointX, pointY, RADIUS);
            this.pointIndex = new SimpleIntegerProperty(pointIndex);

            setId(VERTEX_HANDLE_ID);
            pseudoClassStateChanged(editingPseudoClass, false);

            addMoveFunctionality();
            setUpInternalListeners();
        }

        public boolean isEditing() {
            return this.editing.get();
        }

        public void setEditing(boolean editing) {
            this.editing.set(editing);
        }

        public BooleanProperty editingProperty() {
            return this.editing;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        int getPointIndex() {
            return pointIndex.get();
        }

        private void addMoveFunctionality() {
            setOnMousePressed(mouseEvent -> {
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    dragAnchor.setCoordinates(mouseEvent.getX() - getCenterX(), mouseEvent.getY() - getCenterY());
                } else if(mouseEvent.getButton().equals(MouseButton.MIDDLE) && !isConstructing()) {
                    int lowNeighborIndex =
                            Math.floorMod(pointIndex.get() - 2, BoundingPolygonView.this.getPoints().size());
                    int highNeighborIndex =
                            Math.floorMod(pointIndex.get() + 2, BoundingPolygonView.this.getPoints().size());

                    if(isEditing()) {
                        setEditing(false);
                        BoundingPolygonView.this.editingIndices.remove(pointIndex.get());

                        if(BoundingPolygonView.this.editingIndices.contains(lowNeighborIndex)
                                && BoundingPolygonView.this.editingIndices.contains(highNeighborIndex)
                                && BoundingPolygonView.this.editingIndices.size() !=
                                BoundingPolygonView.this.vertexHandles.size() - 1) {
                            BoundingPolygonView.this.setEditing(false);
                        }
                    } else {
                        if(BoundingPolygonView.this.editingIndices.isEmpty()) {
                            setEditing(true);
                            BoundingPolygonView.this.setEditing(true);
                            BoundingPolygonView.this.editingIndices.add(pointIndex.get());
                        } else if(BoundingPolygonView.this.editingIndices.contains(pointIndex.get())) {
                            if((BoundingPolygonView.this.editingIndices.contains(lowNeighborIndex)
                                    && !BoundingPolygonView.this.editingIndices.contains(highNeighborIndex)) ||
                                    (BoundingPolygonView.this.editingIndices.contains(highNeighborIndex)
                                            && !BoundingPolygonView.this.editingIndices.contains(lowNeighborIndex)) ||
                                    BoundingPolygonView.this.editingIndices.size() == 1) {
                                setEditing(false);
                                BoundingPolygonView.this.editingIndices.remove(pointIndex.get());
                            }
                        } else {
                            if(BoundingPolygonView.this.editingIndices.contains(lowNeighborIndex)
                                    || BoundingPolygonView.this.editingIndices.contains(highNeighborIndex)) {
                                setEditing(true);
                                BoundingPolygonView.this.setEditing(true);
                                BoundingPolygonView.this.editingIndices.add(pointIndex.get());
                            }
                        }
                    }
                }

                mouseEvent.consume();
            });

            setOnMouseDragged(mouseEvent -> {
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    Point2D newXY =
                            new Point2D(mouseEvent.getX() - dragAnchor.getX(), mouseEvent.getY() - dragAnchor.getY());
                    Point2D newXYConfined =
                            MathUtils.clampWithinBounds(newXY, boundingShapeViewData.autoScaleBounds().getValue());

                    setCenterX(newXYConfined.getX());
                    setCenterY(newXYConfined.getY());
                }
                mouseEvent.consume();
            });
        }

        private void setUpInternalListeners() {
            fillProperty().bind(Bindings
                                        .when(selected)
                                        .then(Bindings.createObjectBinding(() -> Color
                                                                                   .web(BoundingPolygonView.this.strokeProperty().get().toString(), 1.0)
                                                                                   .brighter(),
                                                                           BoundingPolygonView.this.strokeProperty()))
                                        .otherwise(Bindings.createObjectBinding(() -> Color
                                                                                        .web(BoundingPolygonView.this.strokeProperty().get().toString(), 1.0),
                                                                                BoundingPolygonView.this
                                                                                        .strokeProperty())));

            visibleProperty().bind(BoundingPolygonView.this.visibleProperty()
                                                           .and(BoundingPolygonView.this.selectedProperty()
                                                                                        .or(BoundingPolygonView.this
                                                                                                    .editingProperty())));

            centerXProperty().addListener((observableValue, oldNumber, newNumber) ->
                                                  BoundingPolygonView.this.getPoints().set(pointIndex.get(),
                                                                                           newNumber.doubleValue())
            );

            centerYProperty().addListener((observableValue, oldNumber, newNumber) ->
                                                  BoundingPolygonView.this.getPoints().set(pointIndex.get() + 1,
                                                                                           newNumber.doubleValue())
            );

            strokeProperty().bind(Bindings.when(editing)
                                          .then(Bindings.createObjectBinding(() ->
                                                                                     ((Color) getFill())
                                                                                             .getBrightness() >
                                                                                             BRIGHTNESS_BLACK_SWITCH_THRESHOLD
                                                                                             ? Color.BLACK :
                                                                                             Color.WHITE,
                                                                             fillProperty()))
                                          .otherwise(Bindings.createObjectBinding(() -> ((Color) getFill()),
                                                                                  fillProperty())));
        }

        private BooleanProperty createEditingProperty() {
            return new BooleanPropertyBase(false) {
                @Override
                public Object getBean() {
                    return VertexHandle.this;
                }

                @Override
                public String getName() {
                    return VertexHandle.EDITING_PSEUDO_CLASS_NAME;
                }

                @Override
                protected void invalidated() {
                    pseudoClassStateChanged(editingPseudoClass, get());
                }
            };
        }
    }
}
