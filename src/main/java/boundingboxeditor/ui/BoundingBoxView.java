package boundingboxeditor.ui;

import boundingboxeditor.model.BoundingBoxCategory;
import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.io.BoundingBoxData;
import boundingboxeditor.utils.MathUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents the visual (UI)-component of a bounding-box. To the app-user, instances of
 * this class serve as fully resizable and movable rectangles which are used to specify the bounds
 * of objects in an image. Implements the {@link Toggle} interface to allow a single-selection
 * mechanism.
 *
 * @see Rectangle
 * @see View
 */
public class BoundingBoxView extends Rectangle implements View, Toggle {
    private static final double HIGHLIGHTED_FILL_OPACITY = 0.3;
    private static final double SELECTED_FILL_OPACITY = 0.5;
    private static final BoundingBoxView NULL_BOUNDING_BOX_VIEW = new BoundingBoxView();
    private static final String BOUNDING_BOX_VIEW_ID = "bounding-rectangle";

    private final DragAnchor dragAnchor = new DragAnchor();
    private final Property<Bounds> autoScaleBounds = new SimpleObjectProperty<>();
    private final Group nodeGroup = new Group(this);
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final BooleanProperty highlighted = new SimpleBooleanProperty(false);
    private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<>();
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private Bounds boundsInImage;
    private TreeItem<BoundingBoxView> treeItem;
    private BoundingBoxCategory boundingBoxCategory;
    private ImageMetaData imageMetaData;

    /**
     * Creates a new bounding-box UI-element, which takes the shape of a rectangle that is resizable
     * and movable by the user.
     *
     * @param boundingBoxCategory the category this bounding-box will be assigned to
     * @param imageMetaData       the image-meta data of the image, this bounding-box will be displayed on (this
     *                            contains the original size of the image which is needed when transforming the visual bounding-box component
     *                            into the data component represented by the {@link BoundingBoxData} class)
     */
    public BoundingBoxView(BoundingBoxCategory boundingBoxCategory, ImageMetaData imageMetaData) {
        this.imageMetaData = imageMetaData;
        this.boundingBoxCategory = boundingBoxCategory;

        setManaged(false);
        setFill(Color.TRANSPARENT);
        setId(BOUNDING_BOX_VIEW_ID);

        nodeGroup.setManaged(false);
        nodeGroup.getChildren().addAll(createResizeHandles());

        addMoveFunctionality();
        setUpInternalListeners();
    }

    private BoundingBoxView() {
    }

    @Override
    public ToggleGroup getToggleGroup() {
        return toggleGroup.get();
    }

    @Override
    public void setToggleGroup(ToggleGroup toggleGroup) {
        this.toggleGroup.set(toggleGroup);
    }

