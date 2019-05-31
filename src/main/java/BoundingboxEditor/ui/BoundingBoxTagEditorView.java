package BoundingboxEditor.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.stream.Collectors;

class BoundingBoxTagEditorView extends ScrollPane implements View {
    private static final String TAG_INPUT_FIELD_ID = "tag-input-field";
    private static final String TEXT_FIELD_PROMPT_TEXT = "New tag";
    private static final String TAG_EDITOR_SCROLL_PANE_ID = "tag-editor-scroll-pane";
    private final SimpleListProperty<String> tags = new SimpleListProperty<>();
    private final TextField inputField = createTagInputField();
    private final FlowPane tagFlowPane = createTagFlowPane();
    private final ListChangeListener<String> tagsListener = createTagsListener();

    BoundingBoxTagEditorView() {
        setContent(tagFlowPane);
        setId(TAG_EDITOR_SCROLL_PANE_ID);
        setFitToWidth(true);
        fixBlurryText();
        setUpInternalListeners();
    }

    void setTags(ObservableList<String> newTags) {
        tags.set(newTags);
    }

    private TextField createTagInputField() {
        TextField textField = new TextField();
        textField.setId(TAG_INPUT_FIELD_ID);
        textField.setPromptText(TEXT_FIELD_PROMPT_TEXT);
        // wll be enabled if a tag-list is registered with the tagLists-property.
        textField.setDisable(true);

        return textField;
    }

    private FlowPane createTagFlowPane() {
        FlowPane flowPane = new FlowPane(inputField);
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        flowPane.setMinHeight(50);

        return flowPane;
    }

    private void setUpInternalListeners() {
        // Auto scroll to end if the height of the tag-flow-pane changes.
        tagFlowPane.heightProperty().addListener((observable, oldValue, newValue) -> vvalueProperty().set(newValue.doubleValue()));

        tagFlowPane.maxWidthProperty().bind(widthProperty());

        inputField.setOnAction(event -> {
            String text = inputField.getText();
            if(!text.isEmpty() && !tags.get().contains(text)) {
                tags.get().add(text);
                inputField.clear();
            }
        });

        tags.addListener((observable, oldValue, newValue) -> {
            if(oldValue == newValue) {
                // Listeners have to be updated exactly once after a new tag-list was set.
                return;
            }
            if(oldValue != null) {
                oldValue.removeListener(tagsListener);
                tagFlowPane.getChildren().removeIf(node -> node instanceof TagBox);
            }

            if(newValue != null) {
                tagFlowPane.getChildren().addAll(0, newValue.stream().map(TagBox::new).collect(Collectors.toList()));
                newValue.addListener(tagsListener);
                inputField.setDisable(false);
            } else {
                inputField.setDisable(true);
            }
        });
    }

    private ListChangeListener<String> createTagsListener() {
        return c -> {
            while(c.next()) {
                if(c.wasRemoved()) {
                    tagFlowPane.getChildren().subList(c.getFrom(), c.getFrom() + c.getRemovedSize()).clear();
                }

                if(c.wasAdded()) {
                    tagFlowPane.getChildren().addAll(c.getFrom(), c.getAddedSubList().stream().map(TagBox::new).collect(Collectors.toList()));
                }
            }
        };
    }

    private void fixBlurryText() {
        // Workaround to fix blurry text in scroll-pane
        // (adapted from: https://gist.github.com/siavvasm/1f15d58351c1e99e0cf5337613b7c3ae)
        Platform.runLater(() -> {
            setCache(false);
            getChildrenUnmodifiable().forEach(childNode -> childNode.setCache(false));
        });
    }

    private class TagBox extends HBox {
        private static final String DELETE_BUTTON_ID = "tag-delete-button";
        private static final String TAG_DELETE_BUTTON_STYLE = "delete-button";
        private static final String TAG_DELETE_ICON_STYLE = "icon";
        private static final String TAG_ID = "tag";
        private static final String TAG_LABEL_ID = "tag-label";

        private final Label tagNameText = new Label();
        private final Button deleteButton = createDeleteButton();

        TagBox(String tagName) {
            tagNameText.setText(tagName);
            getChildren().addAll(tagNameText, deleteButton);
            HBox.setMargin(tagNameText, new Insets(0, 5, 0, 0));
            setId(TAG_ID);

            tagNameText.setTextOverrun(OverrunStyle.ELLIPSIS);
            tagNameText.maxWidthProperty().bind(tagFlowPane.maxWidthProperty()
                    .subtract(deleteButton.widthProperty())
                    .subtract(25));

            tagNameText.setId(TAG_LABEL_ID);
        }

        private Button createDeleteButton() {
            final Button deleteButton = new Button();
            deleteButton.getStyleClass().add(TAG_DELETE_BUTTON_STYLE);
            deleteButton.setFocusTraversable(false);
            deleteButton.setPickOnBounds(true);

            final Region deleteIcon = new Region();
            deleteIcon.getStyleClass().add(TAG_DELETE_ICON_STYLE);
            deleteButton.setId(DELETE_BUTTON_ID);
            deleteButton.setGraphic(deleteIcon);

            deleteButton.setOnAction(event -> tags.get().remove(tagNameText.getText()));

            return deleteButton;
        }
    }
}
