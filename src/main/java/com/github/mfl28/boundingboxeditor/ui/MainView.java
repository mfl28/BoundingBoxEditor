/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import com.github.mfl28.boundingboxeditor.ui.settings.EditorSettingsView;
import com.github.mfl28.boundingboxeditor.ui.settings.InferenceSettingsView;
import com.github.mfl28.boundingboxeditor.ui.settings.SettingsDialogView;
import com.github.mfl28.boundingboxeditor.ui.settings.UISettingsView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.dialog.ExceptionDialog;

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
 * @see com.github.mfl28.boundingboxeditor.model.Model Model
 */
public class MainView extends BorderPane implements View {
    public static final String APPLICATION_ICON_PATH = "/icons/app_icon.png";
    public static final Image APPLICATION_ICON =
            new Image(MainView.class.getResource(APPLICATION_ICON_PATH).toExternalForm());

    private static final int INFO_DIALOGUE_MIN_WIDTH = 600;
    private static final String MAIN_VIEW_ID = "main-view";
    private static final String ANNOTATION_IMPORT_ERROR_REPORT_TITLE = "Annotation Import Error Report";
    private static final String ANNOTATION_SAVING_ERROR_REPORT_TITLE = "Annotation Saving Error Report";
    private static final String STYLESHEET_PATH = "/stylesheets/css/styles.css";


