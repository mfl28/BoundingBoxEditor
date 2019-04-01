package BoundingboxEditor.ui;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.File;

public class ImageExplorerPanelView extends VBox implements View {
    private static final double SIDE_PANEL_SPACING = 5;
    private static final String IMAGE_EXPLORER_ID = "image-explorer-panel";
    private static final String IMAGE_EXPLORER_SIDE_PANEL_STYLE = "image-explorer-side-panel";
    private static final String IMAGE_EXPLORER_LABEL_TEXT = "Image Explorer";

    private final ImageGalleryView imageGallery = new ImageGalleryView();

    ImageExplorerPanelView() {
        getChildren().addAll(new Label(IMAGE_EXPLORER_LABEL_TEXT), imageGallery);
        getStyleClass().add(IMAGE_EXPLORER_SIDE_PANEL_STYLE);

        setSpacing(SIDE_PANEL_SPACING);
        setId(IMAGE_EXPLORER_ID);

        setUpInternalListeners();
    }

    public void setImageGalleryItems(ObservableList<File> imageFiles) {
        imageGallery.setItems(imageFiles);
    }

    public ImageGalleryView getImageGallery() {
        return imageGallery;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());
    }
}
