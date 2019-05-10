package BoundingboxEditor.ui;

import javafx.collections.FXCollections;
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
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final TextField inputField = new TextField();

    BoundingBoxTagEditorView() {
        setHgap(10);
        setVgap(10);

        setUpInternalListeners();

        tags.addAll("Test1",
                "Test2",
                "Test3",
                "Test4",
                "Test5");

        inputField.setOnAction(event -> {
            String text = inputField.getText();
            if(!text.isEmpty() && !tags.contains(text)) {
                tags.add(text);
                inputField.clear();
            }
        });

        inputField.setId(TAG_INPUT_FIELD_ID);
        inputField.setPromptText("New tag");

        getChildren().add(inputField);
    }

    private void setUpInternalListeners() {
        tags.addListener((ListChangeListener<String>) c -> {
            while(c.next()) {
                if(c.wasRemoved()) {
                    getChildren().subList(c.getFrom(), c.getFrom() + c.getRemovedSize()).clear();
                }

                if(c.wasAdded()) {
                    getChildren().addAll(c.getFrom(), c.getAddedSubList().stream().map(TagBox::new).collect(Collectors.toList()));
                }
            }
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

            deleteButton.setOnAction(event -> tags.remove(tagNameText.getText()));

            return deleteButton;
        }
    }
}
