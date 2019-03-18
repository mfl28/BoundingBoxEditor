package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.BoundingBoxCategory;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class MainView extends BorderPane implements View {

//    private static final double IMAGE_PADDING = 30.0;
//    private static final int ZOOM_SLIDER_DEFAULT = 1;
//    private static final int BRIGHTNESS_SLIDER_DEFAULT = 0;
//    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;

    private final TopPanelView topPanel = new TopPanelView();
    private final WorkspaceView workspace = new WorkspaceView();
    private final StatusPanelView statusPanel = new StatusPanelView();

    public MainView() {
        this.setTop(topPanel);
        this.setCenter(workspace);
        //this.setRight(settingsPanel);
//        this.setRight(imageExplorerPanel);
//        this.setLeft(projectSidePanel);
        this.setBottom(statusPanel);
        this.getStyleClass().add("pane");

        setInternalBindingsAndListeners();
        setExplorerCellFactory();
    }

    public ProjectSidePanelView getProjectSidePanel() {
        return workspace.getProjectSidePanel();
    }

    public Button getPreviousButton() {
        return workspace.getImageShower().getNavigationBar().getPreviousButton();
    }

    public Button getNextButton() {
        return workspace.getImageShower().getNavigationBar().getNextButton();
    }

    public SelectionRectangle getSelectionRectangle() {
        return workspace.getImageShower().getImagePane().getSelectionRectangle();
    }

    public ImageView getImageView() {
        return workspace.getImageShower().getImagePane().getImageView();
    }

    public void setImageView(final Image image) {
        workspace.getImageShower().getImagePane().setImageView(image);
    }

    public Image getCurrentImage() {
        return workspace.getImageShower().getImagePane().getCurrentImage();
    }

    public TableView<BoundingBoxCategory> getBoundingBoxItemTableView() {
        return workspace.getProjectSidePanel().getSelectorView();
    }

    public ToolBar getNavigationBar() {
        return workspace.getImageShower().getNavigationBar();
    }

    public TextField getCategoryInputField() {
        return workspace.getProjectSidePanel().getCategoryInputField();
    }

    public ColorPicker getBoundingBoxColorPicker() {
        return workspace.getProjectSidePanel().getBoundingBoxColorPicker();
    }

    public ImagePaneView getImagePaneView() {
        return workspace.getImageShower().getImagePane();
    }

    public void displayErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public DragAnchor getMousePressed() {
        return workspace.getImageShower().getImagePane().getMousePressed();
    }

    public List<SelectionRectangle> getSelectionRectangleList() {
        return workspace.getImageShower().getImagePane().getSelectionRectangleList();
    }

    public void setSelectionRectangleList(ObservableList<SelectionRectangle> selectionRectangleList) {
        workspace.getImageShower().getImagePane().setSelectionRectangleList(selectionRectangleList);
    }

    public Label getBottomLabel() {
        return statusPanel.getBottomLabel();
    }

    public TreeItem<SelectionRectangle> getBoundingBoxTreeViewRoot() {
        return workspace.getProjectSidePanel().getExplorerView().getRoot();
    }

    public ImageShowerView getImageShower() {
        return workspace.getImageShower();
    }

    public TextField getCategorySearchField() {
        return workspace.getProjectSidePanel().getCategorySearchField();
    }

    public List<ObservableList<SelectionRectangle>> getImageSelectionRectangles() {
        return workspace.getImageShower().getImagePane().getImageSelectionRectangles();
    }

    public ImageExplorerPanelView getImageExplorerPanel() {
        return workspace.getImageExplorer();
    }

    @Override
    public void connectToController(final Controller controller) {
        topPanel.connectToController(controller);
        workspace.connectToController(controller);
    }

    public void setSelectionRectangleListListener() {
        getImageShower().getImagePane().getSelectionRectangleList().addListener((ListChangeListener<SelectionRectangle>) c -> {
            while(c.next()) {
                List<? extends SelectionRectangle> addedItemsList = c.getAddedSubList();

                getImageShower().getImagePane().addSelectionRectanglesAsChildren(addedItemsList);
                getProjectSidePanel().getExplorerView().addTreeItemsFromSelectionRectangles(addedItemsList);

                getImageShower().getImagePane().removeSelectionRectanglesFromChildren(c.getRemoved());
            }
        });
    }

    public WorkspaceView getWorkspace() {
        return workspace;
    }

    private void setInternalBindingsAndListeners() {
//
//        final ColorAdjust colorAdjust = new ColorAdjust();
//        colorAdjust.brightnessProperty().bind(settingsPanel.getBrightnessSlider().valueProperty());
//        // FIXME: Throws exception when slider is used in case of no loaded image
//        imagePaneView.getImageView().setEffect(colorAdjust);
//
//        // Make selection rectangle invisible and reset zoom-slider when the image changes.
//        imagePaneView.getImageView().imageProperty().addListener((value, oldValue, newValue) -> {
//            imagePaneView.getSelectionRectangle().setVisible(false);
//            settingsPanel.getZoomSlider().setValue(ZOOM_SLIDER_DEFAULT);
//        });


//        // no finished
//        settingsPanel.getZoomSlider().valueProperty().addListener((value, oldValue, newValue) -> {
//            final ImageView imageView = imagePaneView.getImageView();
//            final Image image = imageView.getImage();
//            final double delta = (newValue.doubleValue() - oldValue.doubleValue()) * 500;
//
//            final double newFitWidth = Utils.clamp(imageView.getFitWidth() + delta,
//                    Math.min(ZOOM_MIN_WINDOW_RATIO * imagePaneView.getWidth(), image.getWidth()),
//                    imagePaneView.getWidth() - 2 * IMAGE_PADDING);
//            final double newFitHeight = Utils.clamp(imageView.getFitHeight() + delta,
//                    Math.min(ZOOM_MIN_WINDOW_RATIO * imagePaneView.getHeight(), image.getHeight()),
//                    imagePaneView.getHeight() - 2 * IMAGE_PADDING);
//
//            imageView.setFitWidth(newFitWidth);
//            imageView.setFitHeight(newFitHeight);
//        });
//
//        // Reset brightnessSlider on Label double-click
//        settingsPanel.getBrightnessLabel().setOnMouseClicked(event -> {
//            if(event.getClickCount() == 2) {
//                settingsPanel.getBrightnessSlider().setValue(BRIGHTNESS_SLIDER_DEFAULT);
//            }
//        });
//
//        // To remove settingsToolbar when it is not visible.
//        settingsPanel.managedProperty().bind(settingsPanel.visibleProperty());
//        settingsPanel.visibleProperty().bind(topPanel.getViewShowSettingsItem().selectedProperty());

        getProjectSidePanel().getSelectorView().getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
            if(newValue != null) {
                getImageShower().getImagePane().getSelectionRectangle().setStroke(newValue.getColor());
            }
        });

        topPanel.getSeparator().visibleProperty().bind(getNavigationBar().visibleProperty());

        // TODO: should only listen when folder is loaded.
        topPanel.getShowImageExplorerMenuItem().selectedProperty().addListener(((observable, oldValue, newValue) -> {
            getImageExplorerPanel().setVisible(newValue);
            if(!newValue) {
                getWorkspace().setDividerPosition(1, 1.0);
            }
        }));

        // TODO: Should be permanent -> switch between two different listeners
        topPanel.getViewFitWindowItem().selectedProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue) {
                getImageShower().getImagePane().setFitToWindow(true);
                getImageShower().getImagePane().fitToWindow();
            } else {
                getImageShower().getImagePane().setFitToWindow(false);
                getImageShower().getImagePane().setInitialImageViewSize();
            }
        }));
    }

    private void setExplorerCellFactory() {
        getProjectSidePanel().getExplorerView().setCellFactory(tv -> {

            SelectionRectangleTreeCell cell = new SelectionRectangleTreeCell();

            cell.getDeleteSelectionRectangleItem().setOnAction(event -> {
                if(!cell.isEmpty()) {
                    final TreeItem<SelectionRectangle> treeItem = cell.getTreeItem();

                    treeItem.getChildren().forEach(child -> getImageShower().getImagePane().getSelectionRectangleList().remove(child.getValue()));
                    treeItem.getChildren().clear();

                    final TreeItem<SelectionRectangle> treeItemParent = treeItem.getParent();
                    final var siblingList = treeItemParent.getChildren();

                    if(treeItem instanceof SelectionRectangleTreeItem) {
                        for(int i = siblingList.indexOf(treeItem) + 1; i < siblingList.size(); ++i) {
                            final SelectionRectangleTreeItem item = (SelectionRectangleTreeItem) siblingList.get(i);
                            item.setId(item.getId() - 1);
                        }
                    }

                    siblingList.remove(treeItem);
                    getImageShower().getImagePane().getSelectionRectangleList().remove(treeItem.getValue());

                    if(siblingList.isEmpty()) {
                        tv.getRoot().getChildren().remove(treeItemParent);
                    }
                }
            });

            return cell;
        });
    }
}
