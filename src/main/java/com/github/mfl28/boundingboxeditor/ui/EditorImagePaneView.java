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
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Collection;
import java.util.Optional;

/**
 * A UI-element responsible for displaying the currently selected image on which the
 * user can draw bounding-shapes.
 *
 * @see StackPane
 * @see View
 * @see BoundingBoxView
 * @see BoundingPolygonView
 */
public class EditorImagePaneView extends ScrollPane implements View {
    private static final double IMAGE_PADDING = 0;
    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;
    private static final String IMAGE_PANE_ID = "image-pane-view";
    private static final int MAXIMUM_IMAGE_WIDTH = 3072;
    private static final int MAXIMUM_IMAGE_HEIGHT = 3072;
    private static final String BOUNDING_SHAPE_SCENE_GROUP_ID = "bounding-shape-scene-group";
    // Arrow keys move the selected shape by this many image pixels (with Shift by the larger distance).
    private static final double NUDGE_DISTANCE = 1;
    private static final double LARGE_NUDGE_DISTANCE = 10;
    // The mask brush's diameter on the screen, in pixels; [ and ] change it by this factor.
    static final double DEFAULT_MASK_BRUSH_SIZE = 20;
    static final double MINIMUM_MASK_BRUSH_SIZE = 2;
    static final double MAXIMUM_MASK_BRUSH_SIZE = 200;
    private static final double MASK_BRUSH_SIZE_STEP_FACTOR = 1.25;
    private static final String MASK_BRUSH_CURSOR_ID = "mask-brush-cursor";

    private final ImageView imageView = new ImageView();
    private final SimpleBooleanProperty maximizeImageView = new SimpleBooleanProperty(true);
    private final ColorAdjust colorAdjust = new ColorAdjust();

    private final Group boundingShapeSceneGroup = new Group();
    private final ToggleGroup boundingShapeSelectionGroup = new ToggleGroup();
    private final ObservableList<BoundingShapeViewable> currentBoundingShapes = FXCollections.observableArrayList(
            item -> new Observable[]{item.getViewData().objectCategoryProperty()});
    private final ObjectProperty<ObjectCategory> selectedCategory = new SimpleObjectProperty<>(null);
    private final DoubleProperty simplifyRelativeDistanceTolerance = new SimpleDoubleProperty(0.0);
    private final BooleanProperty autoSimplifyPolygons = new SimpleBooleanProperty(true);
    private final BooleanProperty showCategoryLabels = new SimpleBooleanProperty(false);
    private final ProgressIndicator imageLoadingProgressIndicator = new ProgressIndicator();
    private final StackPane contentPane = new StackPane(imageView, boundingShapeSceneGroup,
            imageLoadingProgressIndicator);
    private final ObjectProperty<DrawingMode> drawingMode = new SimpleObjectProperty<>(DrawingMode.BOX);
    private final DoubleProperty maskBrushSize = new SimpleDoubleProperty(DEFAULT_MASK_BRUSH_SIZE);
    private final BooleanProperty maskEraser = new SimpleBooleanProperty(false);
    // Shows the brush's size under the pointer in the mask drawing mode.
    private final Circle maskBrushCursor = new Circle();
    private boolean newMaskRequested = false;
    private final BoundingMaskDrawer.Settings maskBrushSettings = new BoundingMaskDrawer.Settings() {
        @Override
        public double getBrushSize() {
            return maskBrushSize.get();
        }

        @Override
        public boolean isErasing() {
            return maskEraser.get();
        }

        @Override
        public boolean consumeNewMaskRequest() {
            final boolean requested = newMaskRequested;
            newMaskRequested = false;
            return requested;
        }
    };
    private String currentImageUrl = null;
    // The size of the image file (oriented as shown): large images are loaded scaled down.
    private Dimension2D currentImageFileSize = null;

    private BoundingShapeDrawer boundingShapeDrawer = null;

