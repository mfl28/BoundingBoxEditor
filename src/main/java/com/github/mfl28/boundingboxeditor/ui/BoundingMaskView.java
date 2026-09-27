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

import com.github.mfl28.boundingboxeditor.model.data.BoundingMaskData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.MaskBitmap;
import com.github.mfl28.boundingboxeditor.model.data.MutableMask;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Arrays;

/**
 * A pixel mask on the image, shown in the color of its category. The mask is drawn with the brush of the mask
 * drawing mode ({@link BoundingMaskDrawer}); in the other modes it can be selected (by clicking one of its pixels)
 * and moved like the other shapes.
 */
public class BoundingMaskView extends ImageView implements View, BoundingShapeDataConvertible, BoundingShapeToggle {
    // The mask is rendered with at most the resolution the editor loads images with.
    static final int MAXIMUM_RENDER_SIZE = 3072;
    private static final int MAXIMUM_CLIP_SIZE = 4096;
    private static final String BOUNDING_MASK_VIEW_ID = "bounding-mask";
    private static final String OUTLINE_STYLE_CLASS = "bounding-mask-outline";
    private static final double DEFAULT_OPACITY = 0.45;
    private static final double HIGHLIGHTED_OPACITY = 0.6;
    private static final double SELECTED_OPACITY = 0.7;

    private final BoundingShapeViewData boundingShapeViewData;
    // Outlines the mask's bounds while it is selected.
    private final Rectangle outline = new Rectangle();
    private final ChangeListener<Color> categoryColorListener = (observable, oldValue, newValue) -> render();
    private MaskBitmap mask;
    // While painting: the edited mask and an image covering the whole image area that shows it.
    private MutableMask paintedMask;
    private WritableImage paintingImage;
    // While the mask is dragged: the image pixel under the pointer at the press, and the offset so far (in whole
    // image pixels, so that zooming during the drag doesn't change it).
    private Point2D dragStartInMask;
    private int dragOffsetX;
    private int dragOffsetY;

    /**
     * Creates a new mask view.
     *
     * @param objectCategory the category
     * @param mask           the mask
     */
    public BoundingMaskView(ObjectCategory objectCategory, MaskBitmap mask) {
        this.mask = mask;
        this.boundingShapeViewData = new BoundingShapeViewData(this, objectCategory);

        setManaged(false);
        setId(BOUNDING_MASK_VIEW_ID);
        // Pixels are shown as squares when zoomed in, so the mask's edges are exact.
        setSmooth(false);
        setPreserveRatio(false);
        // Only the mask's pixels (not its bounding rectangle) react to the mouse.
        setPickOnBounds(false);

        outline.getStyleClass().add(OUTLINE_STYLE_CLASS);
        outline.setManaged(false);
        outline.setMouseTransparent(true);
        outline.setFill(Color.TRANSPARENT);
        outline.visibleProperty().bind(visibleProperty().and(selectedProperty()));

        boundingShapeViewData.getNodeGroup().setManaged(false);
        boundingShapeViewData.getNodeGroup().getChildren().add(outline);

        setUpInternalListeners();
        addMoveFunctionality();
        render();
    }

    /**
     * Creates a mask view from mask data.
     *
     * @param boundingMaskData the data
     * @return the view
     */
    public static BoundingMaskView fromData(BoundingMaskData boundingMaskData) {
        final BoundingMaskView view = new BoundingMaskView(boundingMaskData.getCategory(),
                                                           boundingMaskData.getMask());
        view.boundingShapeViewData.getTags().setAll(boundingMaskData.getTags());
        return view;
    }

    public MaskBitmap getMask() {
        return mask;
    }

    /**
     * Replaces the mask (e.g. after it was moved) and redraws it.
     *
     * @param mask the new mask
     */
    void setMask(MaskBitmap mask) {
        this.mask = mask;
        render();
        updateLayout();
    }

