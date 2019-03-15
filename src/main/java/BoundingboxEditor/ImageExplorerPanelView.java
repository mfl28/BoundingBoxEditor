package BoundingboxEditor;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.File;

public class ImageExplorerPanelView extends VBox implements View {
    private static final double SIDE_PANEL_SPACING = 5;
    private final ImageGalleryView imageGallery = new ImageGalleryView();

    ImageExplorerPanelView() {
        this.getChildren().addAll(new Label("Image Explorer"), imageGallery);
        this.getStyleClass().add("image-explorer-side-panel");
        this.setSpacing(SIDE_PANEL_SPACING);
        this.setVisible(false);
        this.setManaged(false);
    }

    public void setImageGalleryItems(ObservableList<File> imageFiles) {
        imageGallery.setItems(imageFiles);
    }

    public ImageGalleryView getImageGallery() {
        return imageGallery;
    }
}
