/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.ui;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.mfl28.boundingboxeditor.utils.ImageUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

import java.util.Objects;

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
                                              .map(FileInfo::getFileUrl)
                                              .toList());
                }
            }
        });
    }

    public static class FileInfo {
        private final String fileUrl;
        private final String fileName;
        private final BooleanProperty hasAssignedBoundingBoxes = new SimpleBooleanProperty(false);
        private final int orientation;

        public FileInfo(String fileUrl, String fileName, int orientation) {
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.orientation = orientation;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public String getFileName() {
            return fileName;
        }

        public int getOrientation() {
            return orientation;
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
            return Objects.hash(fileUrl, fileName, hasAssignedBoundingBoxes);
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }

            if(!(o instanceof FileInfo fileInfo)) {
                return false;
            }

            return Objects.equals(fileUrl, fileInfo.fileUrl) && Objects.equals(fileName, fileInfo.fileName) &&
                    Objects.equals(hasAssignedBoundingBoxes, fileInfo.hasAssignedBoundingBoxes);
        }
    }

    private class ImageFileInfoCell extends ListCell<FileInfo> {
        private static final String IMAGE_FILE_INFO_CELL_ID = "image-file-info-cell";
        private static final String HAS_ASSIGNED_BOUNDING_BOXES_CLASS_NAME = "has-assigned-bounding-boxes";
        private final PseudoClass hasAssignedBoundingBoxesClass =
                PseudoClass.getPseudoClass(HAS_ASSIGNED_BOUNDING_BOXES_CLASS_NAME);
        private final ImageView imageView = new ImageView();
        private String currentImageUrl = null;

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
                String fileURI = item.getFileUrl();
                // Invalidate cache-object and cancel image-loading in case of a currently loading, not-selected image,
                // that is not the same as the updated image.
                if(currentImage != null && !currentImageUrl.equals(fileURI)
                        && !isSelected() && currentImage.getProgress() != 1.0) {
                    imageCache.invalidate(currentImageUrl);
                    currentImage.cancel();
                }
                // If this cell's ImageView does not contain an image or contains an image different to the
                // image corresponding to this update's file, then update the image (i.e. set the image and start background-loading).
                if(currentImage == null || !currentImageUrl.equals(fileURI)) {
                    setGraphic(imageView);
                    updateCellImage(item, imageCache.get(fileURI));
                }
                setText(item.getFileName());
                currentImageUrl = item.getFileUrl();
                hasAssignedBoundingBoxes.bind(item.hasAssignedBoundingBoxesProperty());
            }
        }

        private void updateCellImage(FileInfo item, Image newImage) {
            if(item.getOrientation() != 1) {
                if(newImage.getProgress() == 1) {
                    imageView.setImage(ImageUtils.reorientImage(newImage, item.getOrientation()));
                } else {
                    ChangeListener<Number> progressListener = new ChangeListener<>() {
                        @Override
                        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                            if(newValue.intValue() == 1) {
                                imageView.setImage(ImageUtils.reorientImage(newImage, item.getOrientation()));
                                newImage.progressProperty().removeListener(this);
                            }
                        }
                    };

                    newImage.progressProperty().addListener(progressListener);
                }
            } else {
                imageView.setImage(newImage);
            }
        }
    }
}
