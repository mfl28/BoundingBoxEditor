/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;

/**
 * Represents a UI-element containing the {@link EditorImagePaneView} object on which the user can draw/edit
 * bounding shape objects. Furthermore this element contains controls to navigate and edit
 * images.
 *
 * @see BorderPane
 * @see View
 * @see EditorToolBarView
 */
public class EditorView extends BorderPane implements View {
    private static final String BOUNDING_BOX_EDITOR_VIEW_ID = "bounding-box-editor-view";

    private final EditorToolBarView editorToolBarView = new EditorToolBarView();
    private final EditorImagePaneView editorImagePaneView = new EditorImagePaneView();

    /**
     * Creates a new editor view UI-element containing the {@link EditorImagePaneView} object on which the user can
     * draw/edit
     * bounding shape objects. Furthermore this element contains controls to navigate and edit
     * images.
     */
    EditorView() {
        setTop(editorToolBarView);
        setCenter(editorImagePaneView);
        setId(BOUNDING_BOX_EDITOR_VIEW_ID);

        editorImagePaneView.setMinSize(25, 25);
        setUpInternalListeners();
    }

    /**
     * Returns the editor toolbar containing controls for adjusting image and bounding-shape settings.
     *
     * @return the toolbar
     */
    public EditorToolBarView getEditorToolBar() {
        return editorToolBarView;
    }

    @Override
    public void connectToController(Controller controller) {
        editorToolBarView.connectToController(controller);
        editorImagePaneView.connectToController(controller);
    }

    /**
     * Returns the editor image-pane.
     *
     * @return the image-pane
     */
    EditorImagePaneView getEditorImagePane() {
        return editorImagePaneView;
    }

    private void setUpInternalListeners() {
        ColorAdjust colorAdjust = editorImagePaneView.getColorAdjust();
        colorAdjust.brightnessProperty().bind(editorToolBarView.getBrightnessSlider().valueProperty());
        colorAdjust.contrastProperty().bind(editorToolBarView.getContrastSlider().valueProperty());
        colorAdjust.saturationProperty().bind(editorToolBarView.getSaturationSlider().valueProperty());

        editorToolBarView.getResetSizeAndCenterImageButton()
                         .setOnAction(event -> editorImagePaneView.resetImageViewSize());

        editorToolBarView.getRectangleModeButton().selectedProperty().addListener((observable, oldValue, newValue) -> {
            editorImagePaneView.setBoundingPolygonsEditingAndConstructing(false);

            if(Boolean.TRUE.equals(newValue)) {
                editorImagePaneView.setDrawingMode(EditorImagePaneView.DrawingMode.BOX);
            } else {
                editorImagePaneView.setDrawingMode(EditorImagePaneView.DrawingMode.POLYGON);
            }
        });

        setOnMousePressed(event -> requestFocus());
    }
}
