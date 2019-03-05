package BoundingboxEditor;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;


import java.util.List;


public class MainView extends BorderPane implements View{
    private static final String ZOOM_ICON_PATH = "/icons/zoom.png";
    private static final String BRIGHTNESS_ICON_PATH = "/icons/brightness.png";
    private static final String SETTINGS_BOX_STYLE = "settings-box";
    private static final String IMAGE_PANE_STYLE = "pane";
    private static final String SIDE_PANEL_STYLE = "side-panel";
    private static final double ICON_WIDTH = 20.0;
    private static final double ICON_HEIGHT = 20.0;
    private static final double IMAGE_PADDING = 30.0;
    private static final String IMAGE_SETTINGS_LABEL_TEXT = "Image";
    private static final int ZOOM_SLIDER_MIN = 1;
    private static final double ZOOM_SLIDER_MAX = 1.5;
    private static final int ZOOM_SLIDER_DEFAULT = 1;
    private static final String SETTINGS_ITEM_STYLE = "settings-item-box";
    private static final double BRIGHTNESS_SLIDER_MIN = -0.5;
    private static final double BRIGHTNESS_SLIDER_MAX = 0.5;
    private static final int BRIGHTNESS_SLIDER_DEFAULT = 0;
    private static final String BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT = "Add";
    private static final String BOUNDING_BOX_ITEM_CONTROLS_STYLE = "table-view-input-controls";
    private static final String BOUNDING_BOX_NAME_TEXT_FIELD_STYLE = "bounding-box-name-text-field";
    private static final String BOUNDING_BOX_COLOR_PICKER_STYLE = "bounding-box-color-picker";
    private static final int SIDE_PANEL_SPACING = 5;
    private static final int SETTINGS_ITEM_SPACING = 10;
    private static final String TABLE_NAME_COLUMN_FACTORY_NAME = "name";
    private static final int TABLE_VIEW_COLOR_COLUMN_WIDTH = 5;
    private static final int TABLE_VIEW_DELETE_COLUMN_WIDTH = 19;
    private static final String TABLE_COLUMN_DELETE_BUTTON_STYLE = "delete-button";
    private static final String TABLE_VIEW_DELETE_ICON_STYLE = "icon";
    private static final String TABLE_VIEW_STYLE = "noheader-table-view";
    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;
    private static final String BOUNDING_BOX_TREE_VIEW_STYLE = "bounding-box-tree-view";
    private static final String CLASS_SELECTOR_LABEL_TEXT = "Category Editor";
    private static final String OBJECT_SELECTOR_LABEL_TEXT = "Explorer";
    private static final String BOTTOM_BAR_STYLE = "bottom-bar";
    private static final String DELETE_CONTEXT_MENU_STYLE = "delete-context-menu";
    private static final String DELETE_CONTEXT_MENU_TEXT = "Delete";

    private final Controller controller;

    // Top
    private final TopPanelView topPanelView = new TopPanelView();

    // Right
    private final VBox settingsPanel;
    private Slider zoomSlider;
    private Label brightnessLabel;
    private Slider brightnessSlider;

    // Center
    private final StackPane imagePane;
    private final DragAnchor mousePressed = new DragAnchor();
    private ImageView imageView;
    private ObservableList<SelectionRectangle> selectionRectangleList = FXCollections.observableArrayList();
    private List<ObservableList<SelectionRectangle>> imageSelectionRectangles;
    private SelectionRectangle selectionRectangle;
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    //FIXME: should be in model
    private Image currentImage;

    // Left
    private TableView<BoundingBoxCategory> boundingBoxItemTableView;
    private TextField nameInput;
    private TextField searchField;
    private ColorPicker boundingBoxColorPicker;
    private Button addButton;
    private TreeView<SelectionRectangle> boundingBoxItemTreeView;
    private TreeItem<SelectionRectangle> boundingBoxTreeViewRoot;

    // Bottom
    private Label bottomLabel;