    @Override
    public BoundingShapeData toBoundingShapeData() {
        return new BoundingMaskData(boundingShapeViewData.getObjectCategory(), mask, boundingShapeViewData.getTags());
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
        return new Rectangle2D((double) mask.getMinX() / mask.getImageWidth(),
                               (double) mask.getMinY() / mask.getImageHeight(),
                               (double) mask.getWidth() / mask.getImageWidth(),
                               (double) mask.getHeight() / mask.getImageHeight());
    }

    @Override
    public BoundingShapeTreeItem toTreeItem() {
        return new BoundingMaskTreeItem(this);
    }

    @Override
    public void moveBy(double dx, double dy) {
        final Bounds imageBounds = boundingShapeViewData.autoScaleBounds().getValue();

        if(imageBounds == null || mask.isEmpty()) {
            return;
        }

        final int pixelsX = (int) Math.round(dx * mask.getImageWidth() / imageBounds.getWidth());
        final int pixelsY = (int) Math.round(dy * mask.getImageHeight() / imageBounds.getHeight());
        setMask(mask.translated(clampMoveX(pixelsX), clampMoveY(pixelsY)));
    }

    void autoScaleWithBounds(ReadOnlyObjectProperty<Bounds> autoScaleBounds) {
        boundingShapeViewData.autoScaleBounds().bind(autoScaleBounds);
        boundingShapeViewData.autoScaleBounds().addListener((observable, oldValue, newValue) -> {
            updateLayout();
            updateDragTranslation();
        });
        updateLayout();
    }

    /**
     * Returns whether a point lies on a pixel of the mask.
     *
     * @param x the x-coordinate in the parent's coordinate system (the one of the image view's bounds)
     * @param y the y-coordinate
     * @return true if the point lies on a set pixel
     */
    boolean containsPointInParent(double x, double y) {
        final Bounds imageBounds = boundingShapeViewData.autoScaleBounds().getValue();

        if(imageBounds == null) {
            return false;
        }

        return mask.get((int) Math.floor(toMaskX(x, imageBounds)), (int) Math.floor(toMaskY(y, imageBounds)));
    }

    /**
     * Creates an image of the mask's bounds that is opaque where the mask is set and transparent elsewhere, e.g. to
     * clip an image of the object to the mask.
     *
     * @param width  the width the image is shown with
     * @param height the height the image is shown with
     * @return the image (at most {@value #MAXIMUM_CLIP_SIZE} pixels on each side)
     */
    Image createClipImage(double width, double height) {
        final int imageWidth = Math.clamp(Math.round(width), 1, MAXIMUM_CLIP_SIZE);
        final int imageHeight = Math.clamp(Math.round(height), 1, MAXIMUM_CLIP_SIZE);
        final WritableImage image = new WritableImage(imageWidth, imageHeight);
        final int[] row = new int[imageWidth];

        for(int y = 0; y < imageHeight; ++y) {
            final int maskY = mask.getMinY() + (int) ((y + 0.5) * mask.getHeight() / imageHeight);

            for(int x = 0; x < imageWidth; ++x) {
                final int maskX = mask.getMinX() + (int) ((x + 0.5) * mask.getWidth() / imageWidth);
                row[x] = mask.get(maskX, maskY) ? 0xFFFFFFFF : 0;
            }

            image.getPixelWriter().setPixels(0, y, imageWidth, 1, PixelFormat.getIntArgbInstance(), row, 0,
                                             imageWidth);
        }

        return image;
    }

    /**
     * Starts a painting stroke: until {@link #finishPainting()} or {@link #cancelPainting()}, {@link #paintLine}
     * edits a copy of the mask, and the whole image area is shown.
     */
    void startPainting() {
        paintedMask = MutableMask.of(mask);
        final double scale = renderScale();
        paintingImage = new WritableImage(Math.max(1, (int) Math.ceil(mask.getImageWidth() * scale)),
                                          Math.max(1, (int) Math.ceil(mask.getImageHeight() * scale)));

        if(!mask.isEmpty()) {
            renderRegion(paintingImage, 0, 0, mask.getMinX(), mask.getMinY(), mask.getMinX() + mask.getWidth(),
                         mask.getMinY() + mask.getHeight());
        }

        setImage(paintingImage);
        updateLayout();
    }

