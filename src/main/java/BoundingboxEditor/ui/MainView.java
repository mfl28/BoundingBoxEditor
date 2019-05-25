package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.io.IOResult;
import BoundingboxEditor.model.io.ImageAnnotationDataElement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.File;
import java.util.List;

public class MainView extends BorderPane implements View {
    private static final int INFO_DIALOGUE_MIN_WIDTH = 600;
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

    public static void displayInfoAlert(String title, String header, String content, Node additionalInfoNode) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(INFO_DIALOGUE_MIN_WIDTH);

        GridPane.setVgrow(additionalInfoNode, Priority.ALWAYS);
        GridPane.setHgrow(additionalInfoNode, Priority.ALWAYS);

        GridPane expandableContent = new GridPane();
        expandableContent.setMaxWidth(Double.MAX_VALUE);
        expandableContent.add(additionalInfoNode, 0, 0);

        alert.getDialogPane().setExpandableContent(expandableContent);
        alert.showAndWait();
    }

    public static void displayLoadResultInfoAlert(IOResult loadResult) {
        TableView<IOResult.ErrorTableEntry> errorTable = new TableView<>();
        TableColumn<IOResult.ErrorTableEntry, String> fileNameColumn = new TableColumn<>("File");

        TableColumn<IOResult.ErrorTableEntry, String> problemColumn = new TableColumn<>("Problem");
        errorTable.getColumns().add(fileNameColumn);
        errorTable.getColumns().add(problemColumn);
        errorTable.setEditable(false);
        errorTable.setMaxWidth(Double.MAX_VALUE);
        errorTable.setMaxHeight(Double.MAX_VALUE);

        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileNameColumn.setMaxWidth(150);
        fileNameColumn.setMinWidth(100);
        problemColumn.setCellValueFactory(new PropertyValueFactory<>("problemDescription"));
        problemColumn.setSortable(false);
        errorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        errorTable.setItems(FXCollections.observableArrayList(loadResult.getErrorEntries()));
        errorTable.getSortOrder().add(fileNameColumn);
        errorTable.sort();

        MainView.displayInfoAlert("Annotation import error report", "There were errors while loading annotations.",
                loadResult.getErrorEntries().size() + " image-annotation file(s) could not be loaded.",
                errorTable);
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

    public ImageView getImageView() {
        return workspace.getImageShower().getImagePane().getImageView();
    }

    public void updateImageFromFile(final File imageFile) {
        workspace.getImageShower().getImagePane().updateImageFromFile(imageFile);
    }

    public Image getCurrentImage() {
        return workspace.getImageShower().getImagePane().getCurrentImage();
    }

    public MenuItem getFileImportAnnotationsItem() {
        return topPanel.getFileImportAnnotationsItem();
    }

    public BoundingBoxCategorySelectorView getBoundingBoxCategorySelectorView() {
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

    public ObservableList<BoundingBoxView> getCurrentBoundingBoxes() {
        return workspace.getImageShower().getImagePane().getCurrentBoundingBoxes();
    }

    public StatusPanelView getStatusPanel() {
        return statusPanel;
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

    public void reset() {
        workspace.reset();
    }

    public BoundingBoxExplorerView getBoundingBoxExplorer() {
        return workspace.getProjectSidePanel().getBoundingBoxExplorer();
    }

    public void loadBoundingBoxesFromAnnotation(ImageAnnotationDataElement annotation) {
        List<BoundingBoxView> boundingBoxes = getBoundingBoxExplorer().extractBoundingBoxViewsAndBuildTreeFromAnnotation(annotation);

        boundingBoxes.forEach(item -> item.confineAndInitialize(getImageView().boundsInParentProperty()));

        // temporarily switch off automatic adding of boundingBoxes to the explorer (those are already imported)
        workspace.setTreeUpdateEnabled(false);
        getImagePaneView().setAllCurrentBoundingBoxes(boundingBoxes);
        workspace.setTreeUpdateEnabled(true);
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
