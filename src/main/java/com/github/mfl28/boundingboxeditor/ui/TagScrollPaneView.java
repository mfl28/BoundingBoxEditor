/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

/**
 * A UI-element used to display and edit tags associated with a bounding-shape.
 *
 * @see ScrollPane
 * @see View
 */
class TagScrollPaneView extends ScrollPane implements View {
    private static final String TEXT_FIELD_PROMPT_TEXT = "New Tag";
    private static final String TAG_INPUT_FIELD_ID = "tag-input-field";
    private static final String TAG_EDITOR_SCROLL_PANE_ID = "tag-editor-scroll-pane";
    private static final int FLOW_PANE_GAP_SIZE = 8;

    private final SimpleListProperty<String> tags = new SimpleListProperty<>();
    private final TextField tagInputField = createTagInputField();
    private final FlowPane tagFlowPane = createTagFlowPane();
    private final ListChangeListener<String> tagsListener = createTagsListener();

    /**
     * Creates a new tag-scroll-pane UI-element.
     */
    TagScrollPaneView() {
        setContent(tagFlowPane);
        setId(TAG_EDITOR_SCROLL_PANE_ID);
        setFitToWidth(true);
        fixBlurryText();
        setUpInternalListeners();
    }

    /**
     * Returns the tag-input text-field used to add new tags.
     *
     * @return the tag-input text-field
     */
    TextField getTagInputField() {
        return tagInputField;
    }

    /**
     * Registers an {@link ObservableList} of tags as the list of currently displayed tags.
     *
     * @param newTags the tags to display
     */
    void setTags(ObservableList<String> newTags) {
        tags.set(newTags);
    }

    private void fixBlurryText() {
        // Workaround to fix blurry text in scroll-pane
        // (adapted from: https://gist.github.com/siavvasm/1f15d58351c1e99e0cf5337613b7c3ae)
        Platform.runLater(() -> {
            setCache(false);
            getChildrenUnmodifiable().forEach(childNode -> childNode.setCache(false));
        });
    }

    private void setUpInternalListeners() {
        // Auto-scroll to the end if the height of the tag-flow-pane changes. This ensures
        // that the tag-input text-field is always visible.
        tagFlowPane.heightProperty()
                   .addListener((observable, oldValue, newValue) -> vvalueProperty().set(newValue.doubleValue()));

        tagFlowPane.maxWidthProperty().bind(widthProperty());

        tagInputField.setOnAction(event -> {
            final String text = tagInputField.getText();
            // Empty or already existing tags are not added to the list of tags.
            if(text != null && !text.isEmpty() && !tags.get().contains(text)) {
                tags.get().add(text);
            }
            requestFocus();
        });

        tagInputField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!Boolean.TRUE.equals(newValue)) {
                tagInputField.setText(null);
            }
        });

        tagInputField.setOnKeyReleased(event -> {
            if(event.getCode() == KeyCode.ESCAPE) {
                requestFocus();
                event.consume();
            }
        });

        tags.addListener((observable, oldValue, newValue) -> {
            if(oldValue == newValue) {
                // Listeners have to be updated exactly once after a new tag-list was set. If
                // the tag-list reference has not changed, nothing needs to be done.
                return;
            }

            if(oldValue != null) {
                oldValue.removeListener(tagsListener);
                tagFlowPane.getChildren().removeIf(TagBox.class::isInstance);
            }

            if(newValue != null) {
                tagFlowPane.getChildren().addAll(0, newValue.stream().map(TagBox::new).toList());
                newValue.addListener(tagsListener);
                tagInputField.setDisable(false);
            } else {
                // If no list of tags is registered, the tag-input field is disabled. This is the case
                // when no bounding-shape is currently selected.
                tagInputField.setDisable(true);
            }
        });
    }

    private TextField createTagInputField() {
        TextField textField = new TextField();
        textField.setId(TAG_INPUT_FIELD_ID);
        textField.setPromptText(TEXT_FIELD_PROMPT_TEXT);
        textField.setTooltip(UiUtils.createFocusTooltip(Controller.KeyCombinations.focusTagTextField));
        // Will be enabled if a tag-list is registered with the tagLists-property.
        textField.setDisable(true);

        return textField;
    }

    private FlowPane createTagFlowPane() {
        FlowPane flowPane = new FlowPane(tagInputField);
        flowPane.setHgap(FLOW_PANE_GAP_SIZE);
        flowPane.setVgap(FLOW_PANE_GAP_SIZE);

        return flowPane;
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ListChangeListener<String> createTagsListener() {
        return change -> {
            while(change.next()) {
                if(change.wasRemoved()) {
                    tagFlowPane.getChildren().subList(change.getFrom(),
                                                      change.getFrom() + change.getRemovedSize()).clear();
                }

                if(change.wasAdded()) {
                    tagFlowPane.getChildren().addAll(change.getFrom(),
                            change.getAddedSubList().stream()
                                    .map(TagBox::new).toList());
                }
            }
        };
    }

    private class TagBox extends HBox {
        private static final String DELETE_BUTTON_ID = "delete-button";
        private static final String DELETE_ICON_ID = "delete-icon";
        private static final String TAG_ID = "tag";
        private static final String TAG_LABEL_ID = "tag-label";

        private final Label tagNameText = new Label();
        private final Button deleteButton = new IconButton(DELETE_ICON_ID, IconButton.IconType.GRAPHIC);

        TagBox(String tagName) {
            tagNameText.setText(tagName);
            getChildren().addAll(tagNameText, deleteButton);
            HBox.setMargin(tagNameText, new Insets(0, 5, 0, 0));
            setId(TAG_ID);
            tagNameText.setTextOverrun(OverrunStyle.ELLIPSIS);
            deleteButton.setId(DELETE_BUTTON_ID);
            tagNameText.setId(TAG_LABEL_ID);

            setUpInternalListeners();
        }

        private void setUpInternalListeners() {
            tagNameText.maxWidthProperty().bind(tagFlowPane.maxWidthProperty()
                                                           .subtract(deleteButton.widthProperty())
                                                           .subtract(25));

            deleteButton.setOnAction(event -> tags.get().remove(tagNameText.getText()));
        }
    }
}
