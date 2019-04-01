package BoundingboxEditor.ui;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;

public class ImageGalleryView extends ListView<File> implements View {
    private static final double REQUESTED_IMAGE_WIDTH = 150;
    private static final double REQUESTED_IMAGE_HEIGHT = 150;

    ImageGalleryView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        setCellFactory(listView -> new ImageGalleryCell());
    }

    private static class ImageGalleryCell extends ListCell<File> {
        final ImageView imageView = new ImageView();

        @Override
        protected void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);

            if(empty || item == null) {
                imageView.setImage(null);
                setGraphic(null);
                setText(null);
            } else {
                Image currentImage = imageView.getImage();

                if(currentImage != null && !isSelected()) {
                    currentImage.cancel();
                }

                if(currentImage == null || !currentImage.getUrl().equals(item.toURI().toString())) {
                    imageView.setImage(new Image(item.toURI().toString(), REQUESTED_IMAGE_WIDTH, REQUESTED_IMAGE_HEIGHT,
                            true, false, true));
                    setGraphic(imageView);
                }

                setText(item.getName());
            }
        }
    }

}
