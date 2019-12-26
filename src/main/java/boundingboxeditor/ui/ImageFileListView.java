package boundingboxeditor.ui;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import javafx.scene.CacheHint;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A UI-element used for displaying and navigating/selecting the loaded image-files.
 *
 * @see ListView
 * @see View
 */
public class ImageFileListView extends ListView<File> implements View {
    private static final double REQUESTED_IMAGE_WIDTH = 150;
    private static final double REQUESTED_IMAGE_HEIGHT = 150;
    private static final int IMAGE_CACHE_SIZE = 500;

    private final LoadingCache<String, Image> imageCache = Caffeine.newBuilder()
            .maximumSize(IMAGE_CACHE_SIZE)
            .build(key -> new Image(key, REQUESTED_IMAGE_WIDTH, REQUESTED_IMAGE_HEIGHT, true, false, true));

    /**
     * Creates a new image-file list UI-element.
     */
    ImageFileListView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        setCellFactory(listView -> new ImageCell());
        setFocusTraversable(false);

        setUpInternalListeners();
    }

    @Override
    public void scrollTo(int index) {
        setFixedCellSize(REQUESTED_IMAGE_HEIGHT + 10);
        super.scrollTo(index);
        setFixedCellSize(0);
    }

    private void setUpInternalListeners() {
        itemsProperty().addListener((observable, oldValue, newValue) -> {
            if(!Objects.equals(newValue, oldValue)) {
                imageCache.invalidateAll();

                if(newValue != null && newValue.size() <= IMAGE_CACHE_SIZE) {
                    // Triggers loading of the new images into the cache.
                    imageCache.getAll(newValue.stream()
                            .map(file -> file.toURI().toString())
                            .collect(Collectors.toList()));
                }
            }
        });
    }

    private class ImageCell extends ListCell<File> {
        final ImageView imageView = new ImageView();

        ImageCell() {
            setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
            prefWidthProperty().bind(ImageFileListView.this.widthProperty().subtract(30));
            setMaxWidth(Region.USE_PREF_SIZE);
            imageView.setCache(true);
            imageView.setCacheHint(CacheHint.SPEED);
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
                String fileURI = item.toURI().toString();
                // Invalidate cache-object and cancel image-loading in case of a currently loading, not-selected image,
                // that is not the same as the updated image.
                if(currentImage != null && !currentImage.getUrl().equals(fileURI)
                        && !isSelected() && currentImage.getProgress() != 1.0) {
                    imageCache.invalidate(currentImage.getUrl());
                    currentImage.cancel();
                }
                // If this cell's ImageView does not contain an image or contains an image different to the
                // image corresponding to this update's file, then update the image (i.e. set the image and start background-loading).
                if(currentImage == null || !currentImage.getUrl().equals(fileURI)) {
                    setGraphic(imageView);
                    imageView.setImage(imageCache.get(fileURI));
                }
                setText(item.getName());
            }
        }
    }
}
