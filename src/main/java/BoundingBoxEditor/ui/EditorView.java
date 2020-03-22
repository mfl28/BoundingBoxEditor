package boundingboxeditor.ui;

import boundingboxeditor.controller.Controller;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;

/**
 * Represents a UI-element containing the {@link EditorImagePaneView} object on which the user can draw/edit
 * {@link BoundingBoxView} objects. Furthermore this element contains controls to navigate and edit
 * images.
 *
 * @see BorderPane
 * @see View
 * @see EditorToolBarView
 */
public class EditorView extends BorderPane implements View {
    private static final String BOUNDING_BOX_EDITOR_VIEW_ID = "bounding-box-editor-view";

    private final EditorToolBarView boundingBoxEditorToolBar = new EditorToolBarView();
    private final EditorImagePaneView boundingBoxEditorImagePane = new EditorImagePaneView();

    /**
     * Creates a new bounding-box-editor view UI-element containing the {@link EditorImagePaneView} object on which the user can draw/edit
     * {@link BoundingBoxView} objects. Furthermore this element contains controls to navigate and edit
     * images.
     */
    EditorView() {
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
    public EditorToolBarView getEditorToolBar() {
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
    EditorImagePaneView getEditorImagePane() {
        return boundingBoxEditorImagePane;
    }

    private void setUpInternalListeners() {
        ColorAdjust colorAdjust = boundingBoxEditorImagePane.getColorAdjust();
        colorAdjust.brightnessProperty().bind(boundingBoxEditorToolBar.getBrightnessSlider().valueProperty());
        colorAdjust.contrastProperty().bind(boundingBoxEditorToolBar.getContrastSlider().valueProperty());
        colorAdjust.saturationProperty().bind(boundingBoxEditorToolBar.getSaturationSlider().valueProperty());

        boundingBoxEditorToolBar.getResetSizeAndCenterImageButton().setOnAction(event -> boundingBoxEditorImagePane.resetImageViewSize());

        boundingBoxEditorToolBar.getRectangleModeButton().selectedProperty().addListener((observable, oldValue, newValue) -> {
            boundingBoxEditorImagePane.finalizeBoundingPolygon();
            if(Boolean.TRUE.equals(newValue)) {
                boundingBoxEditorImagePane.setDrawingMode(EditorImagePaneView.DrawingMode.BOX);
            } else {
                boundingBoxEditorImagePane.setDrawingMode(EditorImagePaneView.DrawingMode.POLYGON);
            }
        });

        setOnMousePressed(event -> requestFocus());
    }
}
