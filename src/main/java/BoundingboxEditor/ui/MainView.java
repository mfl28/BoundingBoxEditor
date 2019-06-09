package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.io.BoundingBoxData;
import BoundingboxEditor.model.io.IOResult;
import BoundingboxEditor.model.io.ImageAnnotation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.controlsfx.dialog.ProgressDialog;

import java.io.File;
import java.util.List;

/**
 * The main view-component of the application (MVC architecture). Contains all other UI-elements.
 *
 * @see BorderPane
 * @see View
 * @see Controller
 * @see BoundingboxEditor.model.Model Model
 */
public class MainView extends BorderPane implements View {
    private static final int INFO_DIALOGUE_MIN_WIDTH = 600;
    private static final String MAIN_VIEW_ID = "main-view";
    private final HeaderView header = new HeaderView();
    private final WorkspaceSplitPaneView workspace = new WorkspaceSplitPaneView();
    private final StatusBarView statusPanel = new StatusBarView();

    /**
     * Constructs the app's main view-component which contains all other UI-elements.
     */
    public MainView() {
        setTop(header);
        setCenter(workspace);
        setBottom(statusPanel);

        setId(MAIN_VIEW_ID);
        setUpInternalListeners();
    }

    /**
     * Displays an error alert dialog-window.
     *
     * @param title   the title of the dialog
     * @param content the text-content of the dialog
     */
    public static void displayErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);

        alert.setContentText(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    /**
     * Displays a dialog-window that shows information about the result of an
     * IO-operation.
     *
     * @param ioResult the {@link IOResult} object containing the information tom display
     */
    public static void displayIOResultErrorInfoAlert(IOResult ioResult) {
        TableView<IOResult.ErrorInfoEntry> errorTable = new TableView<>();
        TableColumn<IOResult.ErrorInfoEntry, String> fileNameColumn = new TableColumn<>("File");

        TableColumn<IOResult.ErrorInfoEntry, String> problemColumn = new TableColumn<>("Error");
        errorTable.getColumns().add(fileNameColumn);
        errorTable.getColumns().add(problemColumn);
        errorTable.setEditable(false);
        errorTable.setMaxWidth(Double.MAX_VALUE);
        errorTable.setMaxHeight(Double.MAX_VALUE);

        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileNameColumn.setMaxWidth(150);
        fileNameColumn.setMinWidth(100);
        problemColumn.setCellValueFactory(new PropertyValueFactory<>("errorDescription"));
        problemColumn.setSortable(false);
        errorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        errorTable.setItems(FXCollections.observableArrayList(ioResult.getErrorTableEntries()));
        errorTable.getSortOrder().add(fileNameColumn);
        errorTable.sort();

        switch(ioResult.getOperationType()) {
            case ANNOTATION_IMPORT:
                MainView.displayInfoAlert("Annotation import error report", "There were errors while loading annotations.",
                        ioResult.getErrorTableEntries().size() + " image-annotation file(s) could not be loaded.", errorTable);
                break;
            case ANNOTATION_SAVING:
                MainView.displayInfoAlert("Annotation saving error report", "There were errors while saving annotations.",
                        ioResult.getErrorTableEntries().size() + " image-annotation file(s) could not be saved.", errorTable);
                break;
        }
    }

    /**
     * Displays a dialog showing the progress of a {@link Service} object's progress-property.
     *
     * @param service the {@link Service} whose progress will be shown
     * @param title   the title of the dialog-window
     * @param header  the header-text of the dialog-window
     */
    public static void displayServiceProgressDialog(Service service, String title, String header) {
        final ProgressDialog progressDialog = new ProgressDialog(service);
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(header);
    }

    public ImageFileExplorerView getImageExplorerPanel() {
        return workspace.getImageFileExplorer();
    }

    public ImageBoundingBoxEditorView getImageShower() {
        return workspace.getImageBoundingBoxEditor();
    }

    @Override
    public void connectToController(final Controller controller) {
        header.connectToController(controller);
        workspace.connectToController(controller);
    }

    @Override
    public void reset() {
        workspace.reset();
    }

    /* Delegating getters */

    /**
     * Updates the displayed image in the main image-pane from a provided image-{@link File}.
     *
     * @param imageFile the file of the new image
     */
    public void updateImageFromFile(final File imageFile) {
        workspace.getImageBoundingBoxEditor().getImagePane().updateImageFromFile(imageFile);
    }

    /**
     * Loads {@link BoundingBoxView} objects from data in a provided {@link ImageAnnotation} object into
     * the view-component. This results in displaying of the {@link BoundingBoxView} objects on top of the currently loaded
     * image and showing of corresponding tree-items in the {@link BoundingBoxTreeView} UI-element. This method
     * is called every time a previously stored {@link ImageAnnotation} object needs to be made visible to the user.
     *
     * @param annotation the image-annotation to load from
     */
    public void loadBoundingBoxViewsFromAnnotation(ImageAnnotation annotation) {
        List<BoundingBoxView> boundingBoxes = getBoundingBoxTreeView().extractBoundingBoxViewsAndBuildTreeFromAnnotation(annotation);
        ToggleGroup boundingBoxSelectionToggleGroup = getImagePane().getBoundingBoxSelectionGroup();

        boundingBoxes.forEach(item -> {
            item.autoScaleWithBoundsAndInitialize(getImageView().boundsInParentProperty());
            item.setToggleGroup(boundingBoxSelectionToggleGroup);
        });
        // temporarily switch off automatic adding of boundingBoxes to the explorer (those are already imported)
        workspace.setTreeUpdateEnabled(false);
        getImagePane().setAllCurrentBoundingBoxes(boundingBoxes);
        workspace.setTreeUpdateEnabled(true);
        // expand all first children of the (invisible) tree-root.
        workspace.getEditorsPanel().getBoundingBoxExplorer().getRoot().getChildren().forEach(item -> item.setExpanded(true));
    }

    /**
     * If a tree-item is currently selected, removes it and all of its child-tree-items. For any removed tree-items
     * which are of type {@link BoundingBoxTreeItem}, the associated {@link BoundingBoxView} objects are removed as well.
     */
    public void removeSelectedTreeItemAndChildren() {
        final TreeItem<BoundingBoxView> selectedTreeItem = getBoundingBoxTreeView().getSelectionModel().getSelectedItem();

        if(selectedTreeItem != null) {
            workspace.removeBoundingBoxWithTreeItemRecursively(selectedTreeItem);
        }
    }

    public BoundingBoxTreeView getBoundingBoxTreeView() {
        return workspace.getEditorsPanel().getBoundingBoxExplorer();
    }

    public BoundingBoxEditorImagePaneView getImagePane() {
        return workspace.getImageBoundingBoxEditor().getImagePane();
    }

    public ImageView getImageView() {
        return workspace.getImageBoundingBoxEditor().getImagePane().getImageView();
    }

    public Button getPreviousImageNavigationButton() {
        return workspace.getImageBoundingBoxEditor().getImageToolBar().getPreviousButton();
    }

    public Button getNextImageNavigationButton() {
        return workspace.getImageBoundingBoxEditor().getImageToolBar().getNextButton();
    }

    public ImageFileListView getImageGallery() {
        return workspace.getImageFileExplorer().getImageFileListView();
    }

    public TextField getImageFileSearchField() {
        return workspace.getImageFileExplorer().getImageFileSearchField();
    }

    public Image getCurrentImage() {
        return workspace.getImageBoundingBoxEditor().getImagePane().getCurrentImage();
    }

    public MenuItem getFileImportAnnotationsItem() {
        return header.getFileImportAnnotationsItem();
    }

    public BoundingBoxCategoryTableView getBoundingBoxCategoryTableView() {
        return workspace.getEditorsPanel().getCategorySelector();
    }

    public TextField getBoundingBoxCategoryInputField() {
        return workspace.getEditorsPanel().getCategoryNameTextField();
    }

    public ColorPicker getBoundingBoxCategoryColorPicker() {
        return workspace.getEditorsPanel().getCategoryColorPicker();
    }

    public ObservableList<BoundingBoxView> getCurrentBoundingBoxes() {
        return workspace.getImageBoundingBoxEditor().getImagePane().getCurrentBoundingBoxes();
    }

    public StatusBarView getStatusPanel() {
        return statusPanel;
    }

    public TextField getCategorySearchField() {
        return workspace.getEditorsPanel().getCategorySearchField();
    }

    public List<BoundingBoxData> getCurrentBoundingBoxData() {
        return getBoundingBoxTreeView().extractCurrentBoundingBoxData();
    }

    public TextField getTagInputField() {
        return getEditorsPanelView().getTagInputField();
    }

    public EditorsPanelView getEditorsPanelView() {
        return workspace.getEditorsPanel();
    }

    private void setUpInternalListeners() {
        header.getSeparator().visibleProperty().bind(workspace.visibleProperty());
        header.getShowImageExplorerMenuItem().disableProperty().bind(workspace.visibleProperty().not());
        header.getViewFitWindowItem().disableProperty().bind(workspace.visibleProperty().not());

        header.getShowImageExplorerMenuItem().selectedProperty().addListener(((observable, oldValue, newValue) -> {
            getImageExplorerPanel().setVisible(newValue);

            if(!newValue) {
                workspace.setDividerPosition(1, 1.0);
            }
        }));

        header.getViewFitWindowItem().selectedProperty().addListener(((observable, oldValue, newValue) -> {
            getImageShower().getImagePane().setMaximizeImageView(newValue);

            if(newValue) {
                getImageShower().getImagePane().setImageViewToMaxAllowedSize();
            } else {
                getImageShower().getImagePane().setImageViewToPreferOriginalImageSize();
            }
        }));
    }

    private static void displayInfoAlert(String title, String header, String content, Node additionalInfoNode) {
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
}
