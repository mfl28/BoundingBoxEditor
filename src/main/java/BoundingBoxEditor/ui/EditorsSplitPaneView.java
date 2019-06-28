package BoundingBoxEditor.ui;

import BoundingBoxEditor.controller.Controller;
import BoundingBoxEditor.utils.ColorUtils;
import BoundingBoxEditor.utils.UiUtils;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A UI-element in the form of a side-panel which contains several UI-components which
 * can be used to interact with bounding-box data (the elements themselves, categories, tags).
 *
 * @see VBox
 * @see View
 */
public class EditorsSplitPaneView extends SplitPane implements View {
    private static final String CLASS_SELECTOR_LABEL_TEXT = "Categories";
    private static final String SEARCH_CATEGORY_PROMPT_TEXT = "Search Category";
    private static final String BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT = "Add";
    private static final String BOUNDING_BOX_COLOR_PICKER_STYLE = "bounding-box-color-picker";
    private static final String OBJECT_SELECTOR_LABEL_TEXT = "Annotated Objects";
    private static final String CATEGORY_INPUT_FIELD_PROMPT_TEXT = "Category Name";
    private static final String CATEGORY_INPUT_FIELD_ID = "category-input-field";
    private static final String ADD_BUTTON_ID = "add-button";
    private static final String TAG_EDITOR_LABEL_TEXT = "Tags";
    private static final String BOUNDING_BOX_EXPLORER_TOP_PANEL_ID = "bounding-box-explorer-top-panel";
    private static final String SEARCH_ICON_ID = "search-icon";
    private static final String SEARCH_ICON_LABEL_ID = "search-icon-label";
    private static final String CATEGORY_SELECTOR_TOP_PANEL_ID = "category-selector-top-panel";
    private static final String EXPAND_TREE_ITEMS_ICON_ID = "expand-tree-items-icon";
    private static final String COLLAPSE_TREE_ITEMS_ICON_ID = "collapse-tree-items-icon";
    private static final String CATEGORY_NAME_FIELD_TOOLTIP = "(Ctrl+N to focus)";
    private static final String CATEGORY_COLOR_PICKER_TOOLTIP = "Category Color";
    private static final String ADD_CATEGORY_BUTTON_TOOLTIP = "Add new Category";
    private static final String CATEGORY_SEARCH_FIELD_TOOLTIP = "(Ctrl+F to focus)";
    private static final String COLLAPSE_TREE_ITEMS_BUTTON_TOOLTIP = "Collapse All";
    private static final String EXPAND_TREE_ITEMS_BUTTON_TOOLTIP = "Expand All";
    private static final String SIDE_PANEL_BOX_STYLE = "side-panel-box";
    private static final String EDITORS_SPLIT_PANE_ID = "editors-split-pane";
    private static final String ADD_CATEGORY_BOX_ID = "add-category-box";
    private static final String TAG_BOX_ID = "tag-box";

    private final TextField categorySearchField = new TextField();
    private final BoundingBoxCategoryTableView boundingBoxCategoryTable = new BoundingBoxCategoryTableView();
    private final ColorPicker categoryColorPicker = new ColorPicker();
    private final TextField categoryNameTextField = new TextField();
    private final Button addCategoryButton = new Button(BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT);

    private final BoundingBoxTreeView boundingBoxTree = new BoundingBoxTreeView();
    private final IconButton expandTreeItemsButton = new IconButton(EXPAND_TREE_ITEMS_ICON_ID, IconButton.IconType.BACKGROUND);
    private final IconButton collapseTreeItemsButton = new IconButton(COLLAPSE_TREE_ITEMS_ICON_ID, IconButton.IconType.BACKGROUND);

    private final BoundingBoxTagScrollPaneView boundingBoxTagScrollPane = new BoundingBoxTagScrollPaneView();

    /**
     * Creates a new panel containing UI-components responsible for interactions
     * with bounding-box data (the elements themselves, categories, tags).
     */
    EditorsSplitPaneView() {
        getItems().addAll(
                createCategorySelectorBox(),
                createBoundingBoxExplorerBox(),
                createTagBox()
        );

        setOrientation(Orientation.VERTICAL);
        setId(EDITORS_SPLIT_PANE_ID);
        setUpButtonsAndTextFields();
        setUpInternalListeners();
    }

    /**
     * Returns the category name-input text-field.
     *
     * @return the text-field
     */
    public TextField getCategoryNameTextField() {
        return categoryNameTextField;
    }

    /**
     * Returns the button which allows the user to add a
     * {@link BoundingBoxEditor.model.BoundingBoxCategory BoundingBoxCategory}.
     *
     * @return the button
     */
    public Button getAddCategoryButton() {
        return addCategoryButton;
    }

    /**
     * Returns the {@link BoundingBoxTreeView} object which is responsible
     * for displaying currently existing bounding-boxes. It also provides
     * functionality to interact with the displayed bounding-boxes.
     *
     * @return the bounding-box tree
     */
    public BoundingBoxTreeView getBoundingBoxTree() {
        return boundingBoxTree;
    }

    @Override
    public void connectToController(Controller controller) {
        addCategoryButton.setOnAction(action -> controller.onRegisterAddBoundingBoxCategoryAction());
        boundingBoxCategoryTable.connectToController(controller);
        categoryNameTextField.setOnAction(action -> controller.onRegisterAddBoundingBoxCategoryAction());
    }

