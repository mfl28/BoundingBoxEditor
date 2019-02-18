import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class View extends BorderPane {
    private static final String NEXT_ICON_PATH = "icons/arrow_right.png";
    private static final String PREVIOUS_ICON_PATH = "icons/arrow_left.png";
    private static final String ZOOM_ICON_PATH = "icons/zoom.png";
    private static final String BRIGHTNESS_ICON_PATH = "icons/brightness.png";
    private static final String TOP_BOX_STYLE = "topBox";
    private static final String SETTINGS_BOX_STYLE = "settings-box";
    private static final String IMAGE_PANE_STYLE = "pane";
    private static final String SIDE_PANEL_STYLE = "side-panel";
    private static final String FILE_MENU_TEXT = "_File";
    private static final String VIEW_MENU_TEXT = "_View";
    private static final String OPEN_FOLDER_TEXT = "_Open Folder...";
    private static final String SAVE_TEXT = "_Save...";
    private static final String FIT_WINDOW_TEXT = "_Fit Window";
    private static final String SHOW_SETTINGS_BAR_TEXT = "Settings Bar";
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
    private static final int TABLE_VIEW_DELETE_COLUMN_WIDTH = 20;
    private static final String TABLE_COLUMN_DELETE_BUTTON_STYLE = "delete-button";
    private static final String TABLE_VIEW_DELETE_ICON_STYLE = "icon";
    private static final String TABLE_VIEW_STYLE = "noheader-table-view";
    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;

    private final Controller controller;

    // Top
    private MenuItem fileOpenFolderItem;
    private MenuItem fileSaveItem;
    private MenuItem viewFitWindowItem;
    private CheckMenuItem viewShowSettingsItem;
    private ToolBar navBar;
    private Button nextButton;
    private Button previousButton;

    // Right
    private final VBox settingsPanel;
    private Slider zoomSlider;
    private Label brightnessLabel;
    private Slider brightnessSlider;

    // Center
    private final StackPane imagePane;
    private final DragAnchor mousePressed = new DragAnchor();
    private ImageView imageView;
    private SelectionRectangle selectionRectangle;

    // Left
    private TableView<BoundingBoxItem> boundingBoxItemTableView;
    private TextField nameInput;
    private ColorPicker boundingBoxColorPicker;
    private Button addButton;

    //private final DoubleProperty zoomRatio = new SimpleDoubleProperty(0.5);

    public View(Controller controller) {
        this.controller = controller;
        settingsPanel = createSettingsPanel();
        imagePane = createImagePane();

        this.setTop(createTopBox());
        this.setCenter(imagePane);
        this.setRight(settingsPanel);
        this.setLeft(createSelectionPanel());

        setActionsFromController();
        setInternalBindingsAndListeners();
    }

    public Button getPreviousButton() {
        return previousButton;
    }

    public Button getNextButton() {
        return nextButton;
    }

    public SelectionRectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(Image image) {

        imageView.setImage(image);
        imageView.setPreserveRatio(true);

        setInitialImageViewSize();
    }

    public TableView<BoundingBoxItem> getBoundingBoxItemTableView() {
        return boundingBoxItemTableView;
    }

    public ToolBar getNavBar() {
        return navBar;
    }

    public TextField getNameInput() {
        return nameInput;
    }

    public ColorPicker getBoundingBoxColorPicker() {
        return boundingBoxColorPicker;
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

    private void setInitialImageViewSize() {
        final double imageWidth = imageView.getImage().getWidth();
        final double imageHeight = imageView.getImage().getHeight();
        final double maxAllowedWidth = imagePane.getWidth() - 2 * IMAGE_PADDING;
        final double maxAllowedHeight = imagePane.getHeight() - 2 * IMAGE_PADDING;

        imageView.setFitWidth(Math.min(imageWidth, maxAllowedWidth));
        imageView.setFitHeight(Math.min(imageHeight, maxAllowedHeight));
    }

    private void setActionsFromController() {
        fileOpenFolderItem.setOnAction(controller::onRegisterOpenFolderAction);
        fileSaveItem.setOnAction(controller::onRegisterSaveAction);
        viewFitWindowItem.setOnAction(controller::onRegisterFitWindowAction);
        nextButton.setOnAction(controller::onRegisterNextAction);
        previousButton.setOnAction(controller::onRegisterPreviousAction);
        addButton.setOnAction(controller::onRegisterAddBoundingBoxItemAction);
        imageView.setOnMousePressed(controller::onMousePressed);
        imageView.setOnMouseDragged(controller::onMouseDragged);
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

        imageView.boundsInParentProperty().addListener((observable, oldValue, newValue) -> {
            selectionRectangle.setWidth(selectionRectangle.getWidth() * newValue.getWidth() / oldValue.getWidth());
            selectionRectangle.setHeight(selectionRectangle.getHeight() * newValue.getHeight() / oldValue.getHeight());

            selectionRectangle.setX(newValue.getMinX() + (selectionRectangle.getX()
                    - oldValue.getMinX()) * newValue.getWidth() / oldValue.getWidth());
            selectionRectangle.setY(newValue.getMinY() + (selectionRectangle.getY()
                    - oldValue.getMinY()) * newValue.getHeight() / oldValue.getHeight());
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


        selectionRectangle.confineTo(imageView.boundsInParentProperty());

        // To remove settingsToolbar when it is not visible.
        settingsPanel.managedProperty().bind(settingsPanel.visibleProperty());
        settingsPanel.visibleProperty().bind(viewShowSettingsItem.selectedProperty());

        boundingBoxItemTableView.getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
            if (newValue != null) {
                selectionRectangle.setStroke(newValue.getColor());
            }
        });

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

        selectionRectangle = new SelectionRectangle();
        imageView = new ImageView();
        imageView.setSmooth(true);
        imageView.setCache(true);
        /* So that events are registered even on transparent image parts. */
        imageView.setPickOnBounds(true);

        imagePane.getChildren().add(imageView);
        imagePane.getChildren().addAll(selectionRectangle.getNodes());

        return imagePane;
    }

    private VBox createTopBox() {
        final VBox topBox = new VBox();

        navBar = createNavBar();

        topBox.getChildren().addAll(createMenuBar(), new Separator(), navBar);
        topBox.getStyleClass().add(TOP_BOX_STYLE);

        return topBox;
    }

    private MenuBar createMenuBar() {
        final MenuBar menuBar = new MenuBar();

        final Menu fileMenu = new Menu(FILE_MENU_TEXT);
        final Menu viewMenu = new Menu(VIEW_MENU_TEXT);

        fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT);

        viewFitWindowItem = new MenuItem(FIT_WINDOW_TEXT);
        viewShowSettingsItem = new CheckMenuItem(SHOW_SETTINGS_BAR_TEXT);

        fileSaveItem = new MenuItem(SAVE_TEXT);

        fileMenu.getItems().addAll(fileOpenFolderItem, fileSaveItem);
        viewMenu.getItems().addAll(viewFitWindowItem, viewShowSettingsItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu);

        return menuBar;
    }

    private ToolBar createNavBar() {
        final ToolBar toolBar = new ToolBar();

        nextButton = createIconButton(NEXT_ICON_PATH);
        previousButton = createIconButton(PREVIOUS_ICON_PATH);

        toolBar.getItems().addAll(createHSpacer(), previousButton, nextButton, createHSpacer());
        toolBar.setVisible(false);

        return toolBar;
    }

    private VBox createSelectionPanel() {
        final VBox sidePanel = new VBox();

        boundingBoxItemTableView = createBoundingBoxTableView();

        nameInput = new TextField();
        nameInput.getStyleClass().add(BOUNDING_BOX_NAME_TEXT_FIELD_STYLE);

        boundingBoxColorPicker = new ColorPicker();
        boundingBoxColorPicker.getStyleClass().add(BOUNDING_BOX_COLOR_PICKER_STYLE);

        addButton = new Button(BOUNDING_BOX_ITEM_ADD_BUTTON_TEXT);
        addButton.setFocusTraversable(false);

        final HBox addItemControls = new HBox(nameInput, createHSpacer(), boundingBoxColorPicker,
                createHSpacer(), addButton);
        addItemControls.getStyleClass().add(BOUNDING_BOX_ITEM_CONTROLS_STYLE);

        sidePanel.getChildren().addAll(boundingBoxItemTableView, addItemControls);
        sidePanel.setSpacing(SIDE_PANEL_SPACING);

        sidePanel.getStyleClass().add(SIDE_PANEL_STYLE);

        return sidePanel;
    }

    private Button createIconButton(String iconPath) {
        final Button button = new Button();
        final ImageView iconView = new ImageView(getClass().getResource(iconPath).toString());

        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);
        button.setGraphic(iconView);
        button.setFocusTraversable(false);

        return button;
    }

    private Label createIconLabel(String iconPath) {
        final Label label = new Label();
        final ImageView iconView = new ImageView(getClass().getResource(iconPath).toString());

        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);
        label.setGraphic(iconView);

        return label;
    }

    private TableView<BoundingBoxItem> createBoundingBoxTableView() {
        final TableView<BoundingBoxItem> tableView = new TableView<>();
        tableView.setEditable(true);

        final TableColumn<BoundingBoxItem, String> nameColumn = new TableColumn<>();
        nameColumn.setCellValueFactory(new PropertyValueFactory<>(TABLE_NAME_COLUMN_FACTORY_NAME));
        nameColumn.setEditable(true);
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        final TableColumn<BoundingBoxItem, Color> colorColumn = new TableColumn<>();
        colorColumn.setMinWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        colorColumn.setMaxWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        colorColumn.setCellFactory(factory -> new ColorTableCell());

        final TableColumn<BoundingBoxItem, BoundingBoxItem> deleteColumn = new TableColumn<>();
        deleteColumn.setMinWidth(TABLE_VIEW_DELETE_COLUMN_WIDTH);
        deleteColumn.setMaxWidth(TABLE_VIEW_DELETE_COLUMN_WIDTH);
        deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        deleteColumn.setCellFactory(value -> new TableCell<>() {
            private final Button deleteButton = new Button();
            private final Region deleteIcon = new Region();

            @Override
            protected void updateItem(BoundingBoxItem item, boolean empty) {
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

    private Pane createHSpacer() {
        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private class ColorTableCell extends TableCell<BoundingBoxItem, Color> {

        @Override
        protected void updateItem(Color item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                final BoundingBoxItem row = getTableRow().getItem();
                if (row != null) {
                    setStyle("-fx-background-color: "
                            + row.getColor().toString().replace("0x", "#") + ";");
                }
            }
        }
    }
}
