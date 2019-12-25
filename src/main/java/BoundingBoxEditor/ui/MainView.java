package BoundingBoxEditor.ui;

import BoundingBoxEditor.controller.Controller;
import BoundingBoxEditor.model.io.BoundingBoxData;
import BoundingBoxEditor.model.io.IOResult;
import BoundingBoxEditor.model.io.ImageAnnotation;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.ProgressDialog;

import java.io.File;
import java.util.List;

/**
 * The main view-component of the application (MVC architecture). Contains all other UI-elements.
 *
 * @see BorderPane
 * @see View
 * @see Controller
 * @see BoundingBoxEditor.model.Model Model
 */
public class MainView extends BorderPane implements View {
    private static final int INFO_DIALOGUE_MIN_WIDTH = 600;
    private static final String MAIN_VIEW_ID = "main-view";
    private static final String ANNOTATION_IMPORT_ERROR_REPORT_TITLE = "Annotation import error report";
    private static final String ANNOTATION_SAVING_ERROR_REPORT_TITLE = "Annotation saving error report";

    private final HeaderView header = new HeaderView();
    private final WorkspaceSplitPaneView workspaceSplitPane = new WorkspaceSplitPaneView();
    private final StatusBarView statusBar = new StatusBarView();

    /**
     * Constructs the app's main view-component which contains all other UI-elements.
     */
    public MainView() {
        setTop(header);
        setCenter(workspaceSplitPane);
        setBottom(statusBar);

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
     * Displays a a dialog with 'Yes', 'No' and 'Cancel' buttons and returns the chosen option.
     *
     * @param title   The title of the dialog window
     * @param content The content text of the dialog window
     * @return {@link ButtonBar.ButtonData}.YES/NO/CANCEL_CLOSE
     */
    public static ButtonBar.ButtonData displayYesNoCancelDialogAndGetResult(String title, String content) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                content, new ButtonType("Yes", ButtonBar.ButtonData.YES),
                new ButtonType("No", ButtonBar.ButtonData.NO), new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(content);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.showAndWait();

        return dialog.getResult().getButtonData();
    }

