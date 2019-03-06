package BoundingboxEditor;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProjectSidePanelView extends VBox implements View {

    private static final String CLASS_SELECTOR_LABEL_TEXT = "Category Editor";
    private static final String SEARCH_CATEGORY_PROMPT_TEXT = "Search Category";
    private static final String BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT = "Add";
    private static final String SIDE_PANEL_STYLE = "side-panel";
    private static final String BOUNDING_BOX_ITEM_CONTROLS_STYLE = "table-view-input-controls";
    private static final String BOUNDING_BOX_NAME_TEXT_FIELD_STYLE = "bounding-box-name-text-field";
    private static final String BOUNDING_BOX_COLOR_PICKER_STYLE = "bounding-box-color-picker";
    private static final String OBJECT_SELECTOR_LABEL_TEXT = "Explorer";
    private static final int CATEGORY_SEARCH_BOX_SPACING = 10;
    private static final int SIDE_PANEL_SPACING = 5;

    private final BoundingBoxCategorySelectorView selectorView = new BoundingBoxCategorySelectorView();

    private final TextField categoryInputField = new TextField();
    private final TextField categorySearchField = new TextField();
    private final ColorPicker boundingBoxColorPicker = new ColorPicker();
    private final Button addButton = new Button(BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT);
    private final SelectionRectangleExplorerView explorerView = new SelectionRectangleExplorerView();

    ProjectSidePanelView() {
        this.getChildren().addAll(
                new Label(CLASS_SELECTOR_LABEL_TEXT),
                createCategorySearchBox(),
                selectorView,
                createAddCategoryControllBox(),
                new Separator(),
                new Label(OBJECT_SELECTOR_LABEL_TEXT),
                explorerView
        );

        setUpStyles();
        setUpInternalListeners();
    }

    public BoundingBoxCategorySelectorView getSelectorView() {
        return selectorView;
    }

    public TextField getCategoryInputField() {
        return categoryInputField;
    }

    public TextField getCategorySearchField() {
        return categorySearchField;
    }

    public ColorPicker getBoundingBoxColorPicker() {
        return boundingBoxColorPicker;
    }

    public Button getAddButton() {
        return addButton;
    }

    public SelectionRectangleExplorerView getExplorerView() {
        return explorerView;
    }

    @Override
    public void connectToController(Controller controller) {
        addButton.setOnAction(controller::onRegisterAddBoundingBoxItemAction);
        categoryInputField.setOnAction(controller::onRegisterAddBoundingBoxItemAction);
    }

    private HBox createCategorySearchBox() {
        HBox.setHgrow(categorySearchField, Priority.ALWAYS);

        categorySearchField.setPromptText(SEARCH_CATEGORY_PROMPT_TEXT);
        categorySearchField.setFocusTraversable(false);

        //FIXME: fix the textfield context menu style
        final HBox categorySearchBox = new HBox();
        categorySearchBox.getChildren().addAll(categorySearchField);
        categorySearchBox.setSpacing(CATEGORY_SEARCH_BOX_SPACING);

        return categorySearchBox;
    }

    private void setUpStyles() {
        categoryInputField.getStyleClass().add(BOUNDING_BOX_NAME_TEXT_FIELD_STYLE);
        boundingBoxColorPicker.getStyleClass().add(BOUNDING_BOX_COLOR_PICKER_STYLE);

        this.setSpacing(SIDE_PANEL_SPACING);

        this.getStyleClass().add(SIDE_PANEL_STYLE);
    }


    private void setUpInternalListeners() {
        // Source: https://stackoverflow.com/questions/40398905/search-tableview-list-in-javafx
        categorySearchField.textProperty().addListener(((observable, oldValue, newValue) ->
                selectorView.getItems().stream()
                        .filter(item -> item.getName().equals(newValue))
                        .findAny()
                        .ifPresent(item -> {
                            selectorView.getSelectionModel().select(item);
                            selectorView.scrollTo(item);
                        })));

        categorySearchField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue) {
                categorySearchField.setText(null);
            }
        }));
    }

    private HBox createAddCategoryControllBox() {
        addButton.setFocusTraversable(false);
        // FIXME: for testing
        addButton.setId("add-button");

        final HBox addItemControls = new HBox(boundingBoxColorPicker, Utils.createHSpacer(), categoryInputField,
                Utils.createHSpacer(), addButton);
        addItemControls.getStyleClass().add(BOUNDING_BOX_ITEM_CONTROLS_STYLE);

        return addItemControls;
    }
}