    /**
     * Creates a new image-pane UI-element responsible for displaying the currently selected image on which the
     * user can draw bounding-shapes.
     */
    EditorImagePaneView() {
        setId(IMAGE_PANE_ID);

        setContent(contentPane);
        setFitToHeight(true);
        setFitToWidth(true);

        setVbarPolicy(ScrollBarPolicy.NEVER);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        boundingShapeSceneGroup.setManaged(false);
        boundingShapeSceneGroup.setId(BOUNDING_SHAPE_SCENE_GROUP_ID);

        setUpImageView();
        setUpInternalListeners();
    }

    @Override
    public void connectToController(Controller controller) {
        imageView.setOnMouseReleased(controller::onRegisterImageViewMouseReleasedEvent);
        imageView.setOnMousePressed(controller::onRegisterImageViewMousePressedEvent);
    }

    public void setDrawingMode(DrawingMode drawingMode) {
        this.drawingMode.set(drawingMode);
    }

    public DoubleProperty simplifyRelativeDistanceToleranceProperty() {
        return simplifyRelativeDistanceTolerance;
    }

    public BooleanProperty autoSimplifyPolygonsProperty() {
        return autoSimplifyPolygons;
    }

    public BooleanProperty showCategoryLabelsProperty() {
        return showCategoryLabels;
    }

    /**
     * Returns the property of the mask brush's diameter on the screen, in pixels.
     *
     * @return the property
     */
    public DoubleProperty maskBrushSizeProperty() {
        return maskBrushSize;
    }

    /**
     * Returns the property that switches the mask brush to erasing.
     *
     * @return the property
     */
    public BooleanProperty maskEraserProperty() {
        return maskEraser;
    }

    /**
     * Makes the next mask stroke start a new mask (instead of painting into the selected mask or the one under the
     * pointer), and deselects the selected shape.
     */
    public void requestNewMask() {
        newMaskRequested = true;
        boundingShapeSelectionGroup.selectToggle(null);
    }

    /**
     * Makes the mask brush larger or smaller by one step.
     *
     * @param larger true to make it larger
     */
    public void changeMaskBrushSize(boolean larger) {
        final double factor = larger ? MASK_BRUSH_SIZE_STEP_FACTOR : 1 / MASK_BRUSH_SIZE_STEP_FACTOR;
        maskBrushSize.set(Math.clamp(maskBrushSize.get() * factor, MINIMUM_MASK_BRUSH_SIZE, MAXIMUM_MASK_BRUSH_SIZE));
    }

    public DrawingMode getDrawingMode() {
        return drawingMode.get();
    }

    ReadOnlyObjectProperty<DrawingMode> drawingModeProperty() {
        return drawingMode;
    }

    public void initializeBoundingShapeDrawing(MouseEvent event) {
        if (isCategorySelected()) {
            boundingShapeDrawer = switch (drawingMode.get()) {
                case BOX -> new BoundingBoxDrawer(imageView, boundingShapeSelectionGroup, currentBoundingShapes);
                case POLYGON -> new BoundingPolygonDrawer(imageView, boundingShapeSelectionGroup, currentBoundingShapes);
                case FREEHAND -> new BoundingFreeHandShapeDrawer(imageView, boundingShapeSelectionGroup,
                        currentBoundingShapes, autoSimplifyPolygons,
                        simplifyRelativeDistanceTolerance);
                case MASK -> new BoundingMaskDrawer(imageView, boundingShapeSelectionGroup, currentBoundingShapes,
                        getCurrentImageFileSize(), maskBrushSettings);
                default -> null;
            };

            if (boundingShapeDrawer != null) {
                boundingShapeDrawer.initializeShape(event, selectedCategory.get());
                boundingShapeSceneGroup.setMouseTransparent(true);
            }
        }
    }

    public void updateBoundingShapeDrawing(MouseEvent event) {
        if (boundingShapeDrawer != null && boundingShapeDrawer.isDrawingInProgress()) {
            boundingShapeDrawer.updateShape(event);
        }
    }

    public void finalizeBoundingShapeDrawing() {
        if (boundingShapeDrawer != null && boundingShapeDrawer.isDrawingInProgress()) {
            boundingShapeDrawer.finalizeShape();
            updateShapesMouseTransparency();
        }
    }