    public MainView(Controller controller) {
        this.controller = controller;
        settingsPanel = createSettingsPanel();
        imagePane = createImagePane();

        this.setTop(topPanelView);
        this.setCenter(imagePane);
        this.setRight(settingsPanel);
        this.setLeft(createSelectionPanel());
        this.setBottom(createBottomBar());

        progressIndicator.setVisible(false);

        connectToController(controller);
        setActionsFromController();
        setInternalBindingsAndListeners();
    }

    public TopPanelView getTopPanelView() {
        return topPanelView;
    }

    public Button getPreviousButton() {
        return topPanelView.getPreviousButton();
    }

    public Button getNextButton() {
        return topPanelView.getNextButton();
    }

    public SelectionRectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public MenuItem getFileOpenFolderItem() {
        return topPanelView.getFileOpenFolderItem();
    }

    public void setImageView(final Image image) {
        // reset progress indicator animation
        currentImage = image;
        progressIndicator.setProgress(0);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setVisible(true);

        image.progressProperty().addListener((value, oldValue, newValue) -> {
            if (newValue.equals(1.0)) {
                imageView.setImage(image);
                imageView.setPreserveRatio(true);
                setInitialImageViewSize();
                progressIndicator.setVisible(false);
            }
        });
    }

    public TableView<BoundingBoxCategory> getBoundingBoxItemTableView() {
        return boundingBoxItemTableView;
    }

    public ToolBar getNavigationBar() {
        return topPanelView.getNavigationBar();
    }

    public TextField getNameInput() {
        return nameInput;
    }

    public ColorPicker getBoundingBoxColorPicker() {
        return boundingBoxColorPicker;
    }

    public TreeView<SelectionRectangle> getBoundingBoxItemTreeView() {
        return boundingBoxItemTreeView;
    }

    public void displayErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public DragAnchor getMousePressed() {
        return mousePressed;
    }

    public List<SelectionRectangle> getSelectionRectangleList() {
        return selectionRectangleList;
    }

    public Label getBottomLabel() {
        return bottomLabel;
    }

    public void setBottomLabel(Label bottomLabel) {
        this.bottomLabel = bottomLabel;
    }