    /**
     * Displays a directory chooser window and returns the chosen directory.
     *
     * @param title The title of the directory chooser window
     * @param stage The stage on top of which the window will be shown
     * @return The chosen directory, or null if the user closed the window without choosing.
     */
    public static File displayDirectoryChooserAndGetChoice(String title, Stage stage) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);

        return directoryChooser.showDialog(stage);
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
                MainView.displayInfoAlert(ANNOTATION_IMPORT_ERROR_REPORT_TITLE, "There were errors while loading annotations.",
                        ioResult.getErrorTableEntries().size() + " image-annotation file(s) could not be loaded.", errorTable);
                break;
            case ANNOTATION_SAVING:
                MainView.displayInfoAlert(ANNOTATION_SAVING_ERROR_REPORT_TITLE, "There were errors while saving annotations.",
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

    @Override
    public void connectToController(final Controller controller) {
        header.connectToController(controller);
        workspaceSplitPane.connectToController(controller);
    }

    @Override
    public void reset() {
        workspaceSplitPane.reset();
    }

    /**
     * Updates the displayed image in the main image-pane from a provided image-{@link File}.
     *
     * @param imageFile the file of the new image
     * @param width     the width of image corresponding to the file
     * @param height    the height of the image corresponding to the file
     */
    public void updateImageFromFile(final File imageFile, double width, double height) {
        workspaceSplitPane.getBoundingBoxEditor().getBoundingBoxEditorImagePane().updateImageFromFile(imageFile, width, height);
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
        List<BoundingBoxView> boundingBoxes = getBoundingBoxTree().extractBoundingBoxViewsAndBuildTreeFromAnnotation(annotation);
        ToggleGroup boundingBoxSelectionToggleGroup = getBoundingBoxEditorImagePane().getBoundingBoxSelectionGroup();

        boundingBoxes.forEach(item -> {
            item.autoScaleWithBoundsAndInitialize(getBoundingBoxEditorImageView().boundsInParentProperty());
            item.setToggleGroup(boundingBoxSelectionToggleGroup);
        });
        // Temporarily switch off automatic adding of boundingBoxes to the explorer (those are already imported)
        workspaceSplitPane.setTreeUpdateEnabled(false);
        getBoundingBoxEditorImagePane().setAllCurrentBoundingBoxes(boundingBoxes);
        workspaceSplitPane.setTreeUpdateEnabled(true);
        // Expand all tree-items in the bounding-box tree-view.
        workspaceSplitPane.getEditorsSplitPane().getBoundingBoxTree().expandAllTreeItems();
    }

    /**
     * If a tree-item is currently selected, removes it and all of its child-tree-items. For any removed tree-items
     * which are of type {@link BoundingBoxTreeItem}, the associated {@link BoundingBoxView} objects are removed as well.
     */
    public void removeSelectedTreeItemAndChildren() {
        final TreeItem<BoundingBoxView> selectedTreeItem = getBoundingBoxTree().getSelectionModel().getSelectedItem();

        if(selectedTreeItem != null) {
            workspaceSplitPane.removeBoundingBoxWithTreeItemRecursively(selectedTreeItem);
        }
    }

    /**
     * Checks if the {@link BoundingBoxEditorImagePaneView}-member currently contains bounding boxes.
     *
     * @return true if there exist bounding boxes, false otherwise.
     */
    public boolean containsBoundingBoxViews() {
        return !getCurrentBoundingBoxes().isEmpty();
    }

    /* Delegating Getters */

    public ImageFileExplorerView getImageFileExplorer() {
        return workspaceSplitPane.getImageFileExplorer();
    }

    public BoundingBoxEditorView getBoundingBoxEditor() {
        return workspaceSplitPane.getBoundingBoxEditor();
    }

    public BoundingBoxTreeView getBoundingBoxTree() {
        return workspaceSplitPane.getEditorsSplitPane().getBoundingBoxTree();
    }

    public BoundingBoxEditorImagePaneView getBoundingBoxEditorImagePane() {
        return workspaceSplitPane.getBoundingBoxEditor().getBoundingBoxEditorImagePane();
    }

    public ImageView getBoundingBoxEditorImageView() {
        return workspaceSplitPane.getBoundingBoxEditor().getBoundingBoxEditorImagePane().getImageView();
    }

    public Button getPreviousImageNavigationButton() {
        return workspaceSplitPane.getBoundingBoxEditor().getBoundingBoxEditorToolBar().getPreviousButton();
    }

    public Button getNextImageNavigationButton() {
        return workspaceSplitPane.getBoundingBoxEditor().getBoundingBoxEditorToolBar().getNextButton();
    }

    public ImageFileListView getImageFileListView() {
        return workspaceSplitPane.getImageFileExplorer().getImageFileListView();
    }

    public TextField getImageFileSearchField() {
        return workspaceSplitPane.getImageFileExplorer().getImageFileSearchField();
    }

    public Image getCurrentImage() {
        return workspaceSplitPane.getBoundingBoxEditor().getBoundingBoxEditorImagePane().getCurrentImage();
    }

    public MenuItem getFileImportAnnotationsItem() {
        return header.getFileImportAnnotationsItem();
    }

    public BoundingBoxCategoryTableView getBoundingBoxCategoryTable() {
        return workspaceSplitPane.getEditorsSplitPane().getBoundingBoxCategoryTable();
    }

    public TextField getBoundingBoxCategoryInputField() {
        return workspaceSplitPane.getEditorsSplitPane().getCategoryNameTextField();
    }

    public ColorPicker getBoundingBoxCategoryColorPicker() {
        return workspaceSplitPane.getEditorsSplitPane().getCategoryColorPicker();
    }

    public ObservableList<BoundingBoxView> getCurrentBoundingBoxes() {
        return workspaceSplitPane.getBoundingBoxEditor().getBoundingBoxEditorImagePane().getCurrentBoundingBoxes();
    }

    public StatusBarView getStatusBar() {
        return statusBar;
    }

    public TextField getCategorySearchField() {
        return workspaceSplitPane.getEditorsSplitPane().getCategorySearchField();
    }

    public List<BoundingBoxData> extractCurrentBoundingBoxData() {
        return getBoundingBoxTree().extractCurrentBoundingBoxData();
    }

    public TextField getTagInputField() {
        return getEditorsSplitPane().getTagInputField();
    }

    public EditorsSplitPaneView getEditorsSplitPane() {
        return workspaceSplitPane.getEditorsSplitPane();
    }

    private void setUpInternalListeners() {
        header.getSeparator().visibleProperty().bind(workspaceSplitPane.visibleProperty());
        header.getViewShowImagesPanelItem().disableProperty().bind(workspaceSplitPane.visibleProperty().not());
        header.getViewMaximizeImagesItem().disableProperty().bind(workspaceSplitPane.visibleProperty().not());
        statusBar.visibleProperty().bind(workspaceSplitPane.visibleProperty());

        header.getViewShowImagesPanelItem().selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                workspaceSplitPane.getItems().add(2, getImageFileExplorer());
                workspaceSplitPane.applySavedDividerPositions();
            } else {
                workspaceSplitPane.saveDividerPositions();
                workspaceSplitPane.getItems().remove(getImageFileExplorer());
            }
        });

        header.getViewMaximizeImagesItem().selectedProperty().addListener((observable, oldValue, newValue) -> {
            getBoundingBoxEditorImagePane().setMaximizeImageView(newValue);
            getBoundingBoxEditorImagePane().resetImageViewSize();
        });
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
