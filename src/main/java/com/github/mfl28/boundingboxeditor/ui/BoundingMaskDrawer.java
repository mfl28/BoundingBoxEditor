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

import com.github.mfl28.boundingboxeditor.model.data.MaskBitmap;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Paints masks with a round brush. A stroke paints into
 * <ul>
 *     <li>the selected mask, if it has the selected category;</li>
 *     <li>otherwise, the (smallest) mask of the selected category under the pointer;</li>
 *     <li>otherwise, a new mask.</li>
 * </ul>
 * After a request for a new mask ({@link Settings#consumeNewMaskRequest()}), the next stroke always starts a new
 * mask. Erasing (with the eraser, or while Shift is held) works on the selected mask or else the mask under the
 * pointer, of any category.
 */
public class BoundingMaskDrawer implements BoundingShapeDrawer {
    private final ImageView imageView;
    private final ToggleGroup toggleGroup;
    private final List<BoundingShapeViewable> boundingShapes;
    private final Dimension2D imageFileSize;
    private final Settings settings;
    private BoundingMaskView target;
    private boolean targetIsNew;
    private boolean erasing;
    private boolean drawingInProgress = false;
    private Point2D lastPoint;

    /**
     * Creates a new mask drawer.
     *
     * @param imageView      the image view
     * @param toggleGroup    the selection group of the shapes
     * @param boundingShapes the current shapes
     * @param imageFileSize  the size of the image file (oriented as shown), which new masks get
     * @param settings       the brush settings
     */
    public BoundingMaskDrawer(ImageView imageView, ToggleGroup toggleGroup, List<BoundingShapeViewable> boundingShapes,
                              Dimension2D imageFileSize, Settings settings) {
        this.imageView = imageView;
        this.toggleGroup = toggleGroup;
        this.boundingShapes = boundingShapes;
        this.imageFileSize = imageFileSize;
        this.settings = settings;
    }

    @Override
    public void initializeShape(MouseEvent event, ObjectCategory objectCategory) {
        if(!event.getEventType().equals(MouseEvent.MOUSE_PRESSED) || !event.getButton().equals(MouseButton.PRIMARY)) {
            return;
        }

        final Point2D point = toParent(event);
        erasing = settings.isErasing() || event.isShiftDown();
        target = findTarget(point, objectCategory);
        targetIsNew = false;

        if(target == null) {
            if(erasing) {
                return;
            }

            target = new BoundingMaskView(objectCategory,
                                          MaskBitmap.empty((int) Math.round(imageFileSize.getWidth()),
                                                           (int) Math.round(imageFileSize.getHeight())));
            target.setToggleGroup(toggleGroup);
            boundingShapes.add(target);
            target.autoScaleWithBounds(imageView.boundsInParentProperty());
            targetIsNew = true;
        }

        toggleGroup.selectToggle(target);
        target.startPainting();
        target.paintLine(point.getX(), point.getY(), point.getX(), point.getY(), brushRadius(), !erasing);
        lastPoint = point;
        drawingInProgress = true;
    }

    @Override
    public void updateShape(MouseEvent event) {
        if(drawingInProgress && event.getEventType().equals(MouseEvent.MOUSE_DRAGGED)
                && event.getButton().equals(MouseButton.PRIMARY)) {
            final Point2D point = toParent(event);
            target.paintLine(lastPoint.getX(), lastPoint.getY(), point.getX(), point.getY(), brushRadius(), !erasing);
            lastPoint = point;
        }
    }

    @Override
    public void finalizeShape() {
        if(!drawingInProgress) {
            return;
        }

        target.finishPainting();
        drawingInProgress = false;

        // A mask that was erased completely (or a new one painted outside the image) is removed, unless other shapes
        // are nested in it.
        final BoundingShapeTreeItem treeItem = target.getViewData().getTreeItem();

        if(target.getMask().isEmpty() && (treeItem == null || treeItem.getChildren().isEmpty())) {
            removeTarget();
        }
    }

    @Override
    public Optional<BoundingShapeViewable> undoLastStep() {
        if(!drawingInProgress) {
            return Optional.empty();
        }

        target.cancelPainting();
        drawingInProgress = false;
        // A new mask is removed by the caller; an existing one keeps its mask from before the stroke.
        return targetIsNew ? Optional.of(target) : Optional.empty();
    }

    @Override
    public boolean isDrawingInProgress() {
        return drawingInProgress;
    }

    @Override
    public EditorImagePaneView.DrawingMode getDrawingMode() {
        return EditorImagePaneView.DrawingMode.MASK;
    }

    private BoundingMaskView findTarget(Point2D point, ObjectCategory category) {
        if(!erasing && settings.consumeNewMaskRequest()) {
            return null;
        }

        if(toggleGroup.getSelectedToggle() instanceof BoundingMaskView selected && selected.isVisible()
                && (erasing || Objects.equals(selected.getViewData().getObjectCategory(), category))) {
            return selected;
        }

        return boundingShapes.stream()
                             .filter(BoundingMaskView.class::isInstance)
                             .map(BoundingMaskView.class::cast)
                             .filter(mask -> mask.isVisible()
                                     && (erasing || Objects.equals(mask.getViewData().getObjectCategory(), category))
                                     && mask.containsPointInParent(point.getX(), point.getY()))
                             .min(Comparator.comparingInt(mask -> mask.getMask().getArea()))
                             .orElse(null);
    }

    private void removeTarget() {
        boundingShapes.remove(target);

        final BoundingShapeTreeItem treeItem = target.getViewData().getTreeItem();

        if(treeItem != null && treeItem.getParent() instanceof ObjectCategoryTreeItem parentTreeItem) {
            parentTreeItem.detachBoundingShapeTreeItemChild(treeItem);

            if(parentTreeItem.getChildren().isEmpty()) {
                parentTreeItem.getParent().getChildren().remove(parentTreeItem);
            }
        }
    }

    private Point2D toParent(MouseEvent event) {
        final Point2D clamped = MathUtils.clampWithinBounds(event.getX(), event.getY(), imageView.getBoundsInLocal());
        return imageView.localToParent(clamped.getX(), clamped.getY());
    }

    private double brushRadius() {
        return settings.getBrushSize() / 2;
    }

    /**
     * The brush settings, shared by the drawers of all strokes.
     */
    public interface Settings {
        /**
         * Returns the diameter of the brush on the screen.
         *
         * @return the diameter in pixels
         */
        double getBrushSize();

        /**
         * Returns whether the eraser is selected.
         *
         * @return true if strokes erase
         */
        boolean isErasing();

        /**
         * Returns whether a new mask was requested for the next stroke, and resets the request.
         *
         * @return true if the next stroke starts a new mask
         */
        boolean consumeNewMaskRequest();
    }
}
