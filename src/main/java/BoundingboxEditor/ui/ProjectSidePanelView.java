package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.utils.ColorUtils;
import BoundingboxEditor.utils.UiUtils;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Random;

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
    private static final String HIDE_ICON_PATH = "/icons/hide.png";
    private static final String SHOW_ICON_PATH = "/icons/show.png";
    private static final String EXPAND_ICON_PATH = "/icons/expand.png";
    private static final String COLLAPSE_ICON_PATH = "/icons/collapse.png";
    private static final String CATEGORY_INPUT_FIELD_PROMPT_TEXT = "Category Name";
    private static final String PROJECT_SIDE_PANEL_ID = "project-side-panel";
    private static final String CATEGORY_INPUT_FIELD_ID = "category-input-field";
    private static final String ADD_BUTTON_ID = "add-button";
    private static final String TAG_EDITOR_LABEL_TEXT = "Tag Editor";


    private final TextField categorySearchField = new TextField();
    private final BoundingBoxCategorySelectorView categorySelector = new BoundingBoxCategorySelectorView();
    private final ColorPicker categoryColorPicker = new ColorPicker();
    private final TextField categoryNameTextField = new TextField();
    private final Button addCategoryButton = new Button(BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT);

    private final BoundingBoxExplorerView boundingBoxExplorer = new BoundingBoxExplorerView();
    private final ToggleButton visibilityToggle = new ToggleIconButton(SHOW_ICON_PATH, HIDE_ICON_PATH);
    private final ToggleButton expansionToggle = new ToggleIconButton(EXPAND_ICON_PATH, COLLAPSE_ICON_PATH);

    private final BoundingBoxTagEditorView tagEditor = new BoundingBoxTagEditorView();

    ProjectSidePanelView() {
        getChildren().addAll(
                new Label(CLASS_SELECTOR_LABEL_TEXT),
                createCategorySearchBox(),
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

    public BoundingBoxExplorerView getBoundingBoxExplorer() {
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

    BoundingBoxCategorySelectorView getCategorySelector() {
        return categorySelector;
    }

    TextField getCategorySearchField() {
        return categorySearchField;
    }

    ColorPicker getCategoryColorPicker() {
        return categoryColorPicker;
    }

    private HBox createCategorySearchBox() {
        HBox.setHgrow(categorySearchField, Priority.ALWAYS);

        categorySearchField.setPromptText(SEARCH_CATEGORY_PROMPT_TEXT);
        categorySearchField.setFocusTraversable(false);

        HBox categorySearchBox = new HBox(categorySearchField);
        categorySearchBox.setSpacing(CATEGORY_SEARCH_BOX_SPACING);

        return categorySearchBox;
    }

    private HBox createAddCategoryControlBox() {
        addCategoryButton.setFocusTraversable(false);

        HBox addItemControls = new HBox(categoryColorPicker, UiUtils.createHSpacer(),
                categoryNameTextField, UiUtils.createHSpacer(), addCategoryButton);
        categoryNameTextField.setPromptText(CATEGORY_INPUT_FIELD_PROMPT_TEXT);
        addItemControls.getStyleClass().add(BOUNDING_BOX_ITEM_CONTROLS_STYLE);

        return addItemControls;
    }

    private HBox createBoundingBoxExplorerTopPanel() {
        HBox panel = new HBox(new Label(OBJECT_SELECTOR_LABEL_TEXT), UiUtils.createHSpacer(), visibilityToggle, expansionToggle);
        panel.setSpacing(10);
        return panel;
    }

    private void setUpStyles() {
        categoryNameTextField.getStyleClass().add(BOUNDING_BOX_NAME_TEXT_FIELD_STYLE);
        categoryColorPicker.getStyleClass().add(BOUNDING_BOX_COLOR_PICKER_STYLE);
        categoryColorPicker.setValue(ColorUtils.createRandomColor(new Random()));
        setSpacing(SIDE_PANEL_SPACING);
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

        // TODO: should reset appropriately
        visibilityToggle.selectedProperty().addListener(((observable, oldValue, newValue) ->
                boundingBoxExplorer.getRoot().getChildren().stream()
                        .map(childItem -> (CategoryTreeItem) childItem)
                        .forEach(childItem -> childItem.setIconToggledOn(!newValue))
        ));

        // FIXME: Flickers when changing between images that contain bounding box annotations
        //  (actually normal because of image loading in between)
        boundingBoxExplorer.rootProperty().addListener((observable, oldValue, newValue) -> {
            visibilityToggle.disableProperty().unbind();
            expansionToggle.disableProperty().unbind();
            visibilityToggle.disableProperty().bind(boundingBoxExplorer.rootProperty().get().leafProperty());
            expansionToggle.disableProperty().bind(boundingBoxExplorer.rootProperty().get().leafProperty());
        });

        expansionToggle.selectedProperty().addListener(((observable, oldValue, newValue) ->
                boundingBoxExplorer.getRoot().getChildren().forEach(child -> child.setExpanded(newValue))
        ));

        tagEditor.maxWidthProperty().bind(widthProperty());

        boundingBoxExplorer.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue instanceof BoundingBoxTreeItem) {
                tagEditor.setTags(newValue.getValue().getTags());
            } else {
                tagEditor.setTags(null);
            }
        });
    }
}
