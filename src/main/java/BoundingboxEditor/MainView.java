package BoundingboxEditor;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class MainView extends BorderPane implements View {

    private static final double IMAGE_PADDING = 30.0;
    private static final int ZOOM_SLIDER_DEFAULT = 1;
    private static final int BRIGHTNESS_SLIDER_DEFAULT = 0;
    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;

    private final TopPanelView topPanel = new TopPanelView();
    private final SettingsPanelView settingsPanel = new SettingsPanelView();
    private final ImagePaneView imagePaneView = new ImagePaneView();
    private final ProjectSidePanelView projectSidePanel = new ProjectSidePanelView();
    private final StatusPanelView statusPanel = new StatusPanelView();

    public MainView() {
        this.setTop(topPanel);
        this.setCenter(imagePaneView);
        this.setRight(settingsPanel);
        this.setLeft(projectSidePanel);
        this.setBottom(statusPanel);

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
        return imagePaneView.getSelectionRectangle();
    }

    public ImageView getImageView() {
        return imagePaneView.getImageView();
    }

    public void setImageView(final Image image) {
        imagePaneView.setImageView(image);
    }

    public Image getCurrentImage() {
        return imagePaneView.getCurrentImage();
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
        return imagePaneView.getMousePressed();
    }

    public List<SelectionRectangle> getSelectionRectangleList() {
        return imagePaneView.getSelectionRectangleList();
    }

    public void setSelectionRectangleList(ObservableList<SelectionRectangle> selectionRectangleList) {
        imagePaneView.setSelectionRectangleList(selectionRectangleList);
    }

    public Label getBottomLabel() {
        return statusPanel.getBottomLabel();
    }

    public TreeItem<SelectionRectangle> getBoundingBoxTreeViewRoot() {
        return projectSidePanel.getExplorerView().getRoot();
    }

    public TextField getCategorySearchField() {
        return projectSidePanel.getCategorySearchField();
    }

    public List<ObservableList<SelectionRectangle>> getImageSelectionRectangles() {
        return imagePaneView.getImageSelectionRectangles();
    }

    public void setImageSelectionRectangles(List<ObservableList<SelectionRectangle>> data) {
        imagePaneView.setImageSelectionRectangles(data);
    }

    @Override
    public void connectToController(final Controller controller) {
        topPanel.connectToController(controller);
        projectSidePanel.connectToController(controller);
        imagePaneView.connectToController(controller);
    }

    public void setSelectionRectangleListListener() {
        imagePaneView.getSelectionRectangleList().addListener((ListChangeListener<SelectionRectangle>) c -> {
            while (c.next()) {
                for (SelectionRectangle selectionRectangle : c.getAddedSubList()) {
                    imagePaneView.getChildren().addAll(selectionRectangle.getNodes());

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
                    imagePaneView.getChildren().removeAll(removedItem.getNodes());
                }
            }
        });
    }

    public void loadSelectionRectangleList(int index) {
        List<ObservableList<SelectionRectangle>> imageSelectionRectangles = imagePaneView.getImageSelectionRectangles();
        List<SelectionRectangle> selectionRectangleList = imagePaneView.getSelectionRectangleList();

        if (imageSelectionRectangles.get(index) == null) {
            imageSelectionRectangles.set(index, FXCollections.observableArrayList());
            selectionRectangleList.forEach(item -> imagePaneView.getChildren().removeAll(item.getNodes()));
            imagePaneView.setSelectionRectangleList(imageSelectionRectangles.get(index));
            setSelectionRectangleListListener();
        } else {
            selectionRectangleList.forEach(item -> imagePaneView.getChildren().removeAll(item.getNodes()));
            imagePaneView.setSelectionRectangleList(imageSelectionRectangles.get(index));
            imagePaneView.getSelectionRectangleList().forEach(item -> imagePaneView.getChildren().addAll(item.getNodes()));

            // update selection rectangle in the scene grapH

        }
    }

    private void setInternalBindingsAndListeners() {

        final ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.brightnessProperty().bind(settingsPanel.getBrightnessSlider().valueProperty());
        // FIXME: Throws exception when slider is used in case of no loaded image
        imagePaneView.getImageView().setEffect(colorAdjust);

        // Make selection rectangle invisible and reset zoom-slider when the image changes.
        imagePaneView.getImageView().imageProperty().addListener((value, oldValue, newValue) -> {
            imagePaneView.getSelectionRectangle().setVisible(false);
            settingsPanel.getZoomSlider().setValue(ZOOM_SLIDER_DEFAULT);
        });


        // no finished
        settingsPanel.getZoomSlider().valueProperty().addListener((value, oldValue, newValue) -> {
            final ImageView imageView = imagePaneView.getImageView();
            final Image image = imageView.getImage();
            final double delta = (newValue.doubleValue() - oldValue.doubleValue()) * 500;

            final double newFitWidth = Utils.clamp(imageView.getFitWidth() + delta,
                    Math.min(ZOOM_MIN_WINDOW_RATIO * imagePaneView.getWidth(), image.getWidth()),
                    imagePaneView.getWidth() - 2 * IMAGE_PADDING);
            final double newFitHeight = Utils.clamp(imageView.getFitHeight() + delta,
                    Math.min(ZOOM_MIN_WINDOW_RATIO * imagePaneView.getHeight(), image.getHeight()),
                    imagePaneView.getHeight() - 2 * IMAGE_PADDING);

            imageView.setFitWidth(newFitWidth);
            imageView.setFitHeight(newFitHeight);
        });

        // Reset brightnessSlider on Label double-click
        settingsPanel.getBrightnessLabel().setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                settingsPanel.getBrightnessSlider().setValue(BRIGHTNESS_SLIDER_DEFAULT);
            }
        });

        // To remove settingsToolbar when it is not visible.
        settingsPanel.managedProperty().bind(settingsPanel.visibleProperty());
        settingsPanel.visibleProperty().bind(topPanel.getViewShowSettingsItem().selectedProperty());

        projectSidePanel.getSelectorView().getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
            if (newValue != null) {
                imagePaneView.getSelectionRectangle().setStroke(newValue.getColor());
            }
        });
    }

    private void setExplorerCellFactory() {
        projectSidePanel.getExplorerView().setCellFactory(tv -> {

            SelectionRectangleTreeCell cell = new SelectionRectangleTreeCell();

            cell.getDeleteSelectionRectangleItem().setOnAction(event -> {
                if (!cell.isEmpty()) {
                    final TreeItem<SelectionRectangle> treeItem = cell.getTreeItem();

                    treeItem.getChildren().forEach(child -> imagePaneView.getSelectionRectangleList().remove(child.getValue()));
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
                    imagePaneView.getSelectionRectangleList().remove(treeItem.getValue());

                    if (siblingList.isEmpty()) {
                        tv.getRoot().getChildren().remove(treeItemParent);
                    }
                }
            });

            return cell;
        });
    }
}
