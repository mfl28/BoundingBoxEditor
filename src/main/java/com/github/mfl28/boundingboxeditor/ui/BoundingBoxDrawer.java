package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import javafx.geometry.Point2D;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.List;


public class BoundingBoxDrawer implements BoundingShapeDrawer {
    private final ImageView imageView;
    private final ToggleGroup toggleGroup;
    private final List<BoundingShapeViewable> boundingShapes;

    private BoundingBoxView boundingBoxView;

    private boolean drawingInProgress = false;

    public BoundingBoxDrawer(ImageView imageView, ToggleGroup toggleGroup, List<BoundingShapeViewable> boundingShapes) {
        this.imageView = imageView;
        this.toggleGroup = toggleGroup;
        this.boundingShapes = boundingShapes;
    }

    @Override
    public void initializeShape(MouseEvent event, ObjectCategory objectCategory) {
        if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED) && event.getButton().equals(MouseButton.PRIMARY)) {
            Point2D parentCoordinates = imageView.localToParent(event.getX(), event.getY());

            boundingBoxView = new BoundingBoxView(objectCategory);
            boundingBoxView.getConstructionAnchorLocal().setFromMouseEvent(event);
            boundingBoxView.setToggleGroup(toggleGroup);

            boundingBoxView.setX(parentCoordinates.getX());
            boundingBoxView.setY(parentCoordinates.getY());
            boundingBoxView.setWidth(0);
            boundingBoxView.setHeight(0);

            boundingShapes.add(boundingBoxView);

            boundingBoxView.autoScaleWithBounds(imageView.boundsInParentProperty());
            toggleGroup.selectToggle(boundingBoxView);

            drawingInProgress = true;
        }
    }

    @Override
    public void updateShape(MouseEvent event) {
        if (event.getEventType().equals(MouseEvent.MOUSE_DRAGGED) && event.getButton().equals(MouseButton.PRIMARY)) {
            final Point2D clampedEventXY =
                    MathUtils.clampWithinBounds(event.getX(), event.getY(), imageView.getBoundsInLocal());

            DragAnchor constructionAnchor = boundingBoxView.getConstructionAnchorLocal();
            Point2D parentCoordinates =
                    imageView.localToParent(Math.min(clampedEventXY.getX(), constructionAnchor.getX()),
                            Math.min(clampedEventXY.getY(), constructionAnchor.getY()));

            boundingBoxView.setX(parentCoordinates.getX());
            boundingBoxView.setY(parentCoordinates.getY());
            boundingBoxView.setWidth(Math.abs(clampedEventXY.getX() - constructionAnchor.getX()));
            boundingBoxView.setHeight(Math.abs(clampedEventXY.getY() - constructionAnchor.getY()));
        }
    }

    @Override
    public void finalizeShape() {
        drawingInProgress = false;
    }

    @Override
    public boolean isDrawingInProgress() {
        return drawingInProgress;
    }

    @Override
    public EditorImagePaneView.DrawingMode getDrawingMode() {
        return EditorImagePaneView.DrawingMode.BOX;
    }
}
