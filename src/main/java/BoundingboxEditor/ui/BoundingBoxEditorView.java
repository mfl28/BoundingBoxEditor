package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;

/**
 * Represents a UI-element containing the {@link BoundingBoxEditorImagePaneView} object on which the user can draw/edit
 * {@link BoundingBoxView} objects. Furthermore this element contains controls to navigate and edit
 * images.
 *
 * @see BorderPane
 * @see View
 * @see BoundingBoxEditorToolBarView
 */
public class BoundingBoxEditorView extends BorderPane implements View {
    private static final String BOUNDING_BOX_EDITOR_VIEW_ID = "bounding-box-editor-view";

    private final BoundingBoxEditorToolBarView boundingBoxEditorToolBar = new BoundingBoxEditorToolBarView();
    private final BoundingBoxEditorImagePaneView boundingBoxEditorImagePane = new BoundingBoxEditorImagePaneView();

    /**
     * Creates a new bounding-box-editor view UI-element containing the {@link BoundingBoxEditorImagePaneView} object on which the user can draw/edit
     * {@link BoundingBoxView} objects. Furthermore this element contains controls to navigate and edit
     * images.
     */
    BoundingBoxEditorView() {
        setTop(boundingBoxEditorToolBar);
        setCenter(boundingBoxEditorImagePane);
        setId(BOUNDING_BOX_EDITOR_VIEW_ID);

        boundingBoxEditorImagePane.setMinSize(25, 25);
        setUpInternalListeners();
    }

    /**
     * Returns the editor toolbar containing controls for adjusting image and bounding-box settings.
     *
     * @return the toolbar
     */
    public BoundingBoxEditorToolBarView getBoundingBoxEditorToolBar() {
        return boundingBoxEditorToolBar;
    }

    @Override
    public void connectToController(Controller controller) {
        boundingBoxEditorToolBar.connectToController(controller);
        boundingBoxEditorImagePane.connectToController(controller);
    }

    /**
     * Returns the editor image-pane.
     *
     * @return the image-pane
     */
    BoundingBoxEditorImagePaneView getBoundingBoxEditorImagePane() {
        return boundingBoxEditorImagePane;
    }

    private void setUpInternalListeners() {
        ColorAdjust colorAdjust = boundingBoxEditorImagePane.getColorAdjust();
        colorAdjust.brightnessProperty().bind(boundingBoxEditorToolBar.getBrightnessSlider().valueProperty());
        colorAdjust.contrastProperty().bind(boundingBoxEditorToolBar.getContrastSlider().valueProperty());
        colorAdjust.saturationProperty().bind(boundingBoxEditorToolBar.getSaturationSlider().valueProperty());

        setOnMousePressed(event -> requestFocus());
    }
}
