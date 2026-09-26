/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
import java.util.Optional;

/**
 * The {@link DialogService} showing the application's JavaFX dialogs.
 */
public class JavaFxDialogService implements DialogService {
    private static final int INFO_DIALOGUE_MIN_WIDTH = 600;
    private static final String ANNOTATION_IMPORT_ERROR_REPORT_TITLE = "Annotation Import Error Report";
    private static final String ANNOTATION_SAVING_ERROR_REPORT_TITLE = "Annotation Saving Error Report";
    private static final String STYLESHEET_PATH = "/stylesheets/css/styles.css";

    /**
     * Applies the application's stylesheet and icon to a dialog.
     *
     * @param dialog the dialog
     */
    public static void applyDialogStyle(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets()
                .add(JavaFxDialogService.class.getResource(STYLESHEET_PATH).toExternalForm());
        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(MainView.APPLICATION_ICON);
    }

    @Override
    public void displayErrorAlert(String title, String content, Window owner) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        setupAndShowDialog(alert, title, content, owner);
    }

    @Override
    public ButtonBar.ButtonData displayYesNoCancelDialogAndGetResult(String title, String content, Window owner) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                content, new ButtonType("Yes", ButtonBar.ButtonData.YES),
                new ButtonType("No", ButtonBar.ButtonData.NO),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
        setupAndShowDialog(dialog, title, content, owner);
        return dialog.getResult().getButtonData();
    }

    @Override
    public ButtonBar.ButtonData displayYesNoDialogAndGetResult(String title, String content, Window owner) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                content, new ButtonType("Yes", ButtonBar.ButtonData.YES),
                new ButtonType("No", ButtonBar.ButtonData.NO));
        setupAndShowDialog(dialog, title, content, owner);
        return dialog.getResult().getButtonData();
    }

    @Override
    public File displayDirectoryChooserAndGetChoice(String title, Stage stage, File initialDirectory) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);

        if(initialDirectory != null && initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        return directoryChooser.showDialog(stage);
    }

    @Override
    public File displayFileChooserAndGetChoice(String title, Window window, File initialDirectory,
                                               String initialFileName, FileChooser.ExtensionFilter extensionFilter,
                                               MainView.FileChooserType type) {
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

        if(type.equals(MainView.FileChooserType.SAVE)) {
            result = fileChooser.showSaveDialog(window);
        } else {
            result = fileChooser.showOpenDialog(window);
        }

        return result;
    }

    @Override
    public void displayIOResultErrorInfoAlert(IOResult ioResult, Window owner) {
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
            case ANNOTATION_SAVING -> displayInfoAlert(ANNOTATION_SAVING_ERROR_REPORT_TITLE,
                    "There were errors while saving annotations.",
                    numErrorEntries + " image-annotation file"
                            + (numErrorEntries > 1 ? "s" : "") + " could not be saved.",
                    errorTable, owner);
            case IMAGE_METADATA_LOADING ->
                    displayImageMetadataLoadingInfoAlert(ioResult, errorTable, numErrorEntries, owner);
            case BOUNDING_BOX_PREDICTION -> displayInfoAlert("Bounding Box Prediction Error Report",
                    "There were errors while performing the prediction",
                    "Bounding box predictions for " + numErrorEntries + " image file" +
                            (numErrorEntries > 1 ? "s" : "") + " could not be loaded.",
                    errorTable, owner);
            case MODEL_NAME_FETCHING -> displayInfoAlert("Model Fetching Error Report",
                    "There were errors while fetching model names from the server",
                    null, errorTable, owner);
        }
    }

    @Override
    public <T> Optional<T> displayChoiceDialogAndGetResult(T defaultChoice, Collection<T> choices, String title,
                                                           String header, String content, Window owner) {
        ChoiceDialog<T> choiceDialog = new ChoiceDialog<>(defaultChoice, choices);
        choiceDialog.setTitle(title);
        choiceDialog.setHeaderText(header);
        choiceDialog.setContentText(content);
        applyDialogStyle(choiceDialog);
        choiceDialog.initOwner(owner);
        return choiceDialog.showAndWait();
    }

    @Override
    public void displayExceptionDialog(Throwable throwable, Window owner) {
        ExceptionDialog exceptionDialog = new ExceptionDialog(throwable);
        applyDialogStyle(exceptionDialog);
        exceptionDialog.initOwner(owner);
        exceptionDialog.showAndWait();
    }

    @Override
    public void displayTextInfoDialog(String title, String header, String content, Window owner) {
        displayInfoAlert(title, header, content, null, owner);
    }

    @Override
    public ServiceProgressDialog createServiceProgressDialog(Service<? extends IOResult> service, String title,
                                                             String header) {
        final ServiceProgressDialog progressDialog = new ServiceProgressDialog(service);
        progressDialog.setTitle(title);
        progressDialog.setHeaderText(header);

        return progressDialog;
    }

    private static void displayImageMetadataLoadingInfoAlert(IOResult ioResult, TableView<IOErrorInfoEntry> errorTable,
                                                             long numErrorEntries, Window owner) {
        if(ioResult.getNrSuccessfullyProcessedItems() == 0) {
            displayInfoAlert("Image loading error report", "There were errors while loading images.",
                    "The folder does not contain any valid image files.", errorTable, owner);
        } else {
            displayInfoAlert("Image loading error report", "There were errors while loading images.",
                    numErrorEntries + " image file" + (numErrorEntries > 1 ? "s" : "") +
                            " could not be loaded.", errorTable, owner);
        }
    }

    private static void displayAnnotationImportInfoAlert(IOResult ioResult, TableView<IOErrorInfoEntry> errorTable,
                                                         long numErrorEntries, Window owner) {
        if(ioResult.getNrSuccessfullyProcessedItems() == 0) {
            displayInfoAlert(ANNOTATION_IMPORT_ERROR_REPORT_TITLE,
                    "There were errors while loading annotations.",
                    "The source does not contain any valid annotations.", errorTable, owner);
        } else {
            displayInfoAlert(ANNOTATION_IMPORT_ERROR_REPORT_TITLE,
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
        applyDialogStyle(dialog);
        dialog.initOwner(owner);
        dialog.showAndWait();
    }

    private static void displayInfoAlert(String title, String header, String content, Node additionalInfoNode,
                                         Window owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setPrefWidth(INFO_DIALOGUE_MIN_WIDTH);
        applyDialogStyle(alert);
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
}
