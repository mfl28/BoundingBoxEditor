package BoundingboxEditor;

import javafx.collections.ObservableList;
import javafx.scene.CacheHint;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.io.File;
import java.util.List;

public class ImageGalleryView extends ListView<File> implements View{

    public ImageGalleryView(){
        VBox.setVgrow(this, Priority.ALWAYS);

        this.setCellFactory(listView -> new ListCell<>(){
            final ImageView imageView = new ImageView();

            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);

                if(empty || item == null){
                    imageView.setImage(null);
                    this.setGraphic(null);
                    this.setText(null);
                }
                else {
                    if(imageView.getImage() != null){
                        imageView.getImage().cancel();
                    }
                    Image image = new Image(item.toURI().toString(), 150, 150, true, false, true);
                    imageView.setImage(image);
                    this.setGraphic(imageView);
                    this.setText(item.getName());
                }
            }
        });
    }

}
