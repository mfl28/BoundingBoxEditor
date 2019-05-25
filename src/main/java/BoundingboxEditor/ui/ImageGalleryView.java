package BoundingboxEditor.ui;

import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
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

    private class ImageGalleryCell extends ListCell<File> {
        final ImageView imageView = new ImageView();

        ImageGalleryCell() {
            setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
            prefWidthProperty().bind(ImageGalleryView.this.widthProperty().subtract(20));
            setMaxWidth(Control.USE_PREF_SIZE);
        }

        @Override
        protected void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);

            if(empty || item == null) {
                imageView.setImage(null);
                setGraphic(null);
                setText(null);
            } else {
                Image currentImage = imageView.getImage();
                // Cancel image-loading in case of an already existing, not-selected image,
                // that is not the same as the updated image.
                if(currentImage != null && !currentImage.getUrl().equals(item.toURI().toString()) && !isSelected()) {
                    currentImage.cancel();
                }
                // If this cell's ImageView does not contain an image or contains an image different to the
                // image corresponding to the update's file, then update the image (i.e. set the image and start background-loading).
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