    /**
     * Undoes the last step of the shape that is being drawn.
     *
     * @return the shape if nothing is left of it and the drawing was cancelled, otherwise an empty optional
     */
    Optional<BoundingShapeViewable> undoBoundingShapeDrawingStep() {
        if(!isDrawingInProgress()) {
            return Optional.empty();
        }

        final Optional<BoundingShapeViewable> cancelledShape = boundingShapeDrawer.undoLastStep();

        if(cancelledShape.isPresent() || !isDrawingInProgress()) {
            updateShapesMouseTransparency();
        }

        return cancelledShape;
    }

    public DrawingMode getCurrentBoundingShapeDrawingMode() {
        if (boundingShapeDrawer == null || !boundingShapeDrawer.isDrawingInProgress()) {
            return DrawingMode.NONE;
        }

        return boundingShapeDrawer.getDrawingMode();
    }

    /**
     * Clears the list of current bounding shape view objects.
     */
    public void removeAllCurrentBoundingShapes() {
        currentBoundingShapes.clear();
    }

    /**
     * Resets the image-view size. Sets to maximum currently possible size if isMaximizeImageView()
     * returns true, otherwise sets the image-view to the size closest to actual image-size while
     * still displaying the whole image.
     */
    public void resetImageViewSize() {
        if(isMaximizeImageView()) {
            imageView.setFitWidth(getMaxAllowedImageWidth());
            imageView.setFitHeight(getMaxAllowedImageHeight());
        } else {
            imageView.setFitWidth(Math.min(imageView.getImage().getWidth(), getMaxAllowedImageWidth()));
            imageView.setFitHeight(Math.min(imageView.getImage().getHeight(), getMaxAllowedImageHeight()));
        }
    }

    /**
     * Switches the zooming and panning functionality on and off.
     *
     * @param value true to switch on, false to switch off
     */
    public void setZoomableAndPannable(boolean value) {
        currentBoundingShapes.forEach(viewable -> {
            if(!(!value && viewable instanceof BoundingPolygonView boundingPolygonView &&
                    boundingPolygonView.isConstructing())) {
                viewable.getViewData().getBaseShape().setMouseTransparent(value);
            }
        });
        final Cursor drawingCursor = drawingMode.get() == DrawingMode.MASK ? Cursor.CROSSHAIR : Cursor.DEFAULT;
        imageView.setCursor(value ? Cursor.OPEN_HAND : drawingCursor);
        setPannable(value);
    }

    /**
     * Returns the image loading progress indicator.
     *
     * @return the progress indicator
     */
    public ProgressIndicator getImageLoadingProgressIndicator() {
        return imageLoadingProgressIndicator;
    }

    /**
     * Returns a boolean indicating that an image is currently registered and fully
     * loaded.
     *
     * @return true if an image is registered and its loading progress is equal to 1, false otherwise
     */
    public boolean isImageFullyLoaded() {
        final Image image = getCurrentImage();

        return image != null && image.getProgress() == 1.0;
    }

    public boolean isDrawingInProgress() {
        return (boundingShapeDrawer != null && boundingShapeDrawer.isDrawingInProgress());
    }

    public boolean isCategorySelected() {
        return selectedCategory.get() != null;
    }

    /**
     * Removes all provided {@link BoundingShapeViewable} objects from the list
     * of current {@link BoundingShapeViewable} objects.
     *
     * @param boundingShapes the list of objects to remove
     */
    void removeAllFromCurrentBoundingShapes(Collection<BoundingShapeViewable> boundingShapes) {
        currentBoundingShapes.removeAll(boundingShapes);
    }

    /**
     * Clears the list of current {@link BoundingShapeViewable} objects and adds all
     * objects from the provided {@link Collection}.
     *
     * @param boundingShapes the {@link BoundingShapeViewable} objects to set
     */
    void setAllCurrentBoundingShapes(Collection<BoundingShapeViewable> boundingShapes) {
        currentBoundingShapes.setAll(boundingShapes);
    }