    @Override
    public void reset() {
        boundingBoxTree.reset();
    }

    /**
     * Returns the {@link BoundingBoxCategoryTableView} object which is responsible for
     * displaying the currently existing bounding-box categories. It also provides
     * functionality to interact with the displayed categories.
     *
     * @return the bounding-box category table
     */
    BoundingBoxCategoryTableView getBoundingBoxCategoryTable() {
        return boundingBoxCategoryTable;
    }

    /**
     * Returns the category-name search text-field.
     *
     * @return the text-field
     */
    TextField getCategorySearchField() {
        return categorySearchField;
    }

    /**
     * Returns the {@link ColorPicker} object which allows the user to
     * choose the color which should be associated with a new bounding-box category.
     *
     * @return the color-picker
     */
    ColorPicker getCategoryColorPicker() {
        return categoryColorPicker;
    }

    /**
     * Returns the tag-input text-field.
     *
     * @return the text-field
     */
    TextField getTagInputField() {
        return boundingBoxTagScrollPane.getTagInputField();
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

        HBox addCategoryBox = new HBox(categoryColorPicker, UiUtils.createHSpacer(),
                categoryNameTextField, UiUtils.createHSpacer(), addCategoryButton);
        categoryNameTextField.setPromptText(CATEGORY_INPUT_FIELD_PROMPT_TEXT);

        addCategoryBox.setId(ADD_CATEGORY_BOX_ID);

        return addCategoryBox;
    }

    private HBox createBoundingBoxExplorerTopPanel() {
        HBox panel = new HBox(
                new Label(OBJECT_SELECTOR_LABEL_TEXT),
                UiUtils.createHSpacer(),
                collapseTreeItemsButton,
                expandTreeItemsButton
        );

        panel.setId(BOUNDING_BOX_EXPLORER_TOP_PANEL_ID);
        return panel;
    }

    private VBox createCategorySelectorBox() {
        VBox categorySelectorBox = new VBox(
                new Label(CLASS_SELECTOR_LABEL_TEXT),
                createCategorySelectorTopPanel(),
                boundingBoxCategoryTable,
                createAddCategoryControlBox()
        );

        VBox.setVgrow(boundingBoxCategoryTable, Priority.ALWAYS);
        categorySelectorBox.getStyleClass().add(SIDE_PANEL_BOX_STYLE);

        return categorySelectorBox;
    }

    private VBox createBoundingBoxExplorerBox() {
        VBox boundingBoxExplorerBox = new VBox(
                createBoundingBoxExplorerTopPanel(),
                boundingBoxTree
        );

        VBox.setVgrow(boundingBoxTree, Priority.ALWAYS);
        boundingBoxExplorerBox.getStyleClass().add(SIDE_PANEL_BOX_STYLE);

        return boundingBoxExplorerBox;
    }

    private VBox createTagBox() {
        VBox tagBox = new VBox(
                new Label(TAG_EDITOR_LABEL_TEXT),
                boundingBoxTagScrollPane
        );

        VBox.setVgrow(boundingBoxTagScrollPane, Priority.ALWAYS);
        tagBox.getStyleClass().add(SIDE_PANEL_BOX_STYLE);
        tagBox.setId(TAG_BOX_ID);

        SplitPane.setResizableWithParent(tagBox, false);
        return tagBox;
    }

    private void setUpButtonsAndTextFields() {
        categoryNameTextField.setId(CATEGORY_INPUT_FIELD_ID);
        categoryNameTextField.setTooltip(new Tooltip(EditorsSplitPaneView.CATEGORY_NAME_FIELD_TOOLTIP));

        categoryColorPicker.getStyleClass().add(BOUNDING_BOX_COLOR_PICKER_STYLE);
        categoryColorPicker.setValue(ColorUtils.createRandomColor());
        categoryColorPicker.setTooltip(new Tooltip(CATEGORY_COLOR_PICKER_TOOLTIP));

        addCategoryButton.setId(ADD_BUTTON_ID);
        addCategoryButton.setTooltip(new Tooltip(ADD_CATEGORY_BUTTON_TOOLTIP));

        categorySearchField.setTooltip(new Tooltip(CATEGORY_SEARCH_FIELD_TOOLTIP));

        collapseTreeItemsButton.setTooltip(new Tooltip(COLLAPSE_TREE_ITEMS_BUTTON_TOOLTIP));

        expandTreeItemsButton.setTooltip(new Tooltip(EXPAND_TREE_ITEMS_BUTTON_TOOLTIP));
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        categorySearchField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                boundingBoxCategoryTable.getItems().stream()
                        .filter(item -> item.getName().startsWith(newValue))
                        .findAny()
                        .ifPresent(item -> {
                            boundingBoxCategoryTable.getSelectionModel().select(item);
                            boundingBoxCategoryTable.scrollTo(item);
                        });
            }
        }));

        categorySearchField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue) {
                categorySearchField.setText(null);
            }
        }));

        expandTreeItemsButton.setOnAction(event -> boundingBoxTree.expandAllTreeItems());

        collapseTreeItemsButton.setOnAction(event ->
                boundingBoxTree.getRoot().getChildren().forEach(child -> child.setExpanded(false)));

        boundingBoxTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue instanceof BoundingBoxTreeItem) {
                boundingBoxTagScrollPane.setTags(newValue.getValue().getTags());
            } else {
                boundingBoxTagScrollPane.setTags(null);
            }
        });
    }
}
