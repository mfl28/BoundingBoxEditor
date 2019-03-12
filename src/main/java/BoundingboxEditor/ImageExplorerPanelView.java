package BoundingboxEditor;

import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;

import java.io.File;

public class ImageExplorerPanelView extends VBox implements View {
    private final ImageGalleryView imageGallery = new ImageGalleryView();

    ImageExplorerPanelView(){
        this.getChildren().add(imageGallery);
        this.getStyleClass().add("image-explorer-side-panel");
    }

    public void setImageGalleryItems(ObservableList<File> imageFiles){
        imageGallery.setItems(imageFiles);
    }

    public ImageGalleryView getImageGallery() {
        return imageGallery;
    }
}