    /**
     * Converts a point to pixel coordinates of the mask's image, at the current size and position of the image.
     *
     * @param x the x-coordinate in the parent's coordinate system (the one of the image view's bounds)
     * @param y the y-coordinate
     * @return the point in image pixels, or null if the image's bounds are not known yet
     */
    Point2D toMaskPoint(double x, double y) {
        final Bounds imageBounds = boundingShapeViewData.autoScaleBounds().getValue();
        return imageBounds == null ? null : new Point2D(toMaskX(x, imageBounds), toMaskY(y, imageBounds));
    }

    /**
     * Converts a length (e.g. the brush radius) to image pixels, at the current size of the image.
     *
     * @param length the length in the parent's coordinate system
     * @return the length in image pixels
     */
    double toMaskLength(double length) {
        final Bounds imageBounds = boundingShapeViewData.autoScaleBounds().getValue();
        return imageBounds == null ? length : length * mask.getImageWidth() / imageBounds.getWidth();
    }

    /**
     * Paints or erases a line with a round brush. The points are in image pixels (see {@link #toMaskPoint}), so
     * that a stroke stays where it was painted when the image is zoomed or scrolled in between.
     *
     * @param from   the start
     * @param to     the end
     * @param radius the radius of the brush in image pixels
     * @param paint  true to paint, false to erase
     */
    void paintLine(Point2D from, Point2D to, double radius, boolean paint) {
        if(paintedMask == null) {
            return;
        }

        final int[] changed = paintedMask.strokeLine(from.getX(), from.getY(), to.getX(), to.getY(), radius, paint);

        if(changed != null) {
            renderRegion(paintingImage, 0, 0, changed[0], changed[1], changed[2], changed[3]);
        }
    }

    /**
     * Ends a painting stroke and keeps the painted mask.
     */
    void finishPainting() {
        if(paintedMask == null) {
            return;
        }

        mask = paintedMask.toBitmap();
        endPainting();
    }

    /**
     * Ends a painting stroke and restores the mask from before it.
     */
    void cancelPainting() {
        endPainting();
    }

    boolean isPainting() {
        return paintedMask != null;
    }

    private void endPainting() {
        paintedMask = null;
        paintingImage = null;
        render();
        updateLayout();
    }

    private double renderScale() {
        return Math.min(1.0, (double) MAXIMUM_RENDER_SIZE / Math.max(mask.getImageWidth(), mask.getImageHeight()));
    }

    private double toMaskX(double x, Bounds imageBounds) {
        return (x - imageBounds.getMinX()) * mask.getImageWidth() / imageBounds.getWidth();
    }

    private double toMaskY(double y, Bounds imageBounds) {
        return (y - imageBounds.getMinY()) * mask.getImageHeight() / imageBounds.getHeight();
    }

    private int clampMoveX(int pixels) {
        return Math.clamp(pixels, -mask.getMinX(), mask.getImageWidth() - mask.getMinX() - mask.getWidth());
    }

    private int clampMoveY(int pixels) {
        return Math.clamp(pixels, -mask.getMinY(), mask.getImageHeight() - mask.getMinY() - mask.getHeight());
    }

    /**
     * Shows the mask's bounds (or, while painting, the whole image area) in the color of the category.
     */
    private void render() {
        if(isPainting()) {
            return;
        }

        if(mask.isEmpty()) {
            setImage(null);
            return;
        }

        final double scale = renderScale();
        final int fromX = (int) Math.floor(mask.getMinX() * scale);
        final int fromY = (int) Math.floor(mask.getMinY() * scale);
        final int toX = Math.max(fromX + 1, (int) Math.ceil((mask.getMinX() + mask.getWidth()) * scale));
        final int toY = Math.max(fromY + 1, (int) Math.ceil((mask.getMinY() + mask.getHeight()) * scale));
        final WritableImage image = new WritableImage(toX - fromX, toY - fromY);
        renderRegion(image, fromX, fromY, mask.getMinX(), mask.getMinY(), mask.getMinX() + mask.getWidth(),
                     mask.getMinY() + mask.getHeight());
        setImage(image);
    }