    /**
     * Adds the provided {@link BoundingShapeViewable} objects to the boundingShapeSceneGroup which is
     * a node in the scene-graph.
     *
     * @param boundingShapes the objects to add
     */
    void addBoundingShapesToSceneGroup(Collection<? extends BoundingShapeViewable> boundingShapes) {
        boundingShapes.forEach(viewable -> viewable.getViewData().categoryLabelShownProperty().bind(showCategoryLabels));
        boundingShapeSceneGroup.getChildren().addAll(boundingShapes.stream()
                .map(viewable -> viewable.getViewData()
                        .getNodeGroup()).toList());
    }

    /**
     * Removes the provided {@link BoundingShapeViewable} objects from the boundingShapeSceneGroup which is
     * a node in the scene-graph.
     *
     * @param boundingShapes the objects to remove
     */
    void removeBoundingShapesFromSceneGroup(Collection<? extends BoundingShapeViewable> boundingShapes) {
        boundingShapes.forEach(viewable -> viewable.getViewData().categoryLabelShownProperty().unbind());
        boundingShapeSceneGroup.getChildren().removeAll(boundingShapes.stream()
                .map(viewable -> viewable.getViewData()
                        .getNodeGroup()).toList());
    }

    /**
     * Updates the displayed image from a provided {@link ImageMetaData} object.
     *
     * @param imageMetaData Metadata of the image to load.
     */
    void updateImageFromMetaData(ImageMetaData imageMetaData) {
        Dimension2D dimension = calculateLoadedImageDimensions(imageMetaData.getImageWidth(), imageMetaData.getImageHeight());

        imageView.setImage(new Image(imageMetaData.getFileUrl(),
                dimension.getWidth(), dimension.getHeight(), true, true, true));

        currentImageUrl = imageMetaData.getFileUrl();
        currentImageFileSize = new Dimension2D(imageMetaData.getOrientedWidth(), imageMetaData.getOrientedHeight());

        resetImageViewSize();
    }

    /**
     * Updates the currently shown image.
     *
     * @param image The image to show.
     * @param url   The URL of the corresponding image file.
     */
    public void updateImage(Image image, String url) {
        imageView.setImage(image);
        currentImageUrl = url;
        resetImageViewSize();
    }

    public String getCurrentImageUrl() {
        return currentImageUrl;
    }

    /**
     * Returns the {@link ToggleGroup} object used to realize the
     * single-selection mechanism for bounding shape objects.
     *
     * @return the toggle-group
     */
    ToggleGroup getBoundingShapeSelectionGroup() {
        return boundingShapeSelectionGroup;
    }

    /**
     * Returns the {@link ObservableList} of current {@link BoundingShapeViewable} objects.
     *
     * @return the list
     */
    ObservableList<BoundingShapeViewable> getCurrentBoundingShapes() {
        return currentBoundingShapes;
    }

    /**
     * Returns the property of the currently selected {@link ObjectCategory}.
     *
     * @return the property
     */
    ObjectProperty<ObjectCategory> selectedCategoryProperty() {
        return selectedCategory;
    }

    /**
     * Returns the {@link ColorAdjust} member which can be used to register effects
     * on the images.
     *
     * @return the {@link ColorAdjust} object
     */
    ColorAdjust getColorAdjust() {
        return colorAdjust;
    }

    /**
     * Returns the {@link ImageView} member which is responsible for displaying the
     * currently selected image.
     *
     * @return the image-view
     */
    ImageView getImageView() {
        return imageView;
    }

    /**
     * Returns the currently loaded {@link Image} object.
     *
     * @return the image
     */
    Image getCurrentImage() {
        return imageView.getImage();
    }

    private void setUpImageView() {
        imageView.setSmooth(true);
        imageView.setCache(false);
        imageView.setPickOnBounds(true);
        imageView.setPreserveRatio(true);
        imageView.setEffect(colorAdjust);
    }

