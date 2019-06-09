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
public class ImageBoundingBoxEditorView extends BorderPane implements View {
    private static final String BOUNDING_BOX_EDITOR_VIEW_ID = "bounding-box-editor-view";

    private final BoundingBoxEditorToolBarView imageToolBar = new BoundingBoxEditorToolBarView();
    private final BoundingBoxEditorImagePaneView imagePane = new BoundingBoxEditorImagePaneView();

    /**
     * Creates a new bounding-box-editor view UI-element containing the {@link BoundingBoxEditorImagePaneView} object on which the user can draw/edit
     * {@link BoundingBoxView} objects. Furthermore this element contains controls to navigate and edit
     * images.
     */
    ImageBoundingBoxEditorView() {
        setTop(imageToolBar);
        setCenter(imagePane);
        setId(BOUNDING_BOX_EDITOR_VIEW_ID);

        imagePane.setMinSize(25, 25);
        setUpInternalListeners();
    }

    public BoundingBoxEditorToolBarView getImageToolBar() {
        return imageToolBar;
    }

    @Override
    public void connectToController(Controller controller) {
        imageToolBar.connectToController(controller);
        imagePane.connectToController(controller);
    }

    BoundingBoxEditorImagePaneView getImagePane() {
        return imagePane;
    }

    private void setUpInternalListeners() {
        ColorAdjust colorAdjust = imagePane.getColorAdjust();
        colorAdjust.brightnessProperty().bind(imageToolBar.getBrightnessSlider().valueProperty());
        colorAdjust.contrastProperty().bind(imageToolBar.getContrastSlider().valueProperty());
        colorAdjust.saturationProperty().bind(imageToolBar.getSaturationSlider().valueProperty());

        setOnMousePressed(event -> requestFocus());
    }
}
