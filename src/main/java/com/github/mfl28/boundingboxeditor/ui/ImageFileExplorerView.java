/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;

/**
 * UI-element that contains the controls to view, select and search image-files.
 *
 * @see VBox
 * @see View
 */
public class ImageFileExplorerView extends VBox implements View {
    private static final String IMAGE_FILE_EXPLORER_ID = "image-file-explorer";
    private static final String IMAGE_FILE_EXPLORER_TABLE_TEXT = "Images";
    private static final String IMAGE_FILE_SEARCH_PROMPT_TEXT = "Search File";
    private static final String IMAGE_FILE_SEARCH_BOX_ID = "image-file-search-box";
    private static final String IMAGE_FILE_SEARCH_ICON_LABEL_ID = "search-icon-label";
    private static final String IMAGE_FILE_SEARCH_ICON_ID = "search-icon";

    private final TextField imageFileSearchField = new TextField();
    private final ImageFileListView imageFileListView = new ImageFileListView();

    /**
     * Creates a new image-file-explorer UI-element.
     */
    ImageFileExplorerView() {
        getChildren().addAll(
                new Label(IMAGE_FILE_EXPLORER_TABLE_TEXT),
                createImageFileSearchBox(),
                imageFileListView);

        setId(IMAGE_FILE_EXPLORER_ID);

        setUpInternalListeners();
    }

    /**
     * Sets the image-files to display in the {@link ImageFileListView} member.
     *
     * @param imageFiles the list of image-files
     */
    public void setImageFiles(List<File> imageFiles) {
        ObservableList<ImageFileListView.FileInfo> imageInfoItems = FXCollections.unmodifiableObservableList(
                FXCollections.observableList(imageFiles.stream()
                                                       .map(ImageFileListView.FileInfo::new)
                                                       .toList())
        );

        imageFileListView.setItems(imageInfoItems);
    }

    /**
     * Returns the {@link ImageFileListView} member.
     *
     * @return the {@link ImageFileListView}
     */
    public ImageFileListView getImageFileListView() {
        return imageFileListView;
    }

    /**
     * Returns the image-file search {@link TextField} member.
     *
     * @return the image-file search-field
     */
    TextField getImageFileSearchField() {
        return imageFileSearchField;
    }

    private HBox createImageFileSearchBox() {
        HBox.setHgrow(imageFileSearchField, Priority.ALWAYS);

        imageFileSearchField.setPromptText(IMAGE_FILE_SEARCH_PROMPT_TEXT);
        imageFileSearchField.setFocusTraversable(false);
        imageFileSearchField.setTooltip(UiUtils.createFocusTooltip(Controller.KeyCombinations.focusFileSearchField));

        Region searchIcon = new Region();
        searchIcon.setId(IMAGE_FILE_SEARCH_ICON_ID);

        Label searchLabel = new Label();
        searchLabel.setGraphic(searchIcon);
        searchLabel.setId(IMAGE_FILE_SEARCH_ICON_LABEL_ID);

        HBox imageFileSearchBox = new HBox(searchLabel, imageFileSearchField);
        imageFileSearchBox.setId(IMAGE_FILE_SEARCH_BOX_ID);

        return imageFileSearchBox;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        imageFileSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                imageFileListView.getItems().stream()
                                 .filter(item -> item.getFile().getName().startsWith(newValue))
                                 .findAny()
                                 .ifPresent(item -> {
                                     // We have to temporarily set a fixed cell size, otherwise
                                     // the scroll-to point will not be calculated correctly.
                                     imageFileListView.setFixedCellSize(ImageFileListView.REQUESTED_IMAGE_HEIGHT);
                                     imageFileListView.getSelectionModel().select(item);
                                     imageFileListView.scrollTo(item);
                                     // Disable fixed cell-size.
                                     imageFileListView.setFixedCellSize(0);
                                 });
            }
        });

        imageFileSearchField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!Boolean.TRUE.equals(newValue)) {
                imageFileSearchField.setText(null);
            }
        });
    }
}