    public TreeItem<SelectionRectangle> getBoundingBoxTreeViewRoot() {
        return boundingBoxTreeViewRoot;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public List<ObservableList<SelectionRectangle>> getImageSelectionRectangles() {
        return imageSelectionRectangles;
    }

    public void setImageSelectionRectangles(List<ObservableList<SelectionRectangle>> data){
        imageSelectionRectangles = data;
    }

    public void setSelectionRectangleList(ObservableList<SelectionRectangle> selectionRectangleList) {
        this.selectionRectangleList = selectionRectangleList;
    }

    private void setInitialImageViewSize() {
        final double imageWidth = imageView.getImage().getWidth();
        final double imageHeight = imageView.getImage().getHeight();
        final double maxAllowedWidth = imagePane.getWidth() - 2 * IMAGE_PADDING;
        final double maxAllowedHeight = imagePane.getHeight() - 2 * IMAGE_PADDING;

        imageView.setFitWidth(Math.min(imageWidth, maxAllowedWidth));
        imageView.setFitHeight(Math.min(imageHeight, maxAllowedHeight));
    }

    @Override
    public void connectToController(final Controller controller){
        topPanelView.connectToController(controller);
    }

    private void setActionsFromController() {
//        fileOpenFolderItem.setOnAction(controller::onRegisterOpenFolderAction);
//        fileSaveItem.setOnAction(controller::onRegisterSaveAction);
//        viewFitWindowItem.setOnAction(controller::onRegisterFitWindowAction);
//        fileExitItem.setOnAction(controller::onRegisterExitAction);
//
//        nextButton.setOnAction(controller::onRegisterNextAction);
//        previousButton.setOnAction(controller::onRegisterPreviousAction);

        addButton.setOnAction(controller::onRegisterAddBoundingBoxItemAction);
        imageView.setOnMousePressed(controller::onMousePressed);
        imageView.setOnMouseDragged(controller::onMouseDragged);
        imageView.setOnMouseReleased(controller::onMouseReleased);
    }

    private void setInternalBindingsAndListeners() {
        imagePane.widthProperty().addListener((value, oldValue, newValue) -> {
            double prefWidth = 0;
            if (imageView.getImage() != null)
                prefWidth = imageView.getImage().getWidth();
            imageView.setFitWidth(Math.min(prefWidth, newValue.doubleValue() - 2 * IMAGE_PADDING));
        });

        imagePane.heightProperty().addListener((value, oldValue, newValue) -> {
            double prefHeight = 0;
            if (imageView.getImage() != null)
                prefHeight = imageView.getImage().getHeight();
            imageView.setFitHeight(Math.min(prefHeight, newValue.doubleValue() - 2 * IMAGE_PADDING));
        });

        final ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.brightnessProperty().bind(brightnessSlider.valueProperty());
        imageView.setEffect(colorAdjust);

        // Make selection rectangle invisible and reset zoom-slider when the image changes.
        imageView.imageProperty().addListener((value, oldValue, newValue) -> {
            selectionRectangle.setVisible(false);
            zoomSlider.setValue(ZOOM_SLIDER_DEFAULT);
        });

        // not finished
        imagePane.setOnScroll(event -> {
            if (event.isControlDown()) {
                final Image image = imageView.getImage();
                final double delta = event.getDeltaY();

                final double newFitWidth = Utils.clamp(imageView.getFitWidth() + delta,
                        Math.min(ZOOM_MIN_WINDOW_RATIO * imagePane.getWidth(), image.getWidth()),
                        imagePane.getWidth() - 2 * IMAGE_PADDING);
                final double newFitHeight = Utils.clamp(imageView.getFitHeight() + delta,
                        Math.min(ZOOM_MIN_WINDOW_RATIO * imagePane.getHeight(), image.getHeight()),
                        imagePane.getHeight() - 2 * IMAGE_PADDING);

                imageView.setFitWidth(newFitWidth);
                imageView.setFitHeight(newFitHeight);
            }
        });

        // no finished
        zoomSlider.valueProperty().addListener((value, oldValue, newValue) -> {
            final Image image = imageView.getImage();
            final double delta = (newValue.doubleValue() - oldValue.doubleValue()) * 500;

            final double newFitWidth = Utils.clamp(imageView.getFitWidth() + delta,
                    Math.min(ZOOM_MIN_WINDOW_RATIO * imagePane.getWidth(), image.getWidth()),
                    imagePane.getWidth() - 2 * IMAGE_PADDING);
            final double newFitHeight = Utils.clamp(imageView.getFitHeight() + delta,
                    Math.min(ZOOM_MIN_WINDOW_RATIO * imagePane.getHeight(), image.getHeight()),
                    imagePane.getHeight() - 2 * IMAGE_PADDING);

            imageView.setFitWidth(newFitWidth);
            imageView.setFitHeight(newFitHeight);
        });

        // Reset brightnessSlider on Label double-click
        brightnessLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                brightnessSlider.setValue(BRIGHTNESS_SLIDER_DEFAULT);
            }
        });

        // when pressing enter after writing text, trigger creation of new main.java.BoundingboxEditor.BoundingBoxElement class
        nameInput.setOnAction(controller::onRegisterAddBoundingBoxItemAction);

        selectionRectangle.confineTo(imageView.boundsInParentProperty());

        // To remove settingsToolbar when it is not visible.
        settingsPanel.managedProperty().bind(settingsPanel.visibleProperty());
        settingsPanel.visibleProperty().bind(topPanelView.getViewShowSettingsItem().selectedProperty());

        boundingBoxItemTableView.getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
            if (newValue != null) {
                selectionRectangle.setStroke(newValue.getColor());
            }
        });
    }

    public void setSelectionRectangleListListener() {
        selectionRectangleList.addListener((ListChangeListener<SelectionRectangle>) c -> {
            while (c.next()) {
                for (SelectionRectangle selectionRectangle : c.getAddedSubList()) {
                    imagePane.getChildren().addAll(selectionRectangle.getNodes());

                    SelectionRectangleTreeItem treeItem = new SelectionRectangleTreeItem(selectionRectangle);

                    boolean found = false;
                    for (TreeItem<SelectionRectangle> category : boundingBoxTreeViewRoot.getChildren()) {
                        if (((SelectionRectangleCategoryTreeItem) category).getBoundingBoxCategory().equals(selectionRectangle.getBoundingBoxCategory())) {
                            treeItem.setId(category.getChildren().size() + 1);
                            category.getChildren().add(treeItem);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        SelectionRectangleCategoryTreeItem category = new SelectionRectangleCategoryTreeItem(selectionRectangle.getBoundingBoxCategory());
                        treeItem.setId(1);
                        category.getChildren().add(treeItem);
                        boundingBoxTreeViewRoot.getChildren().add(category);
                    }
                }

                for (SelectionRectangle removedItem : c.getRemoved()) {
                    imagePane.getChildren().removeAll(removedItem.getNodes());
                }
            }
        });
    }

    public void loadSelectionRectangleList(int index){
        if(imageSelectionRectangles.get(index) == null){
            imageSelectionRectangles.set(index, FXCollections.observableArrayList());
            selectionRectangleList.forEach(item -> imagePane.getChildren().removeAll(item.getNodes()));
            selectionRectangleList = imageSelectionRectangles.get(index);
            setSelectionRectangleListListener();
        }
        else {

            selectionRectangleList.forEach(item -> imagePane.getChildren().removeAll(item.getNodes()));
            selectionRectangleList = imageSelectionRectangles.get(index);
            selectionRectangleList.forEach(item -> imagePane.getChildren().addAll(item.getNodes()));

            // update selection rectangle in the scene grapH

        }
    }

    private VBox createSettingsPanel() {
        final VBox settingsBox = new VBox();
        settingsBox.getStyleClass().add(SETTINGS_BOX_STYLE);

        final Label imageSettingsLabel = new Label(IMAGE_SETTINGS_LABEL_TEXT);

        final Label zoomLabel = createIconLabel(ZOOM_ICON_PATH);
        zoomSlider = new Slider(ZOOM_SLIDER_MIN, ZOOM_SLIDER_MAX, ZOOM_SLIDER_DEFAULT);

        final HBox zoomHBox = new HBox(zoomLabel, zoomSlider);
        zoomHBox.getStyleClass().add(SETTINGS_ITEM_STYLE);
        zoomHBox.setSpacing(SETTINGS_ITEM_SPACING);

        brightnessLabel = createIconLabel(BRIGHTNESS_ICON_PATH);
        brightnessSlider = new Slider(BRIGHTNESS_SLIDER_MIN, BRIGHTNESS_SLIDER_MAX, BRIGHTNESS_SLIDER_DEFAULT);

        final HBox brightnessHBox = new HBox(brightnessLabel, brightnessSlider);
        brightnessHBox.getStyleClass().add(SETTINGS_ITEM_STYLE);
        brightnessHBox.setSpacing(SETTINGS_ITEM_SPACING);

        settingsBox.getChildren().addAll(new Separator(),
                imageSettingsLabel, zoomHBox, brightnessHBox, new Separator());
        settingsBox.setSpacing(SIDE_PANEL_SPACING);

        return settingsBox;
    }

    private StackPane createImagePane() {
        final StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add(IMAGE_PANE_STYLE);

        // The intermediary selectionRectangle, will be used to construct actual bounding boxes.
        selectionRectangle = new SelectionRectangle(null);
        imageView = new ImageView();
        imageView.setSmooth(true);
        imageView.setCache(true);
        /* So that events are registered even on transparent image parts. */
        imageView.setPickOnBounds(true);
        imageView.setId("image-pane");

        imagePane.getChildren().add(imageView);
        imagePane.getChildren().addAll(selectionRectangle.getNodes());
        imagePane.getChildren().add(progressIndicator);

        return imagePane;
    }



    private VBox createSelectionPanel() {
        final VBox sidePanel = new VBox();

        Label classSelectorLabel = new Label(CLASS_SELECTOR_LABEL_TEXT);

        HBox searchBar = new HBox();

        //Label searchTableLabel = createIconLabel(ICONS_SEARCH_ICON_PATH);
        searchField = new TextField();
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setPromptText("Search Category");
        searchField.setFocusTraversable(false);

        // https://stackoverflow.com/questions/40398905/search-tableview-list-in-javafx
        searchField.textProperty().addListener(((observable, oldValue, newValue) ->
                boundingBoxItemTableView.getItems().stream()
                        .filter(item -> item.getName().equals(newValue))
                        .findAny()
                        .ifPresent(item -> {
                            boundingBoxItemTableView.getSelectionModel().select(item);
                            boundingBoxItemTableView.scrollTo(item);
                        })
        ));

        searchField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue) {
                searchField.setText(null);
            }
        }));

        //FIXME: fix the textfield context menu style

        searchBar.getChildren().addAll(searchField);
        searchBar.setSpacing(10);

        boundingBoxItemTableView = createBoundingBoxTableView();

        nameInput = new TextField();
        nameInput.getStyleClass().add(BOUNDING_BOX_NAME_TEXT_FIELD_STYLE);

        boundingBoxColorPicker = new ColorPicker();
        boundingBoxColorPicker.getStyleClass().add(BOUNDING_BOX_COLOR_PICKER_STYLE);

        addButton = new Button(BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT);
        addButton.setFocusTraversable(false);
        // FIXME: for testing
        addButton.setId("add-button");

        final HBox addItemControls = new HBox(boundingBoxColorPicker, createHSpacer(), nameInput,
                createHSpacer(), addButton);
        addItemControls.getStyleClass().add(BOUNDING_BOX_ITEM_CONTROLS_STYLE);

        Label objectSelectorLabel = new Label(OBJECT_SELECTOR_LABEL_TEXT);
        boundingBoxItemTreeView = createBoundingBoxTreeView();

        sidePanel.getChildren().addAll(
                classSelectorLabel,
                searchBar,
                boundingBoxItemTableView,
                addItemControls,
                new Separator(Orientation.HORIZONTAL),
                objectSelectorLabel,
                boundingBoxItemTreeView);
        sidePanel.setSpacing(SIDE_PANEL_SPACING);

        sidePanel.getStyleClass().add(SIDE_PANEL_STYLE);

        return sidePanel;
    }

    private Button createIconButton(final String iconPath) {
        final Button button = new Button();
        final ImageView iconView = new ImageView(getClass().getResource(iconPath).toExternalForm());

        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);
        button.setGraphic(iconView);
        button.setFocusTraversable(false);

        return button;
    }

    private Label createIconLabel(final String iconPath) {
        final Label label = new Label();
        final ImageView iconView = new ImageView(getClass().getResource(iconPath).toExternalForm());

        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);
        label.setGraphic(iconView);

        return label;
    }

    private TableView<BoundingBoxCategory> createBoundingBoxTableView() {
        final TableView<BoundingBoxCategory> tableView = new TableView<>();
        tableView.setEditable(true);

        final TableColumn<BoundingBoxCategory, String> nameColumn = new TableColumn<>();
        nameColumn.setCellValueFactory(new PropertyValueFactory<>(TABLE_NAME_COLUMN_FACTORY_NAME));
        nameColumn.setEditable(true);
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        //FIXME: Sometimes the horizontal scrollbar is shown even though theoretically the width is correct
        //       TableView seems to add some padding/border by default.

        final TableColumn<BoundingBoxCategory, Color> colorColumn = new TableColumn<>();
        colorColumn.setMinWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        colorColumn.setMaxWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        colorColumn.setCellFactory(factory -> new ColorTableCell());

        final TableColumn<BoundingBoxCategory, BoundingBoxCategory> deleteColumn = new TableColumn<>();
        deleteColumn.setMinWidth(TABLE_VIEW_DELETE_COLUMN_WIDTH);
        deleteColumn.setMaxWidth(TABLE_VIEW_DELETE_COLUMN_WIDTH);
        deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        deleteColumn.setCellFactory(value -> new TableCell<>() {
            private final Button deleteButton = new Button();
            private final Region deleteIcon = new Region();

            @Override
            protected void updateItem(BoundingBoxCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setGraphic(null);
                    return;
                }

                setGraphic(deleteButton);
                deleteButton.getStyleClass().add(TABLE_COLUMN_DELETE_BUTTON_STYLE);
                deleteButton.setPickOnBounds(true);
                deleteIcon.getStyleClass().add(TABLE_VIEW_DELETE_ICON_STYLE);
                deleteButton.setGraphic(deleteIcon);
                deleteButton.setFocusTraversable(false);
                deleteButton.setOnAction(event -> getTableView().getItems().remove(item));
            }
        });

        final var tableColumns = tableView.getColumns();
        tableColumns.add(colorColumn);
        tableColumns.add(nameColumn);
        tableColumns.add(deleteColumn);
        tableView.getStyleClass().add(TABLE_VIEW_STYLE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return tableView;
    }

    private TreeView<SelectionRectangle> createBoundingBoxTreeView() {
        final TreeView<SelectionRectangle> treeView = new TreeView<>();
        VBox.setVgrow(treeView, Priority.ALWAYS);

        boundingBoxTreeViewRoot = new TreeItem<>();
        treeView.setRoot(boundingBoxTreeViewRoot);
        treeView.setShowRoot(false);
        treeView.getStyleClass().add(BOUNDING_BOX_TREE_VIEW_STYLE);

        treeView.setCellFactory(tv -> {
            SelectionRectangleTreeViewCell cell = new SelectionRectangleTreeViewCell();
            final MenuItem deleteSelectionRectangle = new MenuItem(DELETE_CONTEXT_MENU_TEXT);
            final ContextMenu deleteMenu = new ContextMenu(deleteSelectionRectangle);

            deleteSelectionRectangle.setId(DELETE_CONTEXT_MENU_STYLE);

            cell.emptyProperty().addListener((value, oldValue, newValue) -> {
                if (!newValue) {
                    cell.setContextMenu(deleteMenu);
                } else {
                    cell.setContextMenu(null);
                }
            });

            deleteSelectionRectangle.setOnAction(event -> {
                if (!cell.isEmpty()) {
                    final TreeItem<SelectionRectangle> treeItem = cell.getTreeItem();

                    treeItem.getChildren().forEach(child -> selectionRectangleList.remove(child.getValue()));
                    treeItem.getChildren().clear();

                    final TreeItem<SelectionRectangle> treeItemParent = treeItem.getParent();
                    final var siblingList = treeItemParent.getChildren();

                    if (treeItem instanceof SelectionRectangleTreeItem) {
                        for (int i = siblingList.indexOf(treeItem) + 1; i < siblingList.size(); ++i) {
                            final SelectionRectangleTreeItem item = (SelectionRectangleTreeItem) siblingList.get(i);
                            item.setId(item.getId() - 1);
                        }
                    }

                    siblingList.remove(treeItem);
                    selectionRectangleList.remove(treeItem.getValue());

                    if (siblingList.isEmpty()) {
                        boundingBoxTreeViewRoot.getChildren().remove(treeItemParent);
                    }
                }
            });

            cell.setOnMouseEntered(event -> {

                if (!cell.isEmpty()) {
                    final TreeItem<SelectionRectangle> treeItem = cell.getTreeItem();
                    final var childList = treeItem.getChildren();

                    if (!childList.isEmpty()) {
                        childList.forEach(child -> child.getValue().fillOpaque());
                    } else {
                        cell.getItem().fillOpaque();
                    }
                }
            });
            cell.setOnMouseExited(event -> {
                if (!cell.isEmpty()) {
                    final var childList = cell.getTreeItem().getChildren();

                    if (!childList.isEmpty()) {
                        childList.forEach(child -> child.getValue().setFill(Color.TRANSPARENT));
                    } else {
                        cell.getItem().setFill(Color.TRANSPARENT);
                    }
                }
            });

            return cell;
        });

        return treeView;
    }

    private HBox createBottomBar() {
        bottomLabel = new Label("");
        HBox bottomBar = new HBox(bottomLabel);

        bottomBar.getStyleClass().add(BOTTOM_BAR_STYLE);
        return bottomBar;
    }

    private Pane createHSpacer() {
        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }


    private class ColorTableCell extends TableCell<BoundingBoxCategory, Color> {

        @Override
        protected void updateItem(Color item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                final BoundingBoxCategory row = getTableRow().getItem();
                if (row != null) {
                    setStyle("-fx-background-color: " + Utils.rgbaFromColor(row.getColor()) + ";");
                }
            }
        }
    }

    private class SelectionRectangleTreeViewCell extends TreeCell<SelectionRectangle> {
        @Override
        protected void updateItem(SelectionRectangle newSelectionRectangle, boolean empty) {
            super.updateItem(newSelectionRectangle, empty);
            if (empty || newSelectionRectangle == null) {
                textProperty().unbind();
                setText(null);
                setGraphic(null);
                return;
            }

            TreeItem<SelectionRectangle> treeItem = getTreeItem();
            setGraphic(treeItem.getGraphic());

            if (!textProperty().isBound()) {
                if (treeItem instanceof SelectionRectangleCategoryTreeItem) {
                    textProperty().bind(((SelectionRectangleCategoryTreeItem) treeItem).getBoundingBoxCategory().nameProperty());
                } else if (treeItem instanceof SelectionRectangleTreeItem) {
                    textProperty().bind(newSelectionRectangle.getBoundingBoxCategory().nameProperty().concat(((SelectionRectangleTreeItem) treeItem).getId()));
                }
            }

        }
    }

    private class SelectionRectangleTreeItem extends TreeItem<SelectionRectangle> {
        private int id = 0;
        private final Rectangle toggleVisibilityIcon = new Rectangle(0, 0, 9, 9);

        public SelectionRectangleTreeItem(SelectionRectangle selectionRectangle) {
            super(selectionRectangle);
            setGraphic(toggleVisibilityIcon);

            toggleVisibilityIcon.fillProperty().bind(selectionRectangle.getBoundingBoxCategory().colorProperty());
            selectionRectangle.visibleProperty().bind(toggleVisibilityIcon.opacityProperty().greaterThan(0.5));

            toggleVisibilityIcon.setOnMousePressed(event -> toggleVisibilityIcon.setOpacity(toggleVisibilityIcon.getOpacity() > 0.5 ? 0.3 : 1.0));
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    private class SelectionRectangleCategoryTreeItem extends TreeItem<SelectionRectangle> {
        private final Rectangle toggleVisibilityIcon = new Rectangle(0, 0, 10, 10);
        private final BoundingBoxCategory boundingBoxCategory;

        public SelectionRectangleCategoryTreeItem(final BoundingBoxCategory category) {
            //FIXME: Currently this is a workaround, because cells that are not associated with a main.java.BoundingboxEditor.SelectionRectangle
            //       are considered empty, which leads to problems when deleting items.
            super(SelectionRectangle.getDummy());
            boundingBoxCategory = category;
            setGraphic(toggleVisibilityIcon);

            toggleVisibilityIcon.fillProperty().bind(category.colorProperty());

            toggleVisibilityIcon.setOnMousePressed(event -> {
                for (TreeItem<SelectionRectangle> childItem : getChildren()) {
                    childItem.getGraphic().setOpacity(toggleVisibilityIcon.getOpacity() > 0.5 ? 0.3 : 1.0);
                }
                toggleVisibilityIcon.setOpacity(toggleVisibilityIcon.getOpacity() > 0.5 ? 0.3 : 1.0);
            });
        }

        public BoundingBoxCategory getBoundingBoxCategory() {
            return boundingBoxCategory;
        }
    }
}
