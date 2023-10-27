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

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
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
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.ClosePath;

import java.util.Collection;
import java.util.List;

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
    private static final double ZOOM_SCALE_DELTA = 0.05;

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
    private final ProgressIndicator imageLoadingProgressIndicator = new ProgressIndicator();
    private final StackPane contentPane = new StackPane(imageView, boundingShapeSceneGroup,
            imageLoadingProgressIndicator);
    private boolean boundingBoxDrawingInProgress = false;
    private DrawingMode drawingMode = DrawingMode.BOX;

    private boolean freehandDrawingInProgress = false;
    private String currentImageUrl = null;

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

        setUpImageView();
        setUpInternalListeners();
    }

    @Override
    public void connectToController(Controller controller) {
        imageView.setOnMouseReleased(controller::onRegisterImageViewMouseReleasedEvent);
        imageView.setOnMousePressed(controller::onRegisterImageViewMousePressedEvent);
    }

    public DrawingMode getDrawingMode() {
        return drawingMode;
    }

    public void setDrawingMode(DrawingMode drawingMode) {
        this.drawingMode = drawingMode;
    }

    public DoubleProperty simplifyRelativeDistanceToleranceProperty() {
        return simplifyRelativeDistanceTolerance;
    }

    public BooleanProperty autoSimplifyPolygonsProperty() {
        return autoSimplifyPolygons;
    }

    /**
     * Finalizes the currently drawn {@link BoundingBoxView}.
     */
    public void finalizeBoundingBox() {
        boundingBoxDrawingInProgress = false;
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
        imageView.setCursor(value ? Cursor.OPEN_HAND : Cursor.DEFAULT);
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

    /**
     * Returns a boolean indicating that a bounding box is currently drawn by the user.
     *
     * @return the boolean value
     */
    public boolean isBoundingBoxDrawingInProgress() {
        return boundingBoxDrawingInProgress;
    }

    public boolean isFreehandDrawingInProgress() {
        return freehandDrawingInProgress;
    }

    public boolean isDrawingInProgress() {
        return boundingBoxDrawingInProgress || freehandDrawingInProgress;
    }

    public void initializeBoundingRectangle(MouseEvent event) {
        Point2D parentCoordinates = imageView.localToParent(event.getX(), event.getY());

        BoundingBoxView boundingBoxView = new BoundingBoxView(selectedCategory.get());
        boundingBoxView.getConstructionAnchorLocal().setFromMouseEvent(event);
        boundingBoxView.setToggleGroup(boundingShapeSelectionGroup);

        boundingBoxView.setX(parentCoordinates.getX());
        boundingBoxView.setY(parentCoordinates.getY());
        boundingBoxView.setWidth(0);
        boundingBoxView.setHeight(0);

        currentBoundingShapes.add(boundingBoxView);

        boundingBoxView.autoScaleWithBounds(imageView.boundsInParentProperty());
        boundingShapeSelectionGroup.selectToggle(boundingBoxView);

        boundingBoxDrawingInProgress = true;
    }

    public void initializeBoundingPolygon(MouseEvent event) {
        Toggle selectedBoundingShape = boundingShapeSelectionGroup.getSelectedToggle();

        BoundingPolygonView selectedBoundingPolygon;

        if(!(selectedBoundingShape instanceof BoundingPolygonView boundingPolygonView &&
                boundingPolygonView.isConstructing())) {
            selectedBoundingPolygon = new BoundingPolygonView(selectedCategory.get());
            selectedBoundingPolygon.setToggleGroup(boundingShapeSelectionGroup);
            selectedBoundingPolygon.setConstructing(true);

            currentBoundingShapes.add(selectedBoundingPolygon);

            selectedBoundingPolygon.autoScaleWithBounds(imageView.boundsInParentProperty());
            selectedBoundingPolygon.setMouseTransparent(true);
            selectedBoundingPolygon.setVisible(true);
            boundingShapeSelectionGroup.selectToggle(selectedBoundingPolygon);
        } else {
            selectedBoundingPolygon = (BoundingPolygonView) selectedBoundingShape;
        }

        Point2D parentCoordinates = imageView.localToParent(event.getX(), event.getY());
        selectedBoundingPolygon.appendNode(parentCoordinates.getX(), parentCoordinates.getY());
        selectedBoundingPolygon.setEditing(true);
    }

    public void initializeBoundingFreehandShape(MouseEvent event) {
        Point2D parentCoordinates = imageView.localToParent(event.getX(), event.getY());

        BoundingFreehandShapeView boundingFreehandShape = new BoundingFreehandShapeView(selectedCategory.get());
        boundingFreehandShape.setToggleGroup(boundingShapeSelectionGroup);

        currentBoundingShapes.add(boundingFreehandShape);


        boundingFreehandShape.autoScaleWithBounds(imageView.boundsInParentProperty());

        boundingFreehandShape.setVisible(true);
        boundingShapeSelectionGroup.selectToggle(boundingFreehandShape);
        boundingFreehandShape.addMoveTo(parentCoordinates.getX(), parentCoordinates.getY());
        freehandDrawingInProgress = true;
    }

    public void finalizeFreehandShape() {
        BoundingFreehandShapeView boundingFreehandShape = (BoundingFreehandShapeView) boundingShapeSelectionGroup
                .getSelectedToggle();

        boundingFreehandShape.getElements().add(new ClosePath());

        BoundingPolygonView boundingPolygonView = new BoundingPolygonView(
                boundingFreehandShape.getViewData().getObjectCategory());

        final List<Double> pointsInImage = boundingFreehandShape.getPointsInImage();

        boundingPolygonView.setEditing(true);

        for(int i = 0; i < pointsInImage.size(); i += 2) {
            boundingPolygonView.appendNode(pointsInImage.get(i), pointsInImage.get(i + 1));
        }

        if(autoSimplifyPolygons.get()) {
            boundingPolygonView.simplify(simplifyRelativeDistanceTolerance.get(),
                    boundingFreehandShape.getViewData().autoScaleBounds().getValue());
        }

        boundingPolygonView.setToggleGroup(boundingShapeSelectionGroup);

        currentBoundingShapes.remove(boundingFreehandShape);

        ObjectCategoryTreeItem parentTreeItem = (ObjectCategoryTreeItem) boundingFreehandShape.getViewData()
                .getTreeItem().getParent();
        parentTreeItem.detachBoundingShapeTreeItemChild(boundingFreehandShape.getViewData().getTreeItem());

        if(parentTreeItem.getChildren().isEmpty()) {
            parentTreeItem.getParent().getChildren().remove(parentTreeItem);
        }

        currentBoundingShapes.add(boundingPolygonView);

        boundingPolygonView.autoScaleWithBounds(imageView.boundsInParentProperty());
        boundingPolygonView.setVisible(true);
        boundingShapeSelectionGroup.selectToggle(boundingPolygonView);
        setBoundingPolygonsEditingAndConstructing(false);

        freehandDrawingInProgress = false;
    }

    public void setBoundingPolygonsEditingAndConstructing(boolean editing) {
        currentBoundingShapes.stream()
                .filter(BoundingPolygonView.class::isInstance)
                .map(BoundingPolygonView.class::cast)
                .forEach(boundingPolygonView -> {
                    boundingPolygonView.setEditing(editing);
                    boundingPolygonView.setConstructing(false);
                });
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
                    final Point2D clampedEventXY =
                            MathUtils.clampWithinBounds(event.getX(), event.getY(), imageView.getBoundsInLocal());

                    if(drawingMode == DrawingMode.BOX) {
                        updateCurrentBoundingBoxFromMouseDrag(clampedEventXY);
                    } else if(drawingMode == DrawingMode.FREEHAND) {
                        updateCurrentFreehandShapeFromMouseDrag(clampedEventXY);
                    }
                }
            }
        });
    }

    private void updateCurrentFreehandShapeFromMouseDrag(Point2D clampedEventXY) {
        Point2D parentCoordinates =
                imageView.localToParent(clampedEventXY.getX(), clampedEventXY.getY());

        BoundingFreehandShapeView shape =
                (BoundingFreehandShapeView) boundingShapeSelectionGroup.getSelectedToggle();

        shape.addLineTo(parentCoordinates.getX(), parentCoordinates.getY());
    }

    private void updateCurrentBoundingBoxFromMouseDrag(Point2D clampedEventXY) {
        BoundingBoxView boundingBoxView =
                (BoundingBoxView) boundingShapeSelectionGroup.getSelectedToggle();

        DragAnchor constructionAnchor = boundingBoxView.getConstructionAnchorLocal();
        Point2D parentCoordinates =
                imageView.localToParent(Math.min(clampedEventXY.getX(), constructionAnchor.getX()),
                        Math.min(clampedEventXY.getY(), constructionAnchor.getY()));

        boundingBoxView.setX(parentCoordinates.getX());
        boundingBoxView.setY(parentCoordinates.getY());
        boundingBoxView.setWidth(Math.abs(clampedEventXY.getX() - constructionAnchor.getX()));
        boundingBoxView.setHeight(Math.abs(clampedEventXY.getY() - constructionAnchor.getY()));
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

                double zoomFactor = 1.0 + Math.signum(event.getDeltaY()) * ZOOM_SCALE_DELTA;

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

    public enum DrawingMode {BOX, POLYGON, FREEHAND}
}
