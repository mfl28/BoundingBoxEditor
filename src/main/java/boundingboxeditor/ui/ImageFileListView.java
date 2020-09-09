package boundingboxeditor.ui;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.ContentDisplay;
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
public class ImageFileListView extends ListView<ImageFileListView.FileInfo> implements View {
    static final double REQUESTED_IMAGE_WIDTH = 205;
    static final double REQUESTED_IMAGE_HEIGHT = 205;
    private static final int IMAGE_CACHE_SIZE = 500;

    private final LoadingCache<String, Image> imageCache = Caffeine.newBuilder()
                                                                   .maximumSize(IMAGE_CACHE_SIZE)
                                                                   .build(key -> new Image(key, REQUESTED_IMAGE_WIDTH,
                                                                                           REQUESTED_IMAGE_HEIGHT, true,
                                                                                           false, true));

    /**
     * Creates a new image-file list UI-element.
     */
    ImageFileListView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        setCellFactory(listView -> new ImageFileInfoCell());
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
                                              .map(fileInfo -> fileInfo.getFile().toURI().toString())
                                              .collect(Collectors.toList()));
                }
            }
        });
    }

    public static class FileInfo {
        private final File file;
        private final BooleanProperty hasAssignedBoundingBoxes = new SimpleBooleanProperty(false);

        public FileInfo(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        public boolean isHasAssignedBoundingShapes() {
            return hasAssignedBoundingBoxes.get();
        }

        public void setHasAssignedBoundingShapes(boolean hasAssignedBoundingBoxes) {
            this.hasAssignedBoundingBoxes.set(hasAssignedBoundingBoxes);
        }

        public BooleanProperty hasAssignedBoundingBoxesProperty() {
            return hasAssignedBoundingBoxes;
        }

        @Override
        public int hashCode() {
            return Objects.hash(file, hasAssignedBoundingBoxes);
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }

            if(!(o instanceof FileInfo)) {
                return false;
            }

            FileInfo fileInfo = (FileInfo) o;

            return Objects.equals(file, fileInfo.file) &&
                    Objects.equals(hasAssignedBoundingBoxes, fileInfo.hasAssignedBoundingBoxes);
        }
    }

    private class ImageFileInfoCell extends ListCell<FileInfo> {
        private static final String IMAGE_FILE_INFO_CELL_ID = "image-file-info-cell";
        private static final String HAS_ASSIGNED_BOUNDING_BOXES_CLASS_NAME = "has-assigned-bounding-boxes";
        private final PseudoClass hasAssignedBoundingBoxesClass =
                PseudoClass.getPseudoClass(HAS_ASSIGNED_BOUNDING_BOXES_CLASS_NAME);
        private final ImageView imageView = new ImageView();

        private final BooleanProperty hasAssignedBoundingBoxes = new BooleanPropertyBase(true) {
            @Override
            public Object getBean() {
                return ImageFileInfoCell.this;
            }

            @Override
            public String getName() {
                return HAS_ASSIGNED_BOUNDING_BOXES_CLASS_NAME;
            }

            @Override
            protected void invalidated() {
                pseudoClassStateChanged(hasAssignedBoundingBoxesClass, get());
            }
        };

        ImageFileInfoCell() {
            setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
            prefWidthProperty().bind(ImageFileListView.this.widthProperty().subtract(30));
            setMaxWidth(Region.USE_PREF_SIZE);
            imageView.setCache(true);
            imageView.setCacheHint(CacheHint.SPEED);

            setContentDisplay(ContentDisplay.TOP);
            setAlignment(Pos.CENTER);

            pseudoClassStateChanged(hasAssignedBoundingBoxesClass, false);
            setId(IMAGE_FILE_INFO_CELL_ID);
        }

        @Override
        protected void updateItem(FileInfo item, boolean empty) {
            super.updateItem(item, empty);

            if(empty || item == null) {
                imageView.setImage(null);
                setGraphic(null);
                setText(null);
                hasAssignedBoundingBoxes.unbind();
            } else {
                Image currentImage = imageView.getImage();
                String fileURI = item.getFile().toURI().toString();
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
                setText(item.getFile().getName());

                hasAssignedBoundingBoxes.bind(item.hasAssignedBoundingBoxesProperty());
            }
        }
    }
}
