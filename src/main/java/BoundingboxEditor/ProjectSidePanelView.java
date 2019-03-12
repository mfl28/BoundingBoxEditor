package BoundingboxEditor;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private static final double ICON_WIDTH = 20.0;
    private static final double ICON_HEIGHT = 20.0;
    private static final String HIDE_ICON_PATH = "/icons/hide.png";
    private static final String SHOW_ICON_PATH = "/icons/show.png";
    private static final String EXPAND_ICON_PATH = "/icons/expand.png";
    private static final String COLLAPSE_ICON_PATH = "/icons/collapse.png";
    private static final String CATEGORY_INPUT_FIELD_PROMPT_TEXT = "Category Name";

    private final BoundingBoxCategorySelectorView selectorView = new BoundingBoxCategorySelectorView();

    private final TextField categoryInputField = new TextField();
    private final TextField categorySearchField = new TextField();
    private final ColorPicker boundingBoxColorPicker = new ColorPicker();
    private final Button addButton = new Button(BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT);
    private final SelectionRectangleExplorerView explorerView = new SelectionRectangleExplorerView();
    private final ToggleButton visibilityToggle = Utils.createToggleIconButton(getClass().getResource(HIDE_ICON_PATH).toExternalForm(), ICON_WIDTH, ICON_HEIGHT);
    private final ToggleButton expansionToggle = Utils.createToggleIconButton(getClass().getResource(COLLAPSE_ICON_PATH).toExternalForm(), ICON_WIDTH, ICON_HEIGHT);
    private final ImageView hideImageView = new ImageView(new Image(getClass().getResource(HIDE_ICON_PATH).toExternalForm()));
    private final ImageView showImageView = new ImageView(new Image(getClass().getResource(SHOW_ICON_PATH).toExternalForm()));
    private final ImageView expandImageView = new ImageView(new Image(getClass().getResource(EXPAND_ICON_PATH).toExternalForm()));
    private final ImageView collapseImageView = new ImageView(new Image(getClass().getResource(COLLAPSE_ICON_PATH).toExternalForm()));

    ProjectSidePanelView() {
        this.getChildren().addAll(
                new Separator(),
                new Label(CLASS_SELECTOR_LABEL_TEXT),
                createCategorySearchBox(),
                selectorView,
                createAddCategoryControllBox(),
                new Separator(),
                new Label(OBJECT_SELECTOR_LABEL_TEXT),
                explorerView,
                new HBox(visibilityToggle, expansionToggle)
        );
        showImageView.setFitWidth(ICON_WIDTH);
        showImageView.setFitHeight(ICON_HEIGHT);
        hideImageView.setFitHeight(ICON_HEIGHT);
        hideImageView.setFitWidth(ICON_WIDTH);
        expandImageView.setFitWidth(ICON_WIDTH);
        expandImageView.setFitHeight(ICON_HEIGHT);
        collapseImageView.setFitHeight(ICON_HEIGHT);
        collapseImageView.setFitWidth(ICON_WIDTH);
        setUpStyles();
        setUpInternalListeners();
        this.setVisible(false);
        this.setManaged(false);
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

    public ToggleButton getVisibilityToggle() {
        return visibilityToggle;
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
        boundingBoxColorPicker.setValue(Utils.createRandomColor(new Random()));
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
            if(!newValue) {
                categorySearchField.setText(null);
            }
        }));

        visibilityToggle.selectedProperty().addListener(((observable, oldValue, newValue) -> {
            // TODO: SHould reset appropriately
            if(!explorerView.getRoot().getChildren().isEmpty()) {
                visibilityToggle.setGraphic(newValue ? hideImageView : showImageView);
            }
            explorerView.getRoot().getChildren().forEach((child) -> {
                child.getGraphic().setOpacity(newValue ? 0.3 : 1.0);
                child.getChildren().forEach((leaf) -> {
                    leaf.getGraphic().setOpacity(newValue ? 0.3 : 1.0);
                });
            });
        }));

        // FIXME: Flickers when changing between images that contain bounding box annotations
        visibilityToggle.disableProperty().bind(explorerView.getRoot().leafProperty());

        expansionToggle.disableProperty().bind(explorerView.getRoot().leafProperty());

        expansionToggle.selectedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!explorerView.getRoot().isLeaf()) {
                expansionToggle.setGraphic(newValue ? expandImageView : collapseImageView);
            }
            explorerView.getRoot().getChildren().forEach(child -> child.setExpanded(newValue));
        }));
    }

    private HBox createAddCategoryControllBox() {
        addButton.setFocusTraversable(false);
        // FIXME: for testing
        addButton.setId("add-button");

        final HBox addItemControls = new HBox(boundingBoxColorPicker, Utils.createHSpacer(), categoryInputField,
                Utils.createHSpacer(), addButton);
        categoryInputField.setPromptText(CATEGORY_INPUT_FIELD_PROMPT_TEXT);
        addItemControls.getStyleClass().add(BOUNDING_BOX_ITEM_CONTROLS_STYLE);

        return addItemControls;
    }
}
