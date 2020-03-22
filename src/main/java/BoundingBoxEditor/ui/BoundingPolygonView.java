package boundingboxeditor.ui;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.model.io.BoundingBoxData;
import boundingboxeditor.model.io.BoundingPolygonData;
import boundingboxeditor.model.io.BoundingShapeData;
import boundingboxeditor.utils.MathUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BoundingPolygonView extends Polygon implements
        View, Toggle, BoundingShapeDataConvertible, BoundingShapeViewable {
    private static final double HIGHLIGHTED_FILL_OPACITY = 0.3;
    private static final double SELECTED_FILL_OPACITY = 0.5;
    private static final String BOUNDING_POLYGON_ID = "bounding-polygon";
    private static final String EDITING_PSEUDO_CLASS_NAME = "editing";
    private static final PseudoClass EDITING_PSEUDO_CLASS = PseudoClass.getPseudoClass(EDITING_PSEUDO_CLASS_NAME);

    private final BoundingShapeViewData boundingShapeViewData;

    private final BooleanProperty editing = createEditingProperty();
    private ObservableList<VertexHandle> vertexHandles = FXCollections.observableArrayList();
    private List<Double> pointsInImage = Collections.emptyList();

    /**
     * Creates a new bounding-box UI-element, which takes the shape of a rectangle that is resizable
     * and movable by the user.
     *
     * @param objectCategory the category this bounding-box will be assigned to
     * @param imageMetaData  the image-meta data of the image, this bounding-box will be displayed on (this
     *                       contains the original size of the image which is needed when transforming the visual bounding-box component
     *                       into the data component represented by the {@link BoundingBoxData} class)
     */
    public BoundingPolygonView(ObjectCategory objectCategory, ImageMetaData imageMetaData) {
        this.boundingShapeViewData = new BoundingShapeViewData(this, objectCategory, imageMetaData);

        setManaged(false);
        setFill(Color.TRANSPARENT);
        setId(BOUNDING_POLYGON_ID);
        setStrokeWidth(2.0);

        boundingShapeViewData.getNodeGroup().setManaged(false);
        pseudoClassStateChanged(EDITING_PSEUDO_CLASS, false);
        setUpInternalListeners();
    }

    public void appendNode(double x, double y) {
        vertexHandles.add(new VertexHandle(x, y, getPoints().size()));
    }

    public ObjectCategory getObjectCategory() {
        return boundingShapeViewData.getObjectCategory();
    }

    public Group getNodeGroup() {
        return boundingShapeViewData.getNodeGroup();
    }

    public BooleanProperty highlightedProperty() {
        return boundingShapeViewData.highlightedProperty();
    }

    public boolean isHighlighted() {
        return boundingShapeViewData.isHighlighted();
    }

    public void setHighlighted(boolean highlighted) {
        boundingShapeViewData.setHighlighted(highlighted);
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
                pointsInImage.size() != other.pointsInImage.size()) {
            return false;
        }

        for(int i = 0; i != pointsInImage.size(); ++i) {
            if(!MathUtils.doubleAlmostEqual(pointsInImage.get(i), other.pointsInImage.get(i))) {
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
                getImageRelativePoints(), boundingShapeViewData.getTags());
    }

    /**
     * Returns the associated {@link ImageMetaData} object.
     *
     * @return the {@link ImageMetaData} object
     */
    public ImageMetaData getImageMetaData() {
        return boundingShapeViewData.getImageMetaData();
    }

    @Override
    public BoundingShapeViewData getViewData() {
        return boundingShapeViewData;
    }

    /**
     * Returns the associated {@link TreeItem} object.
     *
     * @return the {@link TreeItem} object
     */
    BoundingShapeTreeItem getTreeItem() {
        return boundingShapeViewData.getTreeItem();
    }

    /**
     * Sets the associated {@link TreeItem} object.
     */
    void setTreeItem(BoundingShapeTreeItem treeItem) {
        boundingShapeViewData.setTreeItem(treeItem);
    }

    /**
     * Creates a new {@link BoundingBoxView} object from stored bounding-box data. This function is called
     * when bounding-box data stored in the model-component should be transformed to the visual bounding-box
     * component which is displayed to the user.
     *
     * @param boundingPolygonData the stored {@link BoundingBoxData} object used to construct the new {@link BoundingBoxView} object
     * @param metaData            the {@link ImageMetaData} object that should be assigned to the new {@link BoundingBoxView} object
     * @return the new {@link BoundingBoxView} object
     */
    static BoundingPolygonView fromData(BoundingPolygonData boundingPolygonData, ImageMetaData metaData) {
        BoundingPolygonView boundingPolygon = new BoundingPolygonView(boundingPolygonData.getCategory(), metaData);
        boundingPolygon.pointsInImage = boundingPolygonData.getPointsInImage();
        boundingPolygon.getTags().setAll(boundingPolygonData.getTags());
        return boundingPolygon;
    }

    /**
     * Anchors the {@link BoundingBoxView} object to and automatically scales it with the
     * provided {@link Bounds}-property.
     *
     * @param autoScaleBounds the auto-scale-bounds property used to anchor this {@link BoundingBoxView} object
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

    /**
     * Anchors the {@link BoundingBoxView} object to and automatically scales it with the provided {@link Bounds}-property.
     * Initializes the {@link BoundingBoxView} with the correctly scaled size relative to the current size of the
     * value of the autoScaleBounds-property.
     *
     * @param autoScaleBounds the bounds-property to scale with
     */
    void autoScaleWithBoundsAndInitialize(ReadOnlyObjectProperty<Bounds> autoScaleBounds) {
        boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        initializeFromBoundsInImage();
        addAutoScaleListener();
    }

    List<Double> getImageRelativePoints() {
        final Bounds imageViewBounds = boundingShapeViewData.autoScaleBounds().getValue();

        double widthScaleFactor = boundingShapeViewData.getImageMetaData().getImageWidth() / imageViewBounds.getWidth();
        double heightScaleFactor = boundingShapeViewData.getImageMetaData().getImageHeight() / imageViewBounds.getHeight();

        List<Double> imageRelativePoints = new ArrayList<>(getPoints().size());

        for(VertexHandle vertexHandle : vertexHandles) {
            imageRelativePoints.add((vertexHandle.getCenterX() - imageViewBounds.getMinX()) * widthScaleFactor);
            imageRelativePoints.add((vertexHandle.getCenterY() - imageViewBounds.getMinY()) * heightScaleFactor);
        }

        return imageRelativePoints;
    }

    List<Double> getImageRelativeRatios() {
        List<Double> result = getImageRelativePoints();

        for(int i = 0; i < result.size(); i += 2) {
            result.set(i, result.get(i) / boundingShapeViewData.getImageMetaData().getImageWidth());
            result.set(i + 1, result.get(i + 1) / boundingShapeViewData.getImageMetaData().getImageHeight());
        }

        return result;
    }

    private void setUpInternalListeners() {
        strokeProperty().bind(boundingShapeViewData.getObjectCategory().colorProperty());

        fillProperty().bind(Bindings.when(selectedProperty())
                .then(Bindings.createObjectBinding(() -> Color.web(strokeProperty().get().toString(), SELECTED_FILL_OPACITY), strokeProperty()))
                .otherwise(Bindings.when(boundingShapeViewData.highlightedProperty())
                        .then(Bindings.createObjectBinding(() -> Color.web(strokeProperty().get().toString(), HIGHLIGHTED_FILL_OPACITY), strokeProperty()))
                        .otherwise(Color.TRANSPARENT)));

        boundingShapeViewData.getNodeGroup().viewOrderProperty().bind(viewOrderProperty());

        vertexHandles.addListener((ListChangeListener<VertexHandle>) c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    for(VertexHandle vertexHandle : c.getAddedSubList()) {
                        getPoints().add(vertexHandle.pointIndex, vertexHandle.getCenterX());
                        getPoints().add(vertexHandle.pointIndex + 1, vertexHandle.getCenterY());

                        for(int i = vertexHandle.pointIndex / 2 + 1; i < vertexHandles.size(); ++i) {
                            vertexHandles.get(i).pointIndex += 2;
                        }
                    }

                    boundingShapeViewData.getNodeGroup().getChildren().addAll(c.getAddedSubList());
                }

                if(c.wasRemoved()) {
                    for(VertexHandle vertexHandle : c.getRemoved()) {
                        getPoints().remove(vertexHandle.pointIndex);
                        getPoints().remove(vertexHandle.pointIndex);

                        for(int i = vertexHandle.pointIndex / 2; i < vertexHandles.size(); ++i) {
                            vertexHandles.get(i).pointIndex -= 2;
                        }
                    }

                    boundingShapeViewData.getNodeGroup().getChildren().removeAll(c.getRemoved());
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
                getToggleGroup().selectToggle(this);
                event.consume();
            }
        });

        boundingShapeViewData.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(Boolean.TRUE.equals(newValue)) {
                boundingShapeViewData.setHighlighted(false);
            }
        });
    }

    private void initializeFromBoundsInImage() {
        Bounds confinementBoundsValue = boundingShapeViewData.autoScaleBounds().getValue();
        double imageWidth = boundingShapeViewData.getImageMetaData().getImageWidth();
        double imageHeight = boundingShapeViewData.getImageMetaData().getImageHeight();

        vertexHandles.clear();

        for(int i = 0; i < pointsInImage.size(); i += 2) {
            vertexHandles.add(new VertexHandle(pointsInImage.get(i) * confinementBoundsValue.getWidth() / imageWidth + confinementBoundsValue.getMinX(),
                    pointsInImage.get(i + 1) * confinementBoundsValue.getHeight() / imageHeight + confinementBoundsValue.getMinY(), i));
        }
    }

    private void addAutoScaleListener() {
        boundingShapeViewData.autoScaleBounds().addListener((observable, oldValue, newValue) -> {
            double xScaleFactor = newValue.getWidth() / oldValue.getWidth();
            double yScaleFactor = newValue.getHeight() / oldValue.getHeight();

            for(VertexHandle vertexHandle : vertexHandles) {
                vertexHandle.setCenterX(newValue.getMinX() + (vertexHandle.getCenterX() - oldValue.getMinX()) * xScaleFactor);
                vertexHandle.setCenterY(newValue.getMinY() + (vertexHandle.getCenterY() - oldValue.getMinY()) * yScaleFactor);
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

    private class VertexHandle extends Circle {
        private static final double RADIUS = 6.0;
        private static final String VERTEX_HANDLE_ID = "vertex-handle";
        private static final String EDITING_PSEUDO_CLASS_NAME = "editing";
        private final PseudoClass editingPseudoClass = PseudoClass.getPseudoClass(EDITING_PSEUDO_CLASS_NAME);
        private final BooleanProperty editing = createEditingProperty();
        DragAnchor dragAnchor = new DragAnchor();
        BooleanProperty selected = new SimpleBooleanProperty(false);
        private int pointIndex;


        VertexHandle(double pointX, double pointY, int pointIndex) {
            super(pointX, pointY, RADIUS);
            this.pointIndex = pointIndex;

            setId(VERTEX_HANDLE_ID);
            pseudoClassStateChanged(editingPseudoClass, false);

            addMoveFunctionality();
            setUpInternalListeners();
        }

        private void addMoveFunctionality() {
            setOnMousePressed(mouseEvent -> {
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    dragAnchor.setCoordinates(mouseEvent.getX() - getCenterX(), mouseEvent.getY() - getCenterY());
                }
                mouseEvent.consume();
            });

            setOnMouseDragged(mouseEvent -> {
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    Point2D newXY = new Point2D(mouseEvent.getX() - dragAnchor.getX(), mouseEvent.getY() - dragAnchor.getY());
                    Point2D newXYConfined = MathUtils.clampWithinBounds(newXY, boundingShapeViewData.autoScaleBounds().getValue());

                    setCenterX(newXYConfined.getX());
                    setCenterY(newXYConfined.getY());
                }
                mouseEvent.consume();
            });


        }

        private void setUpInternalListeners() {
            fillProperty().bind(Bindings
                    .when(selected)
                    .then(Bindings.createObjectBinding(() -> Color.web(BoundingPolygonView.this.strokeProperty().get().toString(), 1.0).brighter(),
                            BoundingPolygonView.this.strokeProperty()))
                    .otherwise(Bindings.createObjectBinding(() -> Color.web(BoundingPolygonView.this.strokeProperty().get().toString(), 1.0),
                            BoundingPolygonView.this.strokeProperty())));

            visibleProperty().bind(BoundingPolygonView.this.visibleProperty().and(BoundingPolygonView.this.selectedProperty().or(BoundingPolygonView.this.editingProperty())));

            centerXProperty().addListener((observableValue, oldNumber, newNumber) ->
                    BoundingPolygonView.this.getPoints().set(pointIndex, newNumber.doubleValue())
            );

            centerYProperty().addListener((observableValue, oldNumber, newNumber) ->
                    BoundingPolygonView.this.getPoints().set(pointIndex + 1, newNumber.doubleValue())
            );

            editing.bind(BoundingPolygonView.this.editing);
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
                    pseudoClassStateChanged(editingPseudoClass, get());
                }
            };
        }
    }
}
