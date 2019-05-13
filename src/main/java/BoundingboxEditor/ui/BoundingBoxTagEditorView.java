package BoundingboxEditor.ui;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.stream.Collectors;

class BoundingBoxTagEditorView extends FlowPane implements View {
    private static final String TAG_INPUT_FIELD_ID = "tag-input-field";
    private final SimpleListProperty<String> tags = new SimpleListProperty<>();
    private final ListChangeListener<String> tagsListener = createTagsListener();
    private final TextField inputField = new TextField();

    BoundingBoxTagEditorView() {
        setHgap(10);
        setVgap(10);

        setUpInternalListeners();

        inputField.setOnAction(event -> {
            String text = inputField.getText();
            if(!text.isEmpty() && !tags.get().contains(text)) {
                tags.get().add(text);
                inputField.clear();
            }
        });

        inputField.setId(TAG_INPUT_FIELD_ID);
        inputField.setPromptText("New tag");
        // wll be enabled if a tag-list is registered with the tagLists-property.
        inputField.setDisable(true);

        getChildren().add(inputField);
        setMinHeight(50);
    }

    void setTags(ObservableList<String> newTags) {
        tags.set(newTags);
    }

    private void setUpInternalListeners() {
        tags.addListener((observable, oldValue, newValue) -> {
            if(oldValue == newValue) {
                // Listeners have to be updated exactly once after a new tag-list was set.
                return;
            }
            if(oldValue != null) {
                oldValue.removeListener(tagsListener);
                getChildren().removeIf(node -> node instanceof TagBox);
            }

            if(newValue != null) {
                getChildren().addAll(0, newValue.stream().map(TagBox::new).collect(Collectors.toList()));
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
                    getChildren().subList(c.getFrom(), c.getFrom() + c.getRemovedSize()).clear();
                }

                if(c.wasAdded()) {
                    getChildren().addAll(c.getFrom(), c.getAddedSubList().stream().map(TagBox::new).collect(Collectors.toList()));
                }
            }
        };
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
            tagNameText.maxWidthProperty().bind(BoundingBoxTagEditorView.this.maxWidthProperty()
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