    private void setUpInternalListeners() {
        widthProperty().addListener((value, oldValue, newValue) -> {
            if(!isMaximizeImageView()) {
                final Image image = imageView.getImage();
                double prefWidth = image != null ? image.getWidth() : 0;
                imageView.setFitWidth(Math.min(prefWidth, getMaxAllowedImageWidth()));
            } else {
                imageView.setFitWidth(getMaxAllowedImageWidth());
            }
        });

        heightProperty().addListener((value, oldValue, newValue) -> {
            if(!isMaximizeImageView()) {
                final Image image = imageView.getImage();
                double prefHeight = image != null ? image.getHeight() : 0;
                imageView.setFitHeight(Math.min(prefHeight, getMaxAllowedImageHeight()));
            } else {
                imageView.setFitHeight(getMaxAllowedImageHeight());
            }
        });

        setUpImageViewListeners();
        setUpContentPaneListeners();
        setUpMaskBrushCursor();
        drawingMode.addListener((observable, oldValue, newValue) -> {
            updateShapesMouseTransparency();
            imageView.setCursor(newValue == DrawingMode.MASK ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        });
        // A filter, because the scroll pane itself scrolls with the arrow keys.
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleNudgeKeyPressed);
    }

    private void handleNudgeKeyPressed(KeyEvent event) {
        if(event.isShortcutDown() || event.isAltDown() || isDrawingInProgress() || !isImageFullyLoaded()
                || !(boundingShapeSelectionGroup.getSelectedToggle() instanceof BoundingShapeViewable selectedShape)) {
            return;
        }

        final double distance = event.isShiftDown() ? LARGE_NUDGE_DISTANCE : NUDGE_DISTANCE;
        final double dx;
        final double dy;

        switch(event.getCode()) {
            case LEFT -> { dx = -distance; dy = 0; }
            case RIGHT -> { dx = distance; dy = 0; }
            case UP -> { dx = 0; dy = -distance; }
            case DOWN -> { dx = 0; dy = distance; }
            default -> { return; }
        }

        // Pixels of the image file to image-view coordinates.
        final Bounds imageViewBounds = imageView.getBoundsInParent();
        final Dimension2D imageSize = currentImageFileSize != null ? currentImageFileSize :
                new Dimension2D(imageView.getImage().getWidth(), imageView.getImage().getHeight());
        selectedShape.moveBy(dx * imageViewBounds.getWidth() / imageSize.getWidth(),
                             dy * imageViewBounds.getHeight() / imageSize.getHeight());
        event.consume();
    }

    private void setUpImageViewListeners() {
        imageView.setOnMouseDragged(event -> {
            if(isImageFullyLoaded()) {
                if(event.isShortcutDown()) {
                    imageView.setCursor(Cursor.CLOSED_HAND);
                }

                if(isDrawingInProgress() &&
                        event.getButton().equals(MouseButton.PRIMARY) &&
                        isCategorySelected()) {
                    boundingShapeDrawer.updateShape(event);
                }
            }
        });
    }

    private void setUpContentPaneListeners() {
        contentPane.setOnScroll(event -> {
            if(isImageFullyLoaded() && event.isShortcutDown()) {
                Bounds contentPaneBounds = contentPane.getLayoutBounds();
                Bounds viewportBounds = getViewportBounds();

                double offsetX = getHvalue() * (contentPaneBounds.getWidth() - viewportBounds.getWidth());
                double offsetY = getVvalue() * (contentPaneBounds.getHeight() - viewportBounds.getHeight());

                double minimumFitWidth = Math.min(ZOOM_MIN_WINDOW_RATIO * getWidth(), imageView.getImage().getWidth());
                double minimumFitHeight =
                        Math.min(ZOOM_MIN_WINDOW_RATIO * getHeight(), imageView.getImage().getHeight());

                double zoomFactor = ScrollZoom.computeZoomFactor(event.getDeltaY(), event.getMultiplierY());

                imageView.setFitWidth(Math.max(imageView.getFitWidth() * zoomFactor, minimumFitWidth));
                imageView.setFitHeight(Math.max(imageView.getFitHeight() * zoomFactor, minimumFitHeight));

                layout();

                Bounds newContentPaneBounds = contentPane.getBoundsInLocal();
                Point2D mousePointInImageView = imageView.parentToLocal(event.getX(), event.getY());

                if ((newContentPaneBounds.getWidth() - viewportBounds.getWidth()) == 0) {
                    setHvalue(0);
                }
                else {
                    setHvalue((mousePointInImageView.getX() * (zoomFactor - 1) + offsetX)
                            / (newContentPaneBounds.getWidth() - viewportBounds.getWidth()));
                }

                if ((newContentPaneBounds.getHeight() - viewportBounds.getHeight()) == 0) {
                    setVvalue(0);
                }
                else {
                    setVvalue((mousePointInImageView.getY() * (zoomFactor - 1) + offsetY)
                            / (newContentPaneBounds.getHeight() - viewportBounds.getHeight()));
                }

                event.consume();
            }
        });
    }