    /**
     * Draws the mask pixels of a region into an image whose pixel (0, 0) lies at (imageOriginX, imageOriginY) of
     * the rendered image grid. Each rendered pixel shows the mask pixel at its center.
     */
    private void renderRegion(WritableImage image, int imageOriginX, int imageOriginY,
                              int maskFromX, int maskFromY, int maskToX, int maskToY) {
        final double scale = renderScale();
        final int fromX = Math.max(imageOriginX, (int) Math.floor(maskFromX * scale));
        final int fromY = Math.max(imageOriginY, (int) Math.floor(maskFromY * scale));
        final int toX = Math.min(imageOriginX + (int) image.getWidth(), (int) Math.ceil(maskToX * scale));
        final int toY = Math.min(imageOriginY + (int) image.getHeight(), (int) Math.ceil(maskToY * scale));

        if(fromX >= toX || fromY >= toY) {
            return;
        }

        final int color = toArgb(boundingShapeViewData.getObjectCategory() != null
                                         ? boundingShapeViewData.getObjectCategory().getColor() : Color.GRAY);
        final int width = toX - fromX;
        final int[] row = new int[width];

        for(int y = fromY; y < toY; ++y) {
            final int maskY = (int) ((y + 0.5) / scale);
            Arrays.fill(row, 0);

            for(int x = fromX; x < toX; ++x) {
                final int maskX = (int) ((x + 0.5) / scale);

                if(paintedMask != null ? paintedMask.get(maskX, maskY) : mask.get(maskX, maskY)) {
                    row[x - fromX] = color;
                }
            }

            image.getPixelWriter().setPixels(fromX - imageOriginX, y - imageOriginY, width, 1,
                                             PixelFormat.getIntArgbInstance(), row, 0, width);
        }
    }

    private static int toArgb(Color color) {
        return 0xFF000000 | ((int) Math.round(color.getRed() * 255) << 16)
                | ((int) Math.round(color.getGreen() * 255) << 8) | (int) Math.round(color.getBlue() * 255);
    }

    /**
     * Positions the mask (or, while painting, the whole image area) over the image.
     */
    private void updateLayout() {
        final Bounds imageBounds = boundingShapeViewData.autoScaleBounds().getValue();

        if(imageBounds == null) {
            return;
        }

        final double scaleX = imageBounds.getWidth() / mask.getImageWidth();
        final double scaleY = imageBounds.getHeight() / mask.getImageHeight();

        if(isPainting()) {
            setX(imageBounds.getMinX());
            setY(imageBounds.getMinY());
            setFitWidth(imageBounds.getWidth());
            setFitHeight(imageBounds.getHeight());
        } else if(getImage() != null) {
            // The rendered image covers whole rendered pixels, which can reach slightly beyond the mask's bounds.
            final double scale = renderScale();
            final double renderedMinX = Math.floor(mask.getMinX() * scale) / scale;
            final double renderedMinY = Math.floor(mask.getMinY() * scale) / scale;
            setX(imageBounds.getMinX() + renderedMinX * scaleX);
            setY(imageBounds.getMinY() + renderedMinY * scaleY);
            setFitWidth(getImage().getWidth() / scale * scaleX);
            setFitHeight(getImage().getHeight() / scale * scaleY);
        }

        outline.setX(imageBounds.getMinX() + mask.getMinX() * scaleX);
        outline.setY(imageBounds.getMinY() + mask.getMinY() * scaleY);
        outline.setWidth(mask.getWidth() * scaleX);
        outline.setHeight(mask.getHeight() * scaleY);
    }