    @Override
    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroup;
    }

    @Override
    public boolean isSelected() {
        return selected.get();
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    @Override
    public BooleanProperty selectedProperty() {
        return selected;
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundingBoxCategory, imageMetaData, getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingBoxView)) {
            return false;
        }

        BoundingBoxView other = (BoundingBoxView) obj;

        return Objects.equals(boundingBoxCategory, other.boundingBoxCategory) && Objects.equals(imageMetaData, other.imageMetaData)
                && MathUtils.doubleAlmostEqual(getX(), other.getX()) && MathUtils.doubleAlmostEqual(getY(), other.getY())
                && MathUtils.doubleAlmostEqual(getWidth(), other.getWidth()) && MathUtils.doubleAlmostEqual(getHeight(), other.getHeight());
    }

    @Override
    public String toString() {
        return "BoundingBoxView[x=" + getX() + ", y=" + getY() + ", width=" + getWidth() + ", height=" + getHeight()
                + ", fill=" + getFill() + ", category=" + boundingBoxCategory + ", image-metadata=" + imageMetaData + "]";
    }

    /**
     * Returns the associated {@link BoundingBoxCategory} object.
     *
     * @return the {@link BoundingBoxCategory} object
     */
    public BoundingBoxCategory getBoundingBoxCategory() {
        return boundingBoxCategory;
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
     * Extracts a {@link BoundingBoxData} object used to store the 'blueprint' of this {@link BoundingBoxView}
     * object and returns it.
     *
     * @return the {@link  BoundingBoxData} object
     */
    BoundingBoxData toBoundingBoxData() {
        return new BoundingBoxData(boundingBoxCategory, getImageRelativeBounds(), tags);
    }

    /**
     * Returns the currently assigned tags.
     *
     * @return the tags
     */
    ObservableList<String> getTags() {
        return tags;
    }

    /**
     * Returns the associated {@link TreeItem} object.
     *
     * @return the {@link TreeItem} object
     */
    TreeItem<BoundingBoxView> getTreeItem() {
        return treeItem;
    }

    /**
     * Sets the associated {@link TreeItem} object.
     */
    void setTreeItem(TreeItem<BoundingBoxView> treeItem) {
        this.treeItem = treeItem;
    }

    /**
     * Anchors the {@link BoundingBoxView} object to and automatically scales it with the
     * provided {@link Bounds}-property.
     *
     * @param autoScaleBounds the auto-scale-bounds property used to anchor this {@link BoundingBoxView} object
     */
    void autoScaleWithBounds(ReadOnlyObjectProperty<Bounds> autoScaleBounds) {
        this.autoScaleBounds.bind(autoScaleBounds);
        addAutoScaleListener();
    }

    /**
     * Creates a new {@link BoundingBoxView} object from stored bounding-box data. This function is called
     * when bounding-box data stored in the model-component should be transformed to the visual bounding-box
     * component which is displayed to the user.
     *
     * @param boundingBoxData the stored {@link BoundingBoxData} object used to construct the new {@link BoundingBoxView} object
     * @param metaData        the {@link ImageMetaData} object that should be assigned to the new {@link BoundingBoxView} object
     * @return the new {@link BoundingBoxView} object
     */
    static BoundingBoxView fromData(BoundingBoxData boundingBoxData, ImageMetaData metaData) {
        BoundingBoxView boundingBox = new BoundingBoxView(boundingBoxData.getCategory(), metaData);
        boundingBox.setBoundsInImage(boundingBoxData.getBoundsInImage());
        boundingBox.getTags().setAll(boundingBoxData.getTags());
        return boundingBox;
    }

    /**
     * Anchors the {@link BoundingBoxView} object to and automatically scales it with the provided {@link Bounds}-property.
     * Initializes the {@link BoundingBoxView} with the correctly scaled size relative to the current size of the
     * value of the autoScaleBounds-property.
     *
     * @param autoScaleBounds the bounds-property to scale with
     */
    void autoScaleWithBoundsAndInitialize(ReadOnlyObjectProperty<Bounds> autoScaleBounds) {
        this.autoScaleBounds.bind(autoScaleBounds);
        initializeFromBoundsInImage();
        addAutoScaleListener();
    }

    /**
     * Returns a static 'empty' dummy-{@link BoundingBoxView} object. This is only used
     * to pass the necessary {@link BoundingBoxView} object to a {@link BoundingBoxCategoryTreeItem}
     * for it nor to be considered 'empty' by the {@link BoundingBoxTreeView}.
     *
     * @return the dummy object
     */
    static BoundingBoxView getDummy() {
        return NULL_BOUNDING_BOX_VIEW;
    }

    /**
     * Returns a {@link Group} object whose children are the components that make
     * up this {@link BoundingBoxView} UI-element (the rectangle itself as well as the resize-handles).
     * This function is used when the bounding-box should be added to the scene-graph.
     *
     * @return the group
     */
    Group getNodeGroup() {
        return nodeGroup;
    }

    /**
     * Sets the highlighted-status of the bounding-box.
     *
     * @param highlighted true to set highlighting on, otherwise off
     */
    void setHighlighted(boolean highlighted) {
        this.highlighted.set(highlighted);
    }

    private List<ResizeHandle> createResizeHandles() {
        return Arrays.stream(CompassPoint.values())
                .map(ResizeHandle::new)
                .collect(Collectors.toList());
    }

    private void addMoveFunctionality() {
        setOnMouseEntered(event -> {
            setCursor(Cursor.MOVE);

            if(!isSelected()) {
                highlighted.set(true);
            }

            event.consume();
        });

        setOnMouseExited(event -> {
            if(!isSelected()) {
                highlighted.set(false);
            }
        });

        setOnMousePressed(event -> {
            if(!event.isControlDown()) {
                getToggleGroup().selectToggle(this);

                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    dragAnchor.setCoordinates(event.getX() - getX(), event.getY() - getY());
                }

                event.consume();
            }
        });

        setOnMouseDragged(event -> {
            if(!event.isControlDown()) {
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
        nodeGroup.viewOrderProperty().bind(
                Bindings.when(selectedProperty())
                        .then(0)
                        .otherwise(Bindings.min(widthProperty(), heightProperty()))
        );

        strokeProperty().bind(boundingBoxCategory.colorProperty());

        fillProperty().bind(Bindings.when(selectedProperty())
                .then(Bindings.createObjectBinding(() -> Color.web(strokeProperty().get().toString(), SELECTED_FILL_OPACITY), strokeProperty()))
                .otherwise(Bindings.when(highlighted)
                        .then(Bindings.createObjectBinding(() -> Color.web(strokeProperty().get().toString(), HIGHLIGHTED_FILL_OPACITY), strokeProperty()))
                        .otherwise(Color.TRANSPARENT)));

        selected.addListener((observable, oldValue, newValue) -> {
            if(Boolean.TRUE.equals(newValue)) {
                highlighted.set(false);
            }
        });
    }

    private Bounds constructCurrentMoveBounds() {
        final Bounds confinementBoundsValue = autoScaleBounds.getValue();
        return new BoundingBox(confinementBoundsValue.getMinX(), confinementBoundsValue.getMinY(),
                confinementBoundsValue.getWidth() - getWidth(), confinementBoundsValue.getHeight() - getHeight());
    }

    private Bounds getImageRelativeBounds() {
        final Bounds imageViewBounds = autoScaleBounds.getValue();

        double widthScaleFactor = imageMetaData.getImageWidth() / imageViewBounds.getWidth();
        double heightScaleFactor = imageMetaData.getImageHeight() / imageViewBounds.getHeight();

        double xMinRelative = (getX() - imageViewBounds.getMinX()) * widthScaleFactor;
        double yMinRelative = (getY() - imageViewBounds.getMinY()) * heightScaleFactor;
        double widthRelative = getWidth() * widthScaleFactor;
        double heightRelative = getHeight() * heightScaleFactor;

        return new BoundingBox(xMinRelative, yMinRelative, widthRelative, heightRelative);
    }

    private void addAutoScaleListener() {
        autoScaleBounds.addListener((observable, oldValue, newValue) -> {
            setWidth(getWidth() * newValue.getWidth() / oldValue.getWidth());
            setHeight(getHeight() * newValue.getHeight() / oldValue.getHeight());

            setX(newValue.getMinX() + (getX() - oldValue.getMinX()) * newValue.getWidth() / oldValue.getWidth());
            setY(newValue.getMinY() + (getY() - oldValue.getMinY()) * newValue.getHeight() / oldValue.getHeight());
        });
    }

    private void setBoundsInImage(Bounds boundsInImage) {
        this.boundsInImage = boundsInImage;
    }

    private void initializeFromBoundsInImage() {
        Bounds confinementBoundsValue = autoScaleBounds.getValue();
        double imageWidth = imageMetaData.getImageWidth();
        double imageHeight = imageMetaData.getImageHeight();

        setX(boundsInImage.getMinX() * confinementBoundsValue.getWidth() / imageWidth + confinementBoundsValue.getMinX());
        setY(boundsInImage.getMinY() * confinementBoundsValue.getHeight() / imageHeight + confinementBoundsValue.getMinY());
        setWidth(boundsInImage.getWidth() * confinementBoundsValue.getWidth() / imageWidth);
        setHeight(boundsInImage.getHeight() * confinementBoundsValue.getHeight() / imageHeight);
    }

    private enum CompassPoint {NW, N, NE, E, SE, S, SW, W}

    private class ResizeHandle extends Rectangle {
        private static final double SIDE_LENGTH = 8.0;

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
                case NW:
                    xProperty().bind(rectangleX.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.subtract(SIDE_LENGTH / 2));
                    break;
                case N:
                    xProperty().bind(rectangleX.add(rectangleW.subtract(SIDE_LENGTH).divide(2)));
                    yProperty().bind(rectangleY.subtract(SIDE_LENGTH / 2));
                    break;
                case NE:
                    xProperty().bind(rectangleX.add(rectangleW).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.subtract(SIDE_LENGTH / 2));
                    break;
                case E:
                    xProperty().bind(rectangleX.add(rectangleW).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.add(rectangleH.subtract(SIDE_LENGTH).divide(2)));
                    break;
                case SE:
                    xProperty().bind(rectangleX.add(rectangleW).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.add(rectangleH).subtract(SIDE_LENGTH / 2));
                    break;
                case S:
                    xProperty().bind(rectangleX.add(rectangleW.subtract(SIDE_LENGTH).divide(2)));
                    yProperty().bind(rectangleY.add(rectangleH).subtract(SIDE_LENGTH / 2));
                    break;
                case SW:
                    xProperty().bind(rectangleX.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.add(rectangleH).subtract(SIDE_LENGTH / 2));
                    break;
                case W:
                    xProperty().bind(rectangleX.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangleY.add(rectangleH.subtract(SIDE_LENGTH).divide(2)));
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
                case NW:
                    setOnMouseDragged(this::handleMouseDraggedNW);
                    break;
                case N:
                    setOnMouseDragged(this::handleMouseDraggedN);
                    break;
                case NE:
                    setOnMouseDragged(this::handleMouseDraggedNE);
                    break;
                case E:
                    setOnMouseDragged(this::handleMouseDraggedE);
                    break;
                case SE:
                    setOnMouseDragged(this::handleMouseDraggedSE);
                    break;
                case S:
                    setOnMouseDragged(this::handleMouseDraggedS);
                    break;
                case SW:
                    setOnMouseDragged(this::handleMouseDraggedSW);
                    break;
                case W:
                    setOnMouseDragged(this::handleMouseDraggedW);
                    break;
            }
        }

        private void handleMouseDraggedNW(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.autoScaleBounds.getValue();
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
                final Bounds parentBounds = BoundingBoxView.this.autoScaleBounds.getValue();
                final Bounds bounds = new BoundingBox(BoundingBoxView.this.getX(), parentBounds.getMinY(),
                        BoundingBoxView.this.getWidth(), getRectangleMaxY() - parentBounds.getMinY());

                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setY(clampedEventXY.getY());
                BoundingBoxView.this.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
            }

            event.consume();
        }

        private void handleMouseDraggedNE(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.autoScaleBounds.getValue();
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
                final Bounds parentBounds = BoundingBoxView.this.autoScaleBounds.getValue();
                final Bounds bounds = new BoundingBox(BoundingBoxView.this.getX(), BoundingBoxView.this.getY(),
                        parentBounds.getMaxX() - BoundingBoxView.this.getX(), BoundingBoxView.this.getHeight());
                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                BoundingBoxView.this.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
            }

            event.consume();
        }

        private void handleMouseDraggedSE(MouseEvent event) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Bounds parentBounds = BoundingBoxView.this.autoScaleBounds.getValue();
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
                final Bounds parentBounds = BoundingBoxView.this.autoScaleBounds.getValue();
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
                final Bounds parentBounds = BoundingBoxView.this.autoScaleBounds.getValue();
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
                final Bounds parentBounds = BoundingBoxView.this.autoScaleBounds.getValue();
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
