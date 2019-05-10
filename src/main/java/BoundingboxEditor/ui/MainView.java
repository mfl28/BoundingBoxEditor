package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.io.ImageAnnotationDataElement;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class MainView extends BorderPane implements View {
    private final TopPanelView topPanel = new TopPanelView();
    private final WorkspaceView workspace = new WorkspaceView();
    private final StatusPanelView statusPanel = new StatusPanelView();

    public MainView() {
        setTop(topPanel);
        setCenter(workspace);
        setBottom(statusPanel);

        getStyleClass().add("pane");

        setUpInternalListeners();
    }

    public static void displayErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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

    public BoundingBoxView getInitializerBoundingBox() {
        return workspace.getImageShower().getImagePane().getInitializerBoundingBox();
    }

    public ImageView getImageView() {
        return workspace.getImageShower().getImagePane().getImageView();
    }

    public void setImageView(final Image image) {
        workspace.getImageShower().getImagePane().updateImage(image);
    }

    public Image getCurrentImage() {
        return workspace.getImageShower().getImagePane().getCurrentImage();
    }

    public TableView<BoundingBoxCategory> getBoundingBoxCategoryTableView() {
        return workspace.getProjectSidePanel().getCategorySelector();
    }

    public TextField getBoundingBoxCategoryInputField() {
        return workspace.getProjectSidePanel().getCategoryNameTextField();
    }

    public ColorPicker getBoundingBoxCategoryColorPicker() {
        return workspace.getProjectSidePanel().getCategoryColorPicker();
    }

    public ImagePaneView getImagePaneView() {
        return workspace.getImageShower().getImagePane();
    }

    public DragAnchor getImagePaneDragAnchor() {
        return workspace.getImageShower().getImagePane().getDragAnchor();
    }

    public ObservableList<BoundingBoxView> getCurrentBoundingBoxes() {
        return workspace.getImageShower().getImagePane().getCurrentBoundingBoxes();
    }

    public Label getBottomLabel() {
        return statusPanel.getBottomLabel();
    }

    public TreeItem<BoundingBoxView> getBoundingBoxTreeViewRoot() {
        return workspace.getProjectSidePanel().getBoundingBoxExplorer().getRoot();
    }

    public ImageShowerView getImageShower() {
        return workspace.getImageShower();
    }

    public TextField getCategorySearchField() {
        return workspace.getProjectSidePanel().getCategorySearchField();
    }

    public ImageExplorerPanelView getImageExplorerPanel() {
        return workspace.getImageExplorer();
    }

    @Override
    public void connectToController(final Controller controller) {
        topPanel.connectToController(controller);
        workspace.connectToController(controller);
    }

    public void updateBoundingBoxDatabaseListener() {
        workspace.updateBoundingBoxDatabaseListener();
    }

    public void updateWorkspaceFromImageAnnotations(List<ImageAnnotationDataElement> imageAnnotations) {
        workspace.updateFromImageAnnotations(imageAnnotations);
    }

    public void reset() {
        workspace.reset();
    }

    public void updateBoundingBoxesInWorkspace() {
        workspace.updateCurrentImageFullyLoadedListener();
    }

    private void setUpInternalListeners() {
        topPanel.getSeparator().visibleProperty().bind(workspace.visibleProperty());
        topPanel.getShowImageExplorerMenuItem().disableProperty().bind(workspace.visibleProperty().not());
        topPanel.getViewFitWindowItem().disableProperty().bind(workspace.visibleProperty().not());

        topPanel.getShowImageExplorerMenuItem().selectedProperty().addListener(((observable, oldValue, newValue) -> {
            getImageExplorerPanel().setVisible(newValue);

            if(!newValue) {
                workspace.setDividerPosition(1, 1.0);
            }
        }));

        topPanel.getViewFitWindowItem().selectedProperty().addListener(((observable, oldValue, newValue) -> {
            getImageShower().getImagePane().setMaximizeImageView(newValue);

            if(newValue) {
                getImageShower().getImagePane().setImageViewToMaxAllowedSize();
            } else {
                getImageShower().getImagePane().setImageViewToPreferOriginalImageSize();
            }
        }));
    }
}