    private void setUpInternalListeners() {
        opacityProperty().bind(Bindings.when(selectedProperty()).then(SELECTED_OPACITY)
                                       .otherwise(Bindings.when(boundingShapeViewData.highlightedProperty())
                                                          .then(HIGHLIGHTED_OPACITY)
                                                          .otherwise(DEFAULT_OPACITY)));

        // Smaller masks are drawn on top, so that they can be selected; the selected one is on top of all.
        boundingShapeViewData.getNodeGroup().viewOrderProperty().bind(
                Bindings.when(selectedProperty()).then(0)
                        .otherwise(Bindings.createDoubleBinding(
                                () -> Math.min(outline.getWidth(), outline.getHeight()),
                                outline.widthProperty(), outline.heightProperty())));

        boundingShapeViewData.getSelected().addListener((observable, oldValue, newValue) -> {
            if(Boolean.TRUE.equals(newValue)) {
                boundingShapeViewData.getHighlighted().set(false);
            }
        });

        // (The category was already set by the view data's constructor.)
        final ObjectCategory category = boundingShapeViewData.getObjectCategory();

        if(category != null) {
            category.colorProperty().addListener(categoryColorListener);
            outline.strokeProperty().bind(category.colorProperty());
        }

        boundingShapeViewData.objectCategoryProperty().addListener((observable, oldValue, newValue) -> {
            outline.strokeProperty().unbind();

            if(oldValue != null) {
                oldValue.colorProperty().removeListener(categoryColorListener);
            }

            if(newValue != null) {
                newValue.colorProperty().addListener(categoryColorListener);
                outline.strokeProperty().bind(newValue.colorProperty());
            }

            render();
        });
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
                    dragStartInMask = pointerInMask(event.getSceneX(), event.getSceneY());
                    dragOffsetX = 0;
                    dragOffsetY = 0;
                }

                event.consume();
            }
        });

        // While dragging, the whole node group is shifted; the mask itself is moved on release, by whole pixels.
        setOnMouseDragged(event -> {
            if(!event.isShortcutDown() && event.getButton().equals(MouseButton.PRIMARY)) {
                final Point2D pointer = pointerInMask(event.getSceneX(), event.getSceneY());

                if(dragStartInMask != null && pointer != null) {
                    dragOffsetX = clampMoveX((int) Math.round(pointer.getX() - dragStartInMask.getX()));
                    dragOffsetY = clampMoveY((int) Math.round(pointer.getY() - dragStartInMask.getY()));
                    updateDragTranslation();
                }

                event.consume();
            }
        });

        setOnMouseReleased(event -> {
            final int dx = dragOffsetX;
            final int dy = dragOffsetY;
            dragStartInMask = null;
            dragOffsetX = 0;
            dragOffsetY = 0;
            updateDragTranslation();

            if(dx != 0 || dy != 0) {
                setMask(mask.translated(dx, dy));
            }
        });
    }

    /**
     * Returns the image pixel under a point of the scene. The node group's parent is used, because the node group
     * itself is shifted while the mask is dragged.
     */
    private Point2D pointerInMask(double sceneX, double sceneY) {
        final var parent = boundingShapeViewData.getNodeGroup().getParent();

        if(parent == null) {
            return null;
        }

        final Point2D point = parent.sceneToLocal(sceneX, sceneY);
        return toMaskPoint(point.getX(), point.getY());
    }

    /**
     * Shifts the node group by the drag offset, at the current size of the image.
     */
    private void updateDragTranslation() {
        final Bounds imageBounds = boundingShapeViewData.autoScaleBounds().getValue();

        if(imageBounds == null) {
            return;
        }

        boundingShapeViewData.getNodeGroup().setTranslateX(dragOffsetX * imageBounds.getWidth() / mask.getImageWidth());
        boundingShapeViewData.getNodeGroup().setTranslateY(dragOffsetY * imageBounds.getHeight()
                                                                   / mask.getImageHeight());
    }
}
