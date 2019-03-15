package BoundingboxEditor.views;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;

public class ImageGalleryView extends ListView<File> implements View {

    public ImageGalleryView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        this.setCellFactory(listView -> new ImageGalleryCell());
    }

    private static class ImageGalleryCell extends ListCell<File> {
        final ImageView imageView = new ImageView();

        @Override
        protected void updateItem(File item, boolean empty) {
            super.updateItem(item, empty);

            if(empty || item == null) {
                imageView.setImage(null);
                this.setGraphic(null);
                this.setText(null);
            } else {
                if(imageView.getImage() != null && !this.isSelected()) {
                    imageView.getImage().cancel();
                }

                if(imageView.getImage() == null || (!imageView.getImage().getUrl().equals(item.toURI().toString()))) {
                    Image image = new Image(item.toURI().toString(), 150, 150, true, false, true);
                    imageView.setImage(image);
                    this.setGraphic(imageView);
                }

                this.setText(item.getName());
            }
        }
    }

}
