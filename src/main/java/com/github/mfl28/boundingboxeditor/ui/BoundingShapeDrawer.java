package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.scene.input.MouseEvent;

public interface BoundingShapeDrawer {
    void initializeShape(MouseEvent event, ObjectCategory objectCategory);
    void updateShape(MouseEvent event);
    void finalizeShape();
    boolean isDrawingInProgress();

    EditorImagePaneView.DrawingMode getDrawingMode();
}
