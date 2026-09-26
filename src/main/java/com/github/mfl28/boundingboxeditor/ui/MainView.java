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

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.ui.settings.EditorSettingsView;
import com.github.mfl28.boundingboxeditor.ui.settings.InferenceSettingsView;
import com.github.mfl28.boundingboxeditor.ui.settings.SettingsDialogView;
import com.github.mfl28.boundingboxeditor.ui.settings.UISettingsView;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

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

    private static final String MAIN_VIEW_ID = "main-view";


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
     * Undoes the last step of the shape that is being drawn, e.g. removes the last vertex of a polygon. A shape of
     * which nothing is left is removed.
     */
    public void undoBoundingShapeDrawingStep() {
        getEditorImagePane().undoBoundingShapeDrawingStep().ifPresent(shape -> {
            final BoundingShapeTreeItem treeItem = shape.getViewData().getTreeItem();

            if(treeItem != null) {
                workspaceSplitPane.removeBoundingShapeWithTreeItemRecursively(treeItem);
            } else {
                getEditorImagePane().removeAllFromCurrentBoundingShapes(List.of(shape));
            }
        });
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

    public void saveCurrentSelectedBoundingShapeAsImage() {
        final TreeItem<Object> selectedTreeItem = getObjectTree().getSelectionModel().getSelectedItem();

        if(selectedTreeItem instanceof BoundingShapeTreeItem boundingShapeTreeItem &&
                boundingShapeTreeItem.getValue() instanceof BoundingShapeViewable boundingShapeViewable) {
            workspaceSplitPane.initiateSaveAsImage(boundingShapeViewable, boundingShapeTreeItem.getId());
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

    public MenuItem getUndoMenuItem() {
        return header.getUndoMenuItem();
    }

    public MenuItem getRedoMenuItem() {
        return header.getRedoMenuItem();
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


    public Optional<Window> getSettingsWindow() {
        return Window.getWindows()
                .stream()
                .filter(window -> window instanceof Stage stage
                        && stage.getTitle().equals(SettingsDialogView.SETTINGS_TITLE))
                .findFirst();
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

    public enum FileChooserType {SAVE, OPEN}
}
