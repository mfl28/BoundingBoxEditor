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

    private static final int SIDE_PANEL_SPACING = 5;
    private static final int SETTINGS_ITEM_SPACING = 10;

    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;

    private static final String BOTTOM_BAR_STYLE = "bottom-bar";


    private final Controller controller;

    // Top
    private final TopPanelView topPanel = new TopPanelView();

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
    private final ProjectSidePanelView projectSidePanel = new ProjectSidePanelView();

    // Bottom
    private Label bottomLabel;


    public MainView(Controller controller) {
        this.controller = controller;
        settingsPanel = createSettingsPanel();
        imagePane = createImagePane();

        this.setTop(topPanel);
        this.setCenter(imagePane);
        this.setRight(settingsPanel);
        this.setLeft(projectSidePanel);
        this.setBottom(createBottomBar());

        progressIndicator.setVisible(false);

        connectToController(controller);
        setActionsFromController();
        setInternalBindingsAndListeners();
        setExplorerCellFactory();
    }

    public TopPanelView getTopPanel() {
        return topPanel;
    }

    public ProjectSidePanelView getProjectSidePanel() {
        return projectSidePanel;
    }

    public Button getPreviousButton() {
        return topPanel.getPreviousButton();
    }

    public Button getNextButton() {
        return topPanel.getNextButton();
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
        return projectSidePanel.getSelectorView();
    }

    public ToolBar getNavigationBar() {
        return topPanel.getNavigationBar();
    }

    public TextField getCategoryInputField() {
        return projectSidePanel.getCategoryInputField();
    }

    public ColorPicker getBoundingBoxColorPicker() {
        return projectSidePanel.getBoundingBoxColorPicker();
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
        return projectSidePanel.getExplorerView().getRoot();
    }

    public TextField getCategorySearchField() {
        return projectSidePanel.getCategorySearchField();
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
        topPanel.connectToController(controller);
        projectSidePanel.connectToController(controller);
    }

    private void setActionsFromController() {
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


        selectionRectangle.confineTo(imageView.boundsInParentProperty());

        // To remove settingsToolbar when it is not visible.
        settingsPanel.managedProperty().bind(settingsPanel.visibleProperty());
        settingsPanel.visibleProperty().bind(topPanel.getViewShowSettingsItem().selectedProperty());

        projectSidePanel.getSelectorView().getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
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

                    final List<TreeItem<SelectionRectangle>> categoryItems = projectSidePanel.getExplorerView().getRoot().getChildren();


                    boolean found = false;
                    for (TreeItem<SelectionRectangle> category : categoryItems) {
                        if (((CategoryTreeItem) category).getBoundingBoxCategory().equals(selectionRectangle.getBoundingBoxCategory())) {
                            treeItem.setId(category.getChildren().size() + 1);
                            category.getChildren().add(treeItem);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        CategoryTreeItem category = new CategoryTreeItem(selectionRectangle.getBoundingBoxCategory());
                        treeItem.setId(1);
                        category.getChildren().add(treeItem);
                        categoryItems.add(category);
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

    private void setExplorerCellFactory(){
        projectSidePanel.getExplorerView().setCellFactory(tv -> {

            SelectionRectangleTreeCell cell = new SelectionRectangleTreeCell();

            cell.getDeleteSelectionRectangleItem().setOnAction(event -> {
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
                        tv.getRoot().getChildren().remove(treeItemParent);
                    }
                }
            });

            return cell;
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

    private Label createIconLabel(final String iconPath) {
        final Label label = new Label();
        final ImageView iconView = new ImageView(getClass().getResource(iconPath).toExternalForm());

        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);
        label.setGraphic(iconView);

        return label;
    }

    private HBox createBottomBar() {
        bottomLabel = new Label("");
        HBox bottomBar = new HBox(bottomLabel);

        bottomBar.getStyleClass().add(BOTTOM_BAR_STYLE);
        return bottomBar;
    }
}