    private final HeaderView header = new HeaderView();
    private final WorkspaceSplitPaneView workspaceSplitPane = new WorkspaceSplitPaneView();
    private final StatusBarView statusBar = new StatusBarView();
    private final UISettingsConfig uiSettingsConfig = new UISettingsConfig();
    private final EditorSettingsConfig editorSettingsConfig = new EditorSettingsConfig();
    private final InferenceSettingsView inferenceSettingsView = new InferenceSettingsView();
    private final UISettingsView uiSettingsView = new UISettingsView();
    private final EditorSettingsView editorSettingsView = new EditorSettingsView();


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
    public static void displayErrorAlert(String title, String content, Window owner) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setupAndShowDialog(alert, title, content, owner);
    }

    /**
     * Displays a dialog with 'Yes', 'No' and 'Cancel' buttons and returns the chosen option.
     *
     * @param title   The title of the dialog window
     * @param content The content text of the dialog window
     * @return {@link ButtonBar.ButtonData}.YES/NO/CANCEL_CLOSE
     */
    public static ButtonBar.ButtonData displayYesNoCancelDialogAndGetResult(String title, String content,
                                                                            Window owner) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                content, new ButtonType("Yes", ButtonBar.ButtonData.YES),
                new ButtonType("No", ButtonBar.ButtonData.NO),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
        setupAndShowDialog(dialog, title, content, owner);
        return dialog.getResult().getButtonData();
    }

    /**
     * Displays a dialog with 'Yes', 'No' buttons and returns the chosen option.
     *
     * @param title   The title of the dialog window
     * @param content The content text of the dialog window
     * @return {@link ButtonBar.ButtonData}.YES/NO
     */
    public static ButtonBar.ButtonData displayYesNoDialogAndGetResult(String title, String content, Window owner) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                content, new ButtonType("Yes", ButtonBar.ButtonData.YES),
                new ButtonType("No", ButtonBar.ButtonData.NO));
        setupAndShowDialog(dialog, title, content, owner);
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

    /***
     * Displays a file chooser window and returns the chosen directory.
     *
     * @param title The title of the file chooser window
     * @param stage The stage on top of which the window will be shown
     * @param initialDirectory The initial directory
     * @param initialFileName The initial default filename
     * @param extensionFilter The extension filter to apply
     * @return The chosen file, or null if the user closed the window without choosing.
     */
    public static File displayFileChooserAndGetChoice(String title, Stage stage, File initialDirectory,
                                                      String initialFileName,
                                                      FileChooser.ExtensionFilter extensionFilter,
                                                      FileChooserType type) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if(initialFileName != null) {
            fileChooser.setInitialFileName(initialFileName);
        }

        if(initialDirectory != null && initialDirectory.exists()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        if(extensionFilter != null) {
            fileChooser.getExtensionFilters().add(extensionFilter);
            fileChooser.setSelectedExtensionFilter(extensionFilter);
        }

        File result;

        if(type.equals(FileChooserType.SAVE)) {
            result = fileChooser.showSaveDialog(stage);
        } else {
            result = fileChooser.showOpenDialog(stage);
        }

        return result;
    }

    /**
     * Displays a dialog-window that shows information about the result of an
     * IO-operation.
     *
     * @param ioResult the {@link IOResult} object containing the information tom display
     */
    public static void displayIOResultErrorInfoAlert(IOResult ioResult, Window owner) {
        TableView<IOErrorInfoEntry> errorTable = new TableView<>();
        TableColumn<IOErrorInfoEntry, String> errorSourceColumn = new TableColumn<>("Source");
        TableColumn<IOErrorInfoEntry, String> errorDescriptionColumn = new TableColumn<>("Error");

        errorTable.getColumns().add(errorSourceColumn);
        errorTable.getColumns().add(errorDescriptionColumn);
        errorTable.setEditable(false);
        errorTable.setMaxWidth(Double.MAX_VALUE);
        errorTable.setMaxHeight(Double.MAX_VALUE);

        errorSourceColumn.setCellValueFactory(new PropertyValueFactory<>("sourceName"));

        errorDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("errorDescription"));
        errorDescriptionColumn.setSortable(false);
        errorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        errorTable.setItems(FXCollections.observableArrayList(ioResult.getErrorTableEntries()));
        errorTable.getSortOrder().add(errorSourceColumn);
        errorTable.sort();

        long numErrorEntries = ioResult.getErrorTableEntries().stream()
                .map(IOErrorInfoEntry::getSourceName)
                .distinct()
                .count();

        switch(ioResult.getOperationType()) {
            case ANNOTATION_IMPORT -> displayAnnotationImportInfoAlert(ioResult, errorTable, numErrorEntries, owner);
            case ANNOTATION_SAVING -> MainView.displayInfoAlert(ANNOTATION_SAVING_ERROR_REPORT_TITLE,
                    "There were errors while saving annotations.",
                    numErrorEntries + " image-annotation file"
                            + (numErrorEntries > 1 ? "s" : "") + " could not be saved.",
                    errorTable, owner);
            case IMAGE_METADATA_LOADING ->
                    displayImageMetadataLoadingInfoAlert(ioResult, errorTable, numErrorEntries, owner);
            case BOUNDING_BOX_PREDICTION -> MainView.displayInfoAlert("Bounding Box Prediction Error Report",
                    "There were errors while performing the prediction",
                    "Bounding box predictions for " + numErrorEntries + " image file" +
                            (numErrorEntries > 1 ? "s" : "") + " could not be loaded.",
                    errorTable, owner);
            case MODEL_NAME_FETCHING -> MainView.displayInfoAlert("Model Fetching Error Report",
                    "There were errors while fetching model names from the server",
                    null, errorTable, owner);
        }
    }

    /**
     * Displays a dialog with a choice box and returns the user's choice.
     *
     * @param defaultChoice the pre-selected choice
     * @param choices       the available choices
     * @param title         the title of the dialog-window
     * @param header        the header-text of the dialog-window
     * @param content       the content-text of the dialog-window
     * @param <T>           the type of the choices
     * @return an {@link Optional<T>} (possibly) containing the user's choice
     */
    public static <T> Optional<T> displayChoiceDialogAndGetResult(T defaultChoice, Collection<T> choices,
                                                                  String title, String header, String content,
                                                                  Window owner) {
        ChoiceDialog<T> choiceDialog = new ChoiceDialog<>(defaultChoice, choices);
        choiceDialog.setTitle(title);
        choiceDialog.setHeaderText(header);
        choiceDialog.setContentText(content);
        choiceDialog.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) choiceDialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
        choiceDialog.initOwner(owner);
        return choiceDialog.showAndWait();
    }

    /**
     * Shows a dialog for a thrown exception.
     *
     * @param throwable the exception thrown
     */
    public static void displayExceptionDialog(Throwable throwable, Window owner) {
        ExceptionDialog exceptionDialog = new ExceptionDialog(throwable);
        exceptionDialog.getDialogPane().getStylesheets()
                .add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) exceptionDialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
        exceptionDialog.initOwner(owner);
        exceptionDialog.showAndWait();
    }

    public static ServiceProgressDialog createServiceProgressDialog(Service<? extends IOResult> service, String title,
                                                                    String header) {
        final ServiceProgressDialog progressDialog = new ServiceProgressDialog(service);
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(header);

        return progressDialog;
    }

    public static void applyDialogStyle(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets()
                .add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
    }

    @Override
    public void connectToController(final Controller controller) {
        header.connectToController(controller);
        workspaceSplitPane.connectToController(controller);
        inferenceSettingsView.connectToController(controller);

        setOnDragDropped(event -> {
            if(event.getDragboard().hasFiles()) {
                controller.initiateImageFolderLoading(event.getDragboard().getFiles().get(0));
                event.setDropCompleted(true);
            }

            event.consume();
        });
    }

    @Override
    public void reset() {
        workspaceSplitPane.reset();
    }

    /**
     * Gets the visibility of the workspace split pane.
     *
     * @return visibility
     */
    public boolean isWorkspaceVisible() {
        return workspaceSplitPane.isVisible();
    }

    /**
     * Sets the visibility of the workspace split pane.
     *
     * @param value the visibility
     */
    public void setWorkspaceVisible(boolean value) {
        workspaceSplitPane.setVisible(value);
    }

    /**
     * Updates the displayed image in the main image-pane from a provided {@link ImageMetaData}.
     *
     * @param imageMetaData The meta data of the image to show.
     */
    public void updateImageFromMetaData(ImageMetaData imageMetaData) {
        workspaceSplitPane.getEditor().getEditorImagePane().updateImageFromMetaData(imageMetaData);
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
            viewable.autoScaleWithBoundsAndInitialize(getEditorImageView().boundsInParentProperty(),
                    annotation.getImageMetaData().getImageWidth(),
                    annotation.getImageMetaData().getImageHeight());
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

    public void simplifyCurrentSelectedBoundingPolygon() {
        final TreeItem<Object> selectedTreeItem = getObjectTree().getSelectionModel().getSelectedItem();

        if(selectedTreeItem instanceof BoundingShapeTreeItem boundingShapeTreeItem &&
                boundingShapeTreeItem.getValue() instanceof BoundingPolygonView boundingPolygonView) {
            workspaceSplitPane.simplifyBoundingPolygon(boundingPolygonView);
        }
    }

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

    /* Delegating Getters */

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

    public UISettingsConfig getUiSettingsConfig() {
        return uiSettingsConfig;
    }

    public InferenceSettingsView getInferenceSettingsView() {
        return inferenceSettingsView;
    }

    public EditorSettingsView getEditorSettingsView() {
        return editorSettingsView;
    }

    public EditorSettingsConfig getEditorSettingsConfig() {
        return editorSettingsConfig;
    }

    public UISettingsView getUiSettingsView() {
        return uiSettingsView;
    }

    public void displaySettingsDialog(Controller controller, Window owner) {
        final SettingsDialogView settingsDialog = new SettingsDialogView();

        settingsDialog.addCategoryContentPair("Editor", editorSettingsView);
        settingsDialog.addCategoryContentPair("Inference", inferenceSettingsView);
        settingsDialog.addCategoryContentPair("UI", uiSettingsView);

        settingsDialog.connectToController(controller);

        settingsDialog.initOwner(owner);

        settingsDialog.showAndWait();
    }


    public static void displayTextInfoDialog(String title, String header, String content, Window owner) {
        displayInfoAlert(title, header, content, null, owner);
    }

    public Optional<Window> getSettingsWindow() {
        return Window.getWindows()
                .stream()
                .filter(window -> window instanceof Stage stage
                        && stage.getTitle().equals(SettingsDialogView.SETTINGS_TITLE))
                .findFirst();
    }


    private static void displayImageMetadataLoadingInfoAlert(IOResult ioResult, TableView<IOErrorInfoEntry> errorTable,
                                                             long numErrorEntries, Window owner) {
        if(ioResult.getNrSuccessfullyProcessedItems() == 0) {
            MainView.displayInfoAlert("Image loading error report", "There were errors while loading images.",
                    "The folder does not contain any valid image files.", errorTable, owner);
        } else {
            MainView.displayInfoAlert("Image loading error report", "There were errors while loading images.",
                    numErrorEntries + " image file" + (numErrorEntries > 1 ? "s" : "") +
                            " could not be loaded.", errorTable, owner);
        }
    }

    private static void displayAnnotationImportInfoAlert(IOResult ioResult, TableView<IOErrorInfoEntry> errorTable,
                                                         long numErrorEntries, Window owner) {
        if(ioResult.getNrSuccessfullyProcessedItems() == 0) {
            MainView.displayInfoAlert(ANNOTATION_IMPORT_ERROR_REPORT_TITLE,
                    "There were errors while loading annotations.",
                    "The source does not contain any valid annotations.", errorTable, owner);
        } else {
            MainView.displayInfoAlert(ANNOTATION_IMPORT_ERROR_REPORT_TITLE,
                    "There were errors while loading annotations.",
                    "Some bounding boxes could not be loaded from " + numErrorEntries +
                            " image-annotation"
                            + (numErrorEntries > 1 ? "s" : "") + ".", errorTable, owner);
        }
    }

    private static void setupAndShowDialog(Alert dialog, String title, String content, Window owner) {
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(content);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
        dialog.initOwner(owner);
        dialog.showAndWait();
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

        workspaceSplitPane.showObjectPopoverProperty().bind(uiSettingsConfig.showObjectPopoverProperty());
        workspaceSplitPane.getEditor().getEditorImagePane().autoSimplifyPolygonsProperty()
                .bind(editorSettingsConfig.autoSimplifyPolygonsProperty());
        workspaceSplitPane.getEditor().getEditorImagePane().simplifyRelativeDistanceToleranceProperty()
                .bind(editorSettingsConfig.simplifyRelativeDistanceToleranceProperty());

        setOnDragOver(event -> {
            if(event.getDragboard().hasFiles()
                    && event.getDragboard().getFiles().size() == 1
                    && event.getDragboard().getFiles().get(0).isDirectory()) {
                event.acceptTransferModes(TransferMode.LINK);
            }

            event.consume();
        });
    }

    private static void displayInfoAlert(String title, String header, String content, Node additionalInfoNode,
                                         Window owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setPrefWidth(INFO_DIALOGUE_MIN_WIDTH);
        alert.getDialogPane().getStylesheets().add(MainView.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(APPLICATION_ICON);
        alert.initOwner(owner);

        if(additionalInfoNode != null) {
            GridPane.setVgrow(additionalInfoNode, Priority.ALWAYS);
            GridPane.setHgrow(additionalInfoNode, Priority.ALWAYS);

            GridPane expandableContent = new GridPane();
            expandableContent.setMaxWidth(Double.MAX_VALUE);
            expandableContent.add(additionalInfoNode, 0, 0);

            alert.getDialogPane().setExpandableContent(expandableContent);
        }
        alert.showAndWait();
    }

    public enum FileChooserType {SAVE, OPEN}
}