    /**
     * In the mask drawing mode, strokes over existing shapes paint (instead of selecting the shape), so the shapes
     * don't react to the mouse there.
     */
    private void updateShapesMouseTransparency() {
        boundingShapeSceneGroup.setMouseTransparent(drawingMode.get() == DrawingMode.MASK || isDrawingInProgress());
    }

    private void setUpMaskBrushCursor() {
        maskBrushCursor.setId(MASK_BRUSH_CURSOR_ID);
        maskBrushCursor.setManaged(false);
        maskBrushCursor.setMouseTransparent(true);
        maskBrushCursor.setFill(Color.TRANSPARENT);
        maskBrushCursor.setVisible(false);
        maskBrushCursor.setViewOrder(-1);
        maskBrushCursor.radiusProperty().bind(maskBrushSize.divide(2));
        boundingShapeSceneGroup.getChildren().add(maskBrushCursor);

        imageView.addEventHandler(MouseEvent.MOUSE_MOVED, this::updateMaskBrushCursor);
        imageView.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::updateMaskBrushCursor);
        imageView.addEventHandler(MouseEvent.MOUSE_EXITED, event -> maskBrushCursor.setVisible(false));
        drawingMode.addListener((observable, oldValue, newValue) -> maskBrushCursor.setVisible(false));
    }

    private void updateMaskBrushCursor(MouseEvent event) {
        final boolean shown = drawingMode.get() == DrawingMode.MASK && !event.isShortcutDown();
        maskBrushCursor.setVisible(shown);

        if(shown) {
            final Point2D center = imageView.localToParent(event.getX(), event.getY());
            maskBrushCursor.setCenterX(center.getX());
            maskBrushCursor.setCenterY(center.getY());
        }
    }

    private Dimension2D getCurrentImageFileSize() {
        if(currentImageFileSize != null) {
            return currentImageFileSize;
        }

        final Image image = imageView.getImage();
        return new Dimension2D(image.getWidth(), image.getHeight());
    }

    private boolean isMaximizeImageView() {
        return maximizeImageView.get();
    }

    /**
     * Sets the maximize-image-view property to a new value.
     *
     * @param maximizeImageView the new value
     */
    void setMaximizeImageView(boolean maximizeImageView) {
        this.maximizeImageView.set(maximizeImageView);
    }

    private double getMaxAllowedImageWidth() {
        return Math.max(0, getWidth() - 2 * IMAGE_PADDING);
    }

    private double getMaxAllowedImageHeight() {
        return Math.max(0, getHeight() - 2 * IMAGE_PADDING);
    }

    private Dimension2D calculateLoadedImageDimensions(double width, double height) {
        if(width > height) {
            return new Dimension2D(Math.min(width, MAXIMUM_IMAGE_WIDTH), 0);
        } else {
            return new Dimension2D(0, Math.min(height, MAXIMUM_IMAGE_HEIGHT));
        }
    }

    public enum DrawingMode {BOX, POLYGON, FREEHAND, MASK, NONE}
}
