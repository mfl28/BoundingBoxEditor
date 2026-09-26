/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.controller.KeyCombinations;
import com.github.mfl28.boundingboxeditor.model.ImageFileFilter;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;

import java.util.List;
import java.util.stream.IntStream;

/**
 * UI-element that contains the controls to view, select, search and filter image-files.
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
    private static final String IMAGE_FILE_FILTER_BUTTON_ID = "image-file-filter-button";
    private static final String IMAGE_FILE_FILTER_ICON_ID = "image-file-filter-icon";
    private static final String IMAGE_FILE_FILTER_BUTTON_TOOLTIP = "Filter Images";
    private static final String IMAGE_FILE_FILTER_MATCH_LABEL_ID = "image-file-filter-match-label";
    private static final PseudoClass ACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("active");

    private final TextField imageFileSearchField = new TextField();
    private final Button imageFileFilterButton = new IconButton(IMAGE_FILE_FILTER_ICON_ID, IconButton.IconType.GRAPHIC);
    private final Label filterMatchLabel = new Label();
    private final ImageFileFilterView imageFileFilterView = new ImageFileFilterView();
    private final PopOver filterPopOver = new PopOver(imageFileFilterView);
    private final ImageFileListView imageFileListView = new ImageFileListView();
    private boolean updatingControls;

    /**
     * Creates a new image-file-explorer UI-element.
     */
    ImageFileExplorerView() {
        getChildren().addAll(
                new Label(IMAGE_FILE_EXPLORER_TABLE_TEXT),
                createImageFileSearchBox(),
                filterMatchLabel,
                imageFileListView);

        setId(IMAGE_FILE_EXPLORER_ID);
        filterMatchLabel.setId(IMAGE_FILE_FILTER_MATCH_LABEL_ID);
        filterMatchLabel.managedProperty().bind(filterMatchLabel.visibleProperty());
        filterMatchLabel.setVisible(false);

        // Opens to the left of the panel, so that it covers the editor instead of the image list.
        filterPopOver.setArrowLocation(PopOver.ArrowLocation.RIGHT_TOP);
        filterPopOver.setDetachable(false);
        filterPopOver.setAnimated(false);

        setUpInternalListeners();
    }

    /**
     * Creates the list entries of the image files.
     *
     * @param imageMetaData the list of image-meta data elements, ordered by file index
     * @return the entries
     */
    public static List<ImageFileListView.FileInfo> createFileInfos(List<ImageMetaData> imageMetaData) {
        return IntStream.range(0, imageMetaData.size())
                        .mapToObj(index -> {
                            final ImageMetaData metaData = imageMetaData.get(index);
                            return new ImageFileListView.FileInfo(metaData.getFileUrl(), metaData.getFileName(),
                                                                  metaData.getOrientation(), index);
                        })
                        .toList();
    }

    @Override
    public void connectToController(Controller controller) {
        imageFileFilterView.setOnFilterChanged(filter -> onFilterControlsChanged(controller));
        imageFileSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(!updatingControls) {
                controller.onRegisterImageFilterChanged(getFilter());
            }
        });
    }

    /**
     * Resets the search field and the filter controls without reporting a filter change.
     */
    public void resetFilter() {
        updatingControls = true;
        try {
            imageFileSearchField.setText(null);
            imageFileFilterView.reset();
            imageFileFilterButton.pseudoClassStateChanged(ACTIVE_PSEUDO_CLASS, false);
            filterPopOver.hide();
        } finally {
            updatingControls = false;
        }
    }

    /**
     * Returns the filter set by the search field and the filter controls.
     *
     * @return the filter
     */
    public ImageFileFilter getFilter() {
        final ImageFileFilter filter = imageFileFilterView.getFilter();
        return new ImageFileFilter(filter.status(), filter.categoryNames(), filter.categoryMatch(),
                                   imageFileSearchField.getText());
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
     * Returns the controls of the status and category filter.
     *
     * @return the filter view
     */
    public ImageFileFilterView getImageFileFilterView() {
        return imageFileFilterView;
    }

    /**
     * Returns the label that shows how many images match the filter.
     *
     * @return the label
     */
    public Label getFilterMatchLabel() {
        return filterMatchLabel;
    }

    /**
     * Returns the button that opens the filter controls.
     *
     * @return the button
     */
    Button getImageFileFilterButton() {
        return imageFileFilterButton;
    }

    /**
     * Returns the image-file search {@link TextField} member.
     *
     * @return the image-file search-field
     */
    TextField getImageFileSearchField() {
        return imageFileSearchField;
    }

    private void onFilterControlsChanged(Controller controller) {
        imageFileFilterButton.pseudoClassStateChanged(ACTIVE_PSEUDO_CLASS, imageFileFilterView.getFilter().isActive());

        if(!updatingControls) {
            controller.onRegisterImageFilterChanged(getFilter());
        }
    }

    private HBox createImageFileSearchBox() {
        HBox.setHgrow(imageFileSearchField, Priority.ALWAYS);

        imageFileSearchField.setPromptText(IMAGE_FILE_SEARCH_PROMPT_TEXT);
        imageFileSearchField.setFocusTraversable(false);
        imageFileSearchField.setTooltip(UiUtils.createFocusTooltip(KeyCombinations.focusFileSearchField));

        Region searchIcon = new Region();
        searchIcon.setId(IMAGE_FILE_SEARCH_ICON_ID);

        Label searchLabel = new Label();
        searchLabel.setGraphic(searchIcon);
        searchLabel.setId(IMAGE_FILE_SEARCH_ICON_LABEL_ID);

        imageFileFilterButton.setId(IMAGE_FILE_FILTER_BUTTON_ID);
        imageFileFilterButton.setTooltip(UiUtils.createTooltip(IMAGE_FILE_FILTER_BUTTON_TOOLTIP));

        HBox imageFileSearchBox = new HBox(imageFileFilterButton, searchLabel, imageFileSearchField);
        imageFileSearchBox.setId(IMAGE_FILE_SEARCH_BOX_ID);

        return imageFileSearchBox;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        imageFileFilterButton.setOnAction(event -> {
            if(filterPopOver.isShowing()) {
                filterPopOver.hide();
            } else {
                imageFileFilterView.refreshCategoryNames();
                // The arrow points at the panel's left edge, level with the filter button.
                final Bounds panelBounds = localToScreen(getBoundsInLocal());
                final Bounds buttonBounds = imageFileFilterButton.localToScreen(imageFileFilterButton.getBoundsInLocal());
                filterPopOver.show(imageFileFilterButton, panelBounds.getMinX(), buttonBounds.getCenterY());
            }
        });

        imageFileSearchField.setOnAction(event -> requestFocus());

        imageFileSearchField.setOnKeyReleased(event -> {
            if(event.getCode() == KeyCode.ESCAPE) {
                imageFileSearchField.setText(null);
                requestFocus();
                event.consume();
            }
        });
    }
}
