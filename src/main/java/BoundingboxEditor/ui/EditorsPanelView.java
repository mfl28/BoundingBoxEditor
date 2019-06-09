package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.utils.ColorUtils;
import BoundingboxEditor.utils.UiUtils;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class EditorsPanelView extends VBox implements View {
    private static final String CLASS_SELECTOR_LABEL_TEXT = "Category Editor";
    private static final String SEARCH_CATEGORY_PROMPT_TEXT = "Search Category";
    private static final String BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT = "Add";
    private static final String SIDE_PANEL_STYLE = "side-panel";
    private static final String BOUNDING_BOX_NAME_TEXT_FIELD_STYLE = "bounding-box-name-text-field";
    private static final String BOUNDING_BOX_COLOR_PICKER_STYLE = "bounding-box-color-picker";
    private static final String OBJECT_SELECTOR_LABEL_TEXT = "Object Editor";
    private static final String CATEGORY_INPUT_FIELD_PROMPT_TEXT = "Category Name";
    private static final String PROJECT_SIDE_PANEL_ID = "project-side-panel";
    private static final String CATEGORY_INPUT_FIELD_ID = "category-input-field";
    private static final String ADD_BUTTON_ID = "add-button";
    private static final String TAG_EDITOR_LABEL_TEXT = "Tag Editor";
    private static final String BOUNDING_BOX_EXPLORER_TOP_PANEL_ID = "bounding-box-explorer-top-panel";
    private static final String VISIBILITY_TOGGLE_BUTTON_ID = "visibility-toggle-button";
    private static final String AUTO_SHOW_TOGGLE_BUTTON_ID = "auto-show-toggle-button";
    private static final String EXPANSION_TOGGLE_BUTTON_ID = "expansion-toggle-button";
    private static final String SEARCH_ICON_ID = "search-icon";
    private static final String SEARCH_ICON_LABEL_ID = "search-icon-label";
    private static final String CATEGORY_SELECTOR_TOP_PANEL_ID = "category-selector-top-panel";

    private final TextField categorySearchField = new TextField();
    private final BoundingBoxCategoryTableView categorySelector = new BoundingBoxCategoryTableView();
    private final ColorPicker categoryColorPicker = new ColorPicker();
    private final TextField categoryNameTextField = new TextField();
    private final Button addCategoryButton = new Button(BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT);

    private final BoundingBoxTreeView boundingBoxExplorer = new BoundingBoxTreeView();
    private final ToggleButton visibilityToggle = new ToggleIconButton(VISIBILITY_TOGGLE_BUTTON_ID);
    private final ToggleButton autoShowToggle = new ToggleIconButton(AUTO_SHOW_TOGGLE_BUTTON_ID);
    private final ToggleButton expansionToggle = new ToggleIconButton(EXPANSION_TOGGLE_BUTTON_ID);

    private final BoundingBoxTagScrollPaneView tagEditor = new BoundingBoxTagScrollPaneView();

    EditorsPanelView() {
        getChildren().addAll(
                new Label(CLASS_SELECTOR_LABEL_TEXT),
                createCategorySelectorTopPanel(),
                categorySelector,
                createAddCategoryControlBox(),
                new Separator(),
                createBoundingBoxExplorerTopPanel(),
                boundingBoxExplorer,
                new Separator(),
                new Label(TAG_EDITOR_LABEL_TEXT),
                tagEditor
        );

        setUpStyles();
        setUpIds();
        setUpInternalListeners();
    }

    public TextField getCategoryNameTextField() {
        return categoryNameTextField;
    }

    public Button getAddCategoryButton() {
        return addCategoryButton;
    }

    public BoundingBoxTreeView getBoundingBoxExplorer() {
        return boundingBoxExplorer;
    }

    @Override
    public void connectToController(Controller controller) {
        addCategoryButton.setOnAction(action -> controller.onRegisterAddBoundingBoxCategoryAction());
        categorySelector.connectToController(controller);
        categoryNameTextField.setOnAction(action -> controller.onRegisterAddBoundingBoxCategoryAction());
    }

    @Override
    public void reset() {
        boundingBoxExplorer.reset();
    }

    BoundingBoxCategoryTableView getCategorySelector() {
        return categorySelector;
    }

    TextField getCategorySearchField() {
        return categorySearchField;
    }

    ColorPicker getCategoryColorPicker() {
        return categoryColorPicker;
    }

    TextField getTagInputField() {
        return tagEditor.getTagInputField();
    }

    private HBox createCategorySelectorTopPanel() {
        HBox.setHgrow(categorySearchField, Priority.ALWAYS);

        categorySearchField.setPromptText(SEARCH_CATEGORY_PROMPT_TEXT);
        categorySearchField.setFocusTraversable(false);

        Region categorySearchIcon = new Region();
        categorySearchIcon.setId(SEARCH_ICON_ID);

        Label categorySearchIconLabel = new Label();
        categorySearchIconLabel.setGraphic(categorySearchIcon);
        categorySearchIconLabel.setId(SEARCH_ICON_LABEL_ID);

        HBox categorySelectorTopPanel = new HBox(categorySearchIconLabel, categorySearchField);
        categorySelectorTopPanel.setId(CATEGORY_SELECTOR_TOP_PANEL_ID);

        return categorySelectorTopPanel;
    }

    private HBox createAddCategoryControlBox() {
        addCategoryButton.setFocusTraversable(false);

        HBox addItemControls = new HBox(categoryColorPicker, UiUtils.createHSpacer(),
                categoryNameTextField, UiUtils.createHSpacer(), addCategoryButton);
        categoryNameTextField.setPromptText(CATEGORY_INPUT_FIELD_PROMPT_TEXT);

        return addItemControls;
    }

    private HBox createBoundingBoxExplorerTopPanel() {
        HBox panel = new HBox(
                new Label(OBJECT_SELECTOR_LABEL_TEXT),
                UiUtils.createHSpacer(),
                visibilityToggle,
                autoShowToggle,
                expansionToggle
        );

        panel.setId(BOUNDING_BOX_EXPLORER_TOP_PANEL_ID);
        return panel;
    }

    private void setUpStyles() {
        categoryNameTextField.getStyleClass().add(BOUNDING_BOX_NAME_TEXT_FIELD_STYLE);
        categoryColorPicker.getStyleClass().add(BOUNDING_BOX_COLOR_PICKER_STYLE);
        categoryColorPicker.setValue(ColorUtils.createRandomColor());
        getStyleClass().add(SIDE_PANEL_STYLE);
    }

    private void setUpIds() {
        setId(PROJECT_SIDE_PANEL_ID);
        categoryNameTextField.setId(CATEGORY_INPUT_FIELD_ID);
        addCategoryButton.setId(ADD_BUTTON_ID);
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        categorySearchField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                categorySelector.getItems().stream()
                        .filter(item -> item.getName().startsWith(newValue))
                        .findAny()
                        .ifPresent(item -> {
                            categorySelector.getSelectionModel().select(item);
                            categorySelector.scrollTo(item);
                        });
            }
        }));

        categorySearchField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue) {
                categorySearchField.setText(null);
            }
        }));

        visibilityToggle.selectedProperty().addListener(((observable, oldValue, newValue) ->
                boundingBoxExplorer.getRoot().getChildren().stream()
                        .map(childItem -> (BoundingBoxCategoryTreeItem) childItem)
                        .forEach(childItem -> childItem.setIconToggledOn(!newValue))
        ));

        autoShowToggle.selectedProperty().addListener(((observable, oldValue, newValue) ->
                boundingBoxExplorer.setOnlyShowSelectedBoundingBox(newValue)));

        boundingBoxExplorer.rootProperty().addListener((observable, oldValue, newValue) -> {
            visibilityToggle.disableProperty().unbind();
            visibilityToggle.disableProperty().bind(boundingBoxExplorer.rootProperty().get().leafProperty());
            autoShowToggle.disableProperty().unbind();
            autoShowToggle.disableProperty().bind(boundingBoxExplorer.rootProperty().get().leafProperty());
            expansionToggle.disableProperty().unbind();
            expansionToggle.disableProperty().bind(boundingBoxExplorer.rootProperty().get().leafProperty());

        });

        expansionToggle.selectedProperty().addListener(((observable, oldValue, newValue) ->
                boundingBoxExplorer.getRoot().getChildren().forEach(child -> child.setExpanded(newValue))
        ));

        boundingBoxExplorer.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue instanceof BoundingBoxTreeItem) {
                tagEditor.setTags(newValue.getValue().getTags());
            } else {
                tagEditor.setTags(null);
            }
        });
    }
}
