package boundingboxeditor.ui;

import boundingboxeditor.controller.Controller;
import boundingboxeditor.model.io.BoundingShapeData;
import boundingboxeditor.model.io.IOResult;
import boundingboxeditor.model.io.ImageAnnotation;
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
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The main view-component of the application (MVC architecture). Contains all other UI-elements.
 *
 * @see BorderPane
 * @see View
 * @see Controller
 * @see boundingboxeditor.model.Model Model
 */
public class MainView extends BorderPane implements View {
    public static final String APPLICATION_ICON_PATH = "/icons/app_icon.png";
    public static final Image APPLICATION_ICON = new Image(MainView.class.getResource(APPLICATION_ICON_PATH).toExternalForm());

    private static final int INFO_DIALOGUE_MIN_WIDTH = 600;
    private static final String MAIN_VIEW_ID = "main-view";
    private static final String ANNOTATION_IMPORT_ERROR_REPORT_TITLE = "Annotation import error report";
    private static final String ANNOTATION_SAVING_ERROR_REPORT_TITLE = "Annotation saving error report";
    private static final String STYLESHEET_PATH = "/stylesheets/css/styles.css";

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
        alert.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
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
        dialog.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
        dialog.showAndWait();

        return dialog.getResult().getButtonData();
    }

    /**
     * Displays a a dialog with 'Yes', 'No' buttons and returns the chosen option.
     *
     * @param title   The title of the dialog window
     * @param content The content text of the dialog window
     * @return {@link ButtonBar.ButtonData}.YES/NO
     */
    public static ButtonBar.ButtonData displayYesNoDialogAndGetResult(String title, String content) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                content, new ButtonType("Yes", ButtonBar.ButtonData.YES),
                new ButtonType("No", ButtonBar.ButtonData.NO));
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(content);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
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
    public static File displayDirectoryChooserAndGetChoice(String title, Stage stage, File initialDirectory) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);

        if(initialDirectory != null && initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

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
        TableColumn<IOResult.ErrorInfoEntry, String> errorDescriptionColumn = new TableColumn<>("Error");

        errorTable.getColumns().add(fileNameColumn);
        errorTable.getColumns().add(errorDescriptionColumn);
        errorTable.setEditable(false);
        errorTable.setMaxWidth(Double.MAX_VALUE);
        errorTable.setMaxHeight(Double.MAX_VALUE);

        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));

        errorDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("errorDescription"));
        errorDescriptionColumn.setSortable(false);
        errorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        errorTable.setItems(FXCollections.observableArrayList(ioResult.getErrorTableEntries()));
        errorTable.getSortOrder().add(fileNameColumn);
        errorTable.sort();

        long numErrorEntries = ioResult.getErrorTableEntries().stream()
                .map(IOResult.ErrorInfoEntry::getFileName)
                .distinct()
                .count();

        if(ioResult.getOperationType().equals(IOResult.OperationType.ANNOTATION_IMPORT)) {
            if(ioResult.getNrSuccessfullyProcessedItems() == 0) {
                MainView.displayInfoAlert(ANNOTATION_IMPORT_ERROR_REPORT_TITLE, "There were errors while loading annotations.",
                        "The folder does not contain any valid annotation files.", errorTable);
            } else {
                MainView.displayInfoAlert(ANNOTATION_IMPORT_ERROR_REPORT_TITLE, "There were errors while loading annotations.",
                        "Some bounding boxes could not be loaded from " + numErrorEntries + " image-annotation file"
                                + (numErrorEntries > 1 ? "s" : "") + ".", errorTable);
            }

        } else if(ioResult.getOperationType().equals(IOResult.OperationType.ANNOTATION_SAVING)) {
            MainView.displayInfoAlert(ANNOTATION_SAVING_ERROR_REPORT_TITLE, "There were errors while saving annotations.",
                    numErrorEntries + " image-annotation file"
                            + (numErrorEntries > 1 ? "s" : "") + " could not be saved.", errorTable);
        } else if(ioResult.getOperationType().equals(IOResult.OperationType.IMAGE_METADATA_LOADING)) {
            if(ioResult.getNrSuccessfullyProcessedItems() == 0) {
                MainView.displayInfoAlert("Image loading error report", "There were errors while loading images.",
                        "The folder does not contain any valid image files.", errorTable);
            } else {
                MainView.displayInfoAlert("Image loading error report", "There were errors while loading images.",
                        numErrorEntries + " image file" + (numErrorEntries > 1 ? "s" : "") + " could not be loaded.", errorTable);
            }
        }
    }

    /**
     * Displays a dialog showing the progress of a {@link Service} object's progress-property.
     *
     * @param service the {@link Service} whose progress will be shown
     * @param title   the title of the dialog-window
     * @param header  the header-text of the dialog-window
     */
    public static void displayServiceProgressDialog(Service<IOResult> service, String title, String header) {
        final ProgressDialog progressDialog = new ProgressDialog(service);
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(header);
        progressDialog.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) progressDialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
    }

    public static <T> Optional<T> displayChoiceDialogAndGetResult(T defaultChoice, Collection<T> choices,
                                                                  String title, String header, String content) {
        ChoiceDialog<T> choiceDialog = new ChoiceDialog<>(defaultChoice, choices);
        choiceDialog.setTitle(title);
        choiceDialog.setHeaderText(header);
        choiceDialog.setContentText(content);
        choiceDialog.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) choiceDialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);

        return choiceDialog.showAndWait();
    }

    public static void displayExceptionDialog(Throwable throwable) {
        ExceptionDialog exceptionDialog = new ExceptionDialog(throwable);
        exceptionDialog.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) exceptionDialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
        exceptionDialog.showAndWait();
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

    public void setWorkspaceVisible(boolean value) {
        workspaceSplitPane.setVisible(value);
    }

    /**
     * Updates the displayed image in the main image-pane from a provided image-{@link File}.
     *
     * @param imageFile the file of the new image
     * @param width     the width of image corresponding to the file
     * @param height    the height of the image corresponding to the file
     */
    public void updateImageFromFile(final File imageFile, double width, double height) {
        workspaceSplitPane.getEditor().getEditorImagePane().updateImageFromFile(imageFile, width, height);
    }

    /**
     * Loads bounding shape objects from data in a provided {@link ImageAnnotation} object into
     * the view-component. This results in displaying of the bounding shape objects on top of the currently loaded
     * image and showing of corresponding tree-items in the {@link ObjectTreeView} UI-element. This method
     * is called every time a previously stored {@link ImageAnnotation} object needs to be made visible to the user.
     *
     * @param annotation the image-annotation to load from
     */
    public void loadBoundingShapeViewsFromAnnotation(ImageAnnotation annotation) {
        List<BoundingShapeViewable> boundingShapes = getObjectTree()
                .extractBoundingShapesAndBuildTreeFromAnnotation(annotation);

        ToggleGroup boundingShapeSelectionGroup = getEditorImagePane().getBoundingShapeSelectionGroup();

        boundingShapes.forEach(viewable -> {
            viewable.autoScaleWithBoundsAndInitialize(getEditorImageView().boundsInParentProperty());
            viewable.getViewData().setToggleGroup(boundingShapeSelectionGroup);
        });

        // Temporarily switch off automatic adding of boundingShapes to the explorer (those are already imported)
        workspaceSplitPane.setTreeUpdateEnabled(false);
        getEditorImagePane().setAllCurrentBoundingShapes(boundingShapes);
        workspaceSplitPane.setTreeUpdateEnabled(true);
        // Expand all tree-items in the object tree-view.
        workspaceSplitPane.getEditorsSplitPane().getObjectTree().expandAllTreeItems();
        // Immediately after loading, no object should be selected.
        boundingShapeSelectionGroup.selectToggle(null);
    }

    /**
     * If a tree-item is currently selected, removes it and all of its child-tree-items. For any removed tree-items,
     * the associated view-objects are removed as well.
     */
    public void removeSelectedTreeItemAndChildren() {
        final TreeItem<Object> selectedTreeItem = getObjectTree().getSelectionModel().getSelectedItem();

        if(selectedTreeItem != null) {
            workspaceSplitPane.removeBoundingShapeWithTreeItemRecursively(selectedTreeItem);
        }
    }

    /**
     * If a {@link BoundingPolygonTreeItem} is currently selected, removes its vertices with state 'editing'.
     */
    public void removeEditingVerticesWhenPolygonViewSelected() {
        final TreeItem<Object> selectedTreeItem = getObjectTree().getSelectionModel().getSelectedItem();

        if(selectedTreeItem instanceof BoundingPolygonTreeItem) {
            ((BoundingPolygonView) selectedTreeItem.getValue()).removeEditingVertices();
        }
    }

    /**
     * Checks if the {@link EditorImagePaneView}-member currently contains bounding shapes.
     *
     * @return true if there exist bounding shapes, false otherwise.
     */
    public boolean containsBoundingShapeViews() {
        return !getCurrentBoundingShapes().isEmpty();
    }

    /**
     * Initiates the category change process for the currently selected bounding shape.
     */
    public void initiateCurrentSelectedBoundingBoxCategoryChange() {
        final TreeItem<Object> selectedTreeItem = getObjectTree().getSelectionModel().getSelectedItem();

        if(selectedTreeItem instanceof BoundingShapeTreeItem) {
            workspaceSplitPane.initiateObjectCategoryChange((BoundingShapeViewable) selectedTreeItem.getValue());
        }
    }

    /* Delegating Getters */

    public ImageFileExplorerView getImageFileExplorer() {
        return workspaceSplitPane.getImageFileExplorer();
    }

    public EditorView getEditor() {
        return workspaceSplitPane.getEditor();
    }

    public ObjectTreeView getObjectTree() {
        return workspaceSplitPane.getEditorsSplitPane().getObjectTree();
    }

    public EditorImagePaneView getEditorImagePane() {
        return workspaceSplitPane.getEditor().getEditorImagePane();
    }

    public ImageView getEditorImageView() {
        return workspaceSplitPane.getEditor().getEditorImagePane().getImageView();
    }

    public Button getPreviousImageNavigationButton() {
        return workspaceSplitPane.getEditor().getEditorToolBar().getPreviousButton();
    }

    public Button getNextImageNavigationButton() {
        return workspaceSplitPane.getEditor().getEditorToolBar().getNextButton();
    }

    public ImageFileListView getImageFileListView() {
        return workspaceSplitPane.getImageFileExplorer().getImageFileListView();
    }

    public TextField getImageFileSearchField() {
        return workspaceSplitPane.getImageFileExplorer().getImageFileSearchField();
    }

    public Image getCurrentImage() {
        return workspaceSplitPane.getEditor().getEditorImagePane().getCurrentImage();
    }

    public MenuItem getFileImportAnnotationsItem() {
        return header.getFileImportAnnotationsItem();
    }

    public ObjectCategoryTableView getObjectCategoryTable() {
        return workspaceSplitPane.getEditorsSplitPane().getObjectCategoryTable();
    }

    public TextField getObjectCategoryInputField() {
        return workspaceSplitPane.getEditorsSplitPane().getCategoryNameTextField();
    }

    public ColorPicker getObjectCategoryColorPicker() {
        return workspaceSplitPane.getEditorsSplitPane().getCategoryColorPicker();
    }

    public ObservableList<BoundingShapeViewable> getCurrentBoundingShapes() {
        return workspaceSplitPane.getEditor().getEditorImagePane().getCurrentBoundingShapes();
    }

    public StatusBarView getStatusBar() {
        return statusBar;
    }

    public TextField getCategorySearchField() {
        return workspaceSplitPane.getEditorsSplitPane().getCategorySearchField();
    }

    public List<BoundingShapeData> extractCurrentBoundingShapeData() {
        return getObjectTree().extractCurrentBoundingShapeData();
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
            if(Boolean.TRUE.equals(newValue)) {
                workspaceSplitPane.getItems().add(2, getImageFileExplorer());
                workspaceSplitPane.applySavedDividerPositions();
            } else {
                workspaceSplitPane.saveDividerPositions();
                workspaceSplitPane.getItems().remove(getImageFileExplorer());
            }
        });

        header.getViewMaximizeImagesItem().selectedProperty().addListener((observable, oldValue, newValue) -> {
            getEditorImagePane().setMaximizeImageView(newValue);
            getEditorImagePane().resetImageViewSize();
        });
    }

    private static void displayInfoAlert(String title, String header, String content, Node additionalInfoNode) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(INFO_DIALOGUE_MIN_WIDTH);
        alert.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);

        GridPane.setVgrow(additionalInfoNode, Priority.ALWAYS);
        GridPane.setHgrow(additionalInfoNode, Priority.ALWAYS);

        GridPane expandableContent = new GridPane();
        expandableContent.setMaxWidth(Double.MAX_VALUE);
        expandableContent.add(additionalInfoNode, 0, 0);

        alert.getDialogPane().setExpandableContent(expandableContent);
        alert.showAndWait();
    }
}
