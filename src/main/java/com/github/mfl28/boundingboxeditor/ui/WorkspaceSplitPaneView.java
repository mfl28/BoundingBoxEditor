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
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.util.Callback;
import javafx.util.Duration;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * A UI-element representing the main workspace for the user to interact with or navigate through images
 * as well as for creating/editing/deleting bounding-shapes.
 *
 * @see SplitPane
 * @see View
 */
class WorkspaceSplitPaneView extends SplitPane implements View {
    private static final double DEFAULT_FIRST_DIVIDER_RATIO = 0.0;
    private static final double DEFAULT_SECOND_DIVIDER_RATIO = 1.0;
    private static final String WORK_SPACE_ID = "work-space";
    private static final String CHANGE_CATEGORY_DIALOG_TITLE = "Change Category";
    private static final String CATEGORY_CHANGE_DIALOG_CONTENT_TEXT = "New category:";
    private static final SnapshotParameters snapShotParameters = new SnapshotParameters();
    private static final DataFormat dragDataFormat = new DataFormat("box-item");

    static {
        snapShotParameters.setTransform(Transform.scale(1.5, 1.5));
        snapShotParameters.setFill(Color.TRANSPARENT);
    }

    private final EditorsSplitPaneView editorsSplitPane = new EditorsSplitPaneView();
    private final EditorView editor = new EditorView();
    private final ImageFileExplorerView imageFileExplorer = new ImageFileExplorerView();
    private final BooleanProperty showObjectPopover = new SimpleBooleanProperty();
    private boolean treeUpdateEnabled = true;
    private double[] savedDividerPositions = {DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO};

    /**
     * Creates a new UI-element representing the main workspace for the user to interact with or navigate through images
     * as well as for creating/editing/deleting bounding-shapes.
     */
    WorkspaceSplitPaneView() {
        getItems().addAll(editorsSplitPane, editor, imageFileExplorer);

        SplitPane.setResizableWithParent(editorsSplitPane, false);
        SplitPane.setResizableWithParent(imageFileExplorer, false);

        setDividerPositions(DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO);
        setVisible(false);
        setId(WORK_SPACE_ID);

        setUpInternalListeners();
        setObjectTreeCellFactory();
    }

    @Override
    public void connectToController(final Controller controller) {
        editorsSplitPane.connectToController(controller);
        editor.connectToController(controller);
    }

    @Override
    public void reset() {
        setVisible(true);
        editorsSplitPane.reset();
    }

    public BooleanProperty showObjectPopoverProperty() {
        return showObjectPopover;
    }

    /**
     * Initiates a category change for the provided {@link BoundingShapeViewable}.
     *
     * @param boundingShapeViewable the {@link BoundingShapeViewable} associated with the
     *                              bounding shape whose category should be changed
     */
    void initiateObjectCategoryChange(BoundingShapeViewable boundingShapeViewable) {
        ObjectCategory currentCategory = boundingShapeViewable.getViewData().getObjectCategory();

        MainView.displayChoiceDialogAndGetResult(currentCategory,
                        editorsSplitPane.getObjectCategoryTable().getItems(),
                        CHANGE_CATEGORY_DIALOG_TITLE,
                        "Select new category (current: \"" + currentCategory.getName() + "\")",
                        CATEGORY_CHANGE_DIALOG_CONTENT_TEXT,
                        this.getScene().getWindow())
                .ifPresent(newChoice -> {
                    if(!Objects.equals(newChoice, currentCategory)) {
                        // Set new object category.
                        boundingShapeViewable.getViewData().setObjectCategory(newChoice);
                        // Get tree-item that should be moved.
                        TreeItem<Object> treeItemToMove = boundingShapeViewable.getViewData().getTreeItem();
                        // Get target tree-item.
                        TreeItem<Object> targetItem = treeItemToMove.getParent().getParent();
                        // Attach tree-item to new target tree-item.
                        editorsSplitPane.getObjectTree()
                                .reattachTreeItemToNewTargetTreeItem(treeItemToMove, targetItem);
                    }
                });
    }

    void initiateSaveAsImage(BoundingShapeViewable boundingShapeViewable, int itemId) {
        final Rectangle2D relativeOutline =
                boundingShapeViewable.getRelativeOutlineRectangle();

        if(relativeOutline == null
                || relativeOutline.getWidth() < ObjectTreeElementCellFactory.MIN_RELATIVE_SIDE_LENGTH
                || relativeOutline.getHeight() < ObjectTreeElementCellFactory.MIN_RELATIVE_SIDE_LENGTH) {
            MainView.displayErrorAlert("Image Saving Error", "Bounding shape region is too small.",
                    this.getScene().getWindow());
            return;
        }

        final String currentEditorImageUrl = getEditor().getEditorImagePane().getCurrentImageUrl();

        StringBuilder filenameBuilder = new StringBuilder();

        try {
            filenameBuilder.append(FilenameUtils.getBaseName(
                    Paths.get(new URI(currentEditorImageUrl).getPath()).getFileName().toString()));
            filenameBuilder.append("_");
        } catch (URISyntaxException ignored) {
        }

        filenameBuilder.append(boundingShapeViewable.getViewData().getObjectCategory().getName());
        filenameBuilder.append("_");
        filenameBuilder.append(itemId);
        filenameBuilder.append(".png");

        File outputFile = MainView.displayFileChooserAndGetChoice(
                "Save as Image", this.getScene().getWindow(), null,
                filenameBuilder.toString(), new FileChooser.ExtensionFilter("PNG files", "*.png"),
                MainView.FileChooserType.SAVE);

        if (outputFile == null) {
            return;
        }

        final Image currentImage = getEditor().getEditorImagePane().getCurrentImage();
        final ImageView imageView = new ImageView(currentImage);

        clipImageview(boundingShapeViewable, relativeOutline, currentImage, imageView, null);

        final SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imageView.snapshot(parameters, null), null);
        try {
            ImageIO.write(bufferedImage, "png", outputFile);
        } catch (IOException e) {
            MainView.displayErrorAlert("Image Saving Error", "Could not write image.",
                    this.getScene().getWindow());
        }
    }

    void simplifyBoundingPolygon(BoundingPolygonView boundingPolygonView) {
        boundingPolygonView.simplify(editor.getEditorImagePane().simplifyRelativeDistanceToleranceProperty().get());
    }

    /**
     * Returns the editor.
     *
     * @return the editor
     */
    EditorView getEditor() {
        return editor;
    }

    /**
     * Returns the editors-panel UI-element.
     *
     * @return the editors-panel
     */
    EditorsSplitPaneView getEditorsSplitPane() {
        return editorsSplitPane;
    }

    /**
     * Returns the image-file-explorer.
     *
     * @return the image-file-explorer
     */
    ImageFileExplorerView getImageFileExplorer() {
        return imageFileExplorer;
    }

    /**
     * Recursively removes objects starting with the object
     * assigned to the provided tree-item.
     *
     * @param treeItem the root-tree-item for the removal
     */
    void removeBoundingShapeWithTreeItemRecursively(TreeItem<Object> treeItem) {
        editor.getEditorImagePane()
                .removeAllFromCurrentBoundingShapes(ObjectTreeView.getBoundingShapesRecursively(treeItem));

        if(treeItem instanceof ObjectCategoryTreeItem) {
            treeItem.getParent().getChildren().remove(treeItem);
        } else if(treeItem instanceof BoundingShapeTreeItem boundingShapeTreeItem) {
            ObjectCategoryTreeItem parentTreeItem = (ObjectCategoryTreeItem) treeItem.getParent();
            parentTreeItem.detachBoundingShapeTreeItemChild(boundingShapeTreeItem);

            if(parentTreeItem.getChildren().isEmpty()) {
                parentTreeItem.getParent().getChildren().remove(parentTreeItem);
            }
        }

        getEditorsSplitPane().getObjectTree().getSelectionModel().clearSelection();
    }

    /**
     * Saves the current divider-positions to a member-variable.
     */
    void saveDividerPositions() {
        savedDividerPositions = getDividerPositions();
    }

    /**
     * Applies the currently-saved divider-positions.
     */
    void applySavedDividerPositions() {
        setDividerPositions(savedDividerPositions);
    }

    /**
     * Sets the treeUpdateEnabled boolean which indicates if the {@link ObjectTreeView} object
     * should be updated when bounding shape objects are added to the current list of objects
     * in the {@link EditorView} member.
     *
     * @param treeUpdateEnabled true means tree-updates are enabled, false means disabled
     */
    void setTreeUpdateEnabled(boolean treeUpdateEnabled) {
        this.treeUpdateEnabled = treeUpdateEnabled;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        setUpEditorSplitPaneListeners();
        setUpEditorListeners();
    }

    private void setUpEditorSplitPaneListeners() {
        editorsSplitPane.getObjectTree()
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    ObjectTreeView objectTreeView = getEditorsSplitPane().getObjectTree();

                    if(oldValue instanceof ObjectCategoryTreeItem) {
                        for(TreeItem<Object> child : oldValue.getChildren()) {
                            ((BoundingShapeTreeItem) child).setHighlightShape(false);
                        }
                    }

                    if(newValue instanceof ObjectCategoryTreeItem) {
                        getEditor().getEditorImagePane()
                                .getBoundingShapeSelectionGroup().selectToggle(null);

                        for(TreeItem<Object> child : newValue.getChildren()) {
                            ((BoundingShapeTreeItem) child).setHighlightShape(true);
                        }
                    } else if(newValue instanceof BoundingShapeTreeItem) {
                        objectTreeView.keepTreeItemInView(newValue);
                        getEditor().getEditorImagePane()
                                .getBoundingShapeSelectionGroup().selectToggle((Toggle) newValue.getValue());
                    } else if(newValue == null) {
                        getEditor().getEditorImagePane().getBoundingShapeSelectionGroup().selectToggle(null);
                    }
                });
    }

    private void setUpEditorListeners() {
        editor.getEditorImagePane().setOnMousePressed(event -> {
            if(event.getButton().equals(MouseButton.SECONDARY) && !getEditor().getEditorImagePane().isDrawingInProgress()) {
                getEditorsSplitPane().getObjectTree().getSelectionModel().clearSelection();
            }
        });

        editor.getEditorImagePane()
                .getBoundingShapeSelectionGroup()
                .selectedToggleProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if(newValue instanceof BoundingShapeViewable boundingShapeViewable) {
                        getEditorsSplitPane().getObjectTree()
                                .getSelectionModel()
                                .select(boundingShapeViewable.getViewData().getTreeItem());
                    }
                });

        editor.getEditorImagePane().getCurrentBoundingShapes()
                .addListener(new CurrentBoundingShapeListChangeListener());

        editor.getEditorImagePane().selectedCategoryProperty()
                .bind(editorsSplitPane.getObjectCategoryTable().getSelectionModel().selectedItemProperty());

        editor.getEditorToolBar().getShowBoundingShapesButton().setOnAction(event ->
                editorsSplitPane.getObjectTree()
                        .setToggleIconStateForAllTreeItems(
                                true));

        editor.getEditorToolBar().getHideBoundingShapesButton().setOnAction(event ->
                editorsSplitPane.getObjectTree()
                        .setToggleIconStateForAllTreeItems(
                                false));
    }

    private void setObjectTreeCellFactory() {
        editorsSplitPane.getObjectTree().setCellFactory(new ObjectTreeElementCellFactory());
    }

    private class CurrentBoundingShapeListChangeListener implements ListChangeListener<BoundingShapeViewable> {
        @Override
        public void onChanged(Change<? extends BoundingShapeViewable> c) {
            while(c.next()) {
                final ImageFileListView.FileInfo currentSelectedItem = imageFileExplorer.getImageFileListView()
                        .getSelectionModel()
                        .getSelectedItem();

                if(c.wasAdded()) {
                    List<? extends BoundingShapeViewable> addedItems = c.getAddedSubList();

                    editor.getEditorImagePane().addBoundingShapesToSceneGroup(addedItems);

                    if(treeUpdateEnabled) {
                        editorsSplitPane.getObjectTree().addTreeItemsFromBoundingShapeViews(addedItems);
                    }

                    currentSelectedItem.setHasAssignedBoundingShapes(true);
                }

                if(c.wasRemoved()) {
                    editor.getEditorImagePane().removeBoundingShapesFromSceneGroup(c.getRemoved());

                    if(editor.getEditorImagePane().getCurrentBoundingShapes().isEmpty() &&
                            editor.getEditorImagePane().getCurrentImageUrl().equals(
                                    currentSelectedItem.getFileUrl())
                    ) {
                        currentSelectedItem.setHasAssignedBoundingShapes(false);
                    }
                }
            }
        }
    }

    private class ObjectTreeElementCellFactory implements Callback<TreeView<Object>, TreeCell<Object>> {
        private static final double POPOVER_SCREEN_RATIO = 0.25;
        private static final double MIN_RELATIVE_SIDE_LENGTH = 0.0001;
        private final PauseTransition popoverDelayTransition = new PauseTransition(Duration.seconds(0.8));
        private TreeItem<Object> draggedItem;
        private String currentImageUrl;

        @Override
        public TreeCell<Object> call(TreeView<Object> treeView) {
            final ObjectTreeElementCell cell = new ObjectTreeElementCell();

            applyOnDeleteBoundingShapeMenuItemListener(cell);
            applyChangeObjectCategoryMenuItemListener(cell);
            applyOnDragDetectedListener(cell);
            applyOnDragOverListener(cell);
            applyOnDragEnteredListener(cell);
            applyOnDragExitedListener(cell);
            applyOnDragDroppedListener(cell);
            applyOnMouseEnteredListener(cell);
            applyOnMouseExitedListener(cell);
            applyOnHideAllBoundingShapesMenuItemListener(cell);
            applyOnHideOtherBoundingShapesMenuItemListener(cell);
            applyOnShowAllBoundingShapesMenuItemListener(cell);
            applySimplifyPolygonMenuItemListener(cell);
            applySaveAsImageMenuItemListener(cell);

            return cell;
        }

        private void applyOnShowAllBoundingShapesMenuItemListener(ObjectTreeElementCell cell) {
            cell.getShowAllBoundingShapesMenuItem().setOnAction(
                    event -> getEditorsSplitPane().getObjectTree().setToggleIconStateForAllTreeItems(true)
            );
        }

        private void applyOnHideOtherBoundingShapesMenuItemListener(ObjectTreeElementCell cell) {
            cell.getHideOtherBoundingShapesMenuItem().setOnAction(
                    event -> getEditorsSplitPane().getObjectTree()
                            .setToggleIconStateForAllTreeItemsExcept(cell.getTreeItem(), false));
        }

        private void applyOnHideAllBoundingShapesMenuItemListener(ObjectTreeElementCell cell) {
            cell.getHideAllBoundingShapesMenuItem().setOnAction(
                    event -> getEditorsSplitPane().getObjectTree().setToggleIconStateForAllTreeItems(false));
        }

        private void applyOnDeleteBoundingShapeMenuItemListener(ObjectTreeElementCell cell) {
            cell.getDeleteBoundingShapeMenuItem().setOnAction(event -> {
                if(!cell.isEmpty()) {
                    removeBoundingShapeWithTreeItemRecursively(cell.getTreeItem());
                }
            });
        }

        private void applyChangeObjectCategoryMenuItemListener(ObjectTreeElementCell cell) {
            cell.getChangeObjectCategoryMenuItem().setOnAction(event -> {
                if(!cell.isEmpty()) {
                    initiateObjectCategoryChange((BoundingShapeViewable) cell.getItem());
                }
            });
        }

        private void applySaveAsImageMenuItemListener(ObjectTreeElementCell cell) {
            cell.getSaveAsImageMenuItem().setOnAction(event -> {
                if(!cell.isEmpty() &&  !(cell.getTreeItem() instanceof ObjectCategoryTreeItem)) {
                    initiateSaveAsImage((BoundingShapeViewable) cell.getItem(), ((BoundingShapeTreeItem) cell.getTreeItem()).getId());
                }
            });
        }

        private void applySimplifyPolygonMenuItemListener(ObjectTreeElementCell cell) {
            cell.getSimplifyMenuItem().setOnAction(event -> {
                if(!cell.isEmpty() && cell.getItem() instanceof BoundingPolygonView boundingPolygonView) {
                    simplifyBoundingPolygon(boundingPolygonView);
                }
            });
        }

        private void applyOnDragDetectedListener(ObjectTreeElementCell cell) {
            cell.setOnDragDetected(event -> {
                if(cell.isEmpty()) {
                    return;
                }

                draggedItem = cell.getTreeItem();

                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.put(dragDataFormat, "");

                Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
                dragboard.setContent(clipboardContent);
                dragboard.setDragView(cell.getGraphic().snapshot(snapShotParameters, null));
                event.consume();
            });
        }

        private void applyOnDragOverListener(ObjectTreeElementCell cell) {
            cell.setOnDragOver(event -> {
                if(!event.getDragboard().hasContent(dragDataFormat)) {
                    return;
                }

                TreeItem<Object> thisItem = cell.getTreeItem();

                if(draggedItem == null || draggedItem == thisItem || thisItem instanceof ObjectCategoryTreeItem) {
                    return;
                }

                if(thisItem != null &&
                        (thisItem.getChildren().contains(draggedItem) || (draggedItem instanceof ObjectCategoryTreeItem
                                && draggedItem.getChildren().contains(thisItem)))) {
                    return;
                }

                if(thisItem == null && ((draggedItem instanceof ObjectCategoryTreeItem
                        && draggedItem.getParent().equals(cell.getTreeView().getRoot()))
                        || draggedItem.getParent().getParent().equals(cell.getTreeView().getRoot()))) {
                    return;
                }

                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            });
        }

        private void applyOnDragEnteredListener(ObjectTreeElementCell cell) {
            cell.setOnDragEntered(event -> {
                TreeItem<Object> thisItem = cell.getTreeItem();

                if(draggedItem == null || thisItem == null || draggedItem == thisItem
                        || thisItem instanceof ObjectCategoryTreeItem
                        || thisItem.getChildren().contains(draggedItem)
                        || (draggedItem instanceof ObjectCategoryTreeItem
                        && draggedItem.getChildren().contains(thisItem))) {
                    return;
                }

                cell.setDraggedOver(true);
                event.consume();
            });
        }

        private void applyOnDragExitedListener(ObjectTreeElementCell cell) {
            cell.setOnDragExited(event -> {
                cell.setDraggedOver(false);
                event.consume();
            });
        }

        private void applyOnDragDroppedListener(ObjectTreeElementCell cell) {
            cell.setOnDragDropped(event -> {
                if(!event.getDragboard().hasContent(dragDataFormat)) {
                    return;
                }

                TreeItem<Object> targetItem = cell.getTreeItem();

                // Cannot drop on ObjectCategoryTreeItem
                if(targetItem instanceof ObjectCategoryTreeItem) {
                    return;
                }

                ((ObjectTreeView) cell.getTreeView()).reattachTreeItemToNewTargetTreeItem(draggedItem, targetItem);

                event.setDropCompleted(true);
                event.consume();
            });
        }

        private void applyOnMouseEnteredListener(ObjectTreeElementCell cell) {
            cell.setOnMouseEntered(event -> {
                if(!cell.isEmpty()) {
                    cell.setHighlightStatusIncludingChildren(true);
                }

                if(!showObjectPopover.get() || cell.isEmpty() || cell.getTreeItem() instanceof ObjectCategoryTreeItem) {
                    return;
                }

                popoverDelayTransition.setOnFinished(action -> handlePopoverTimerFinished(cell));

                popoverDelayTransition.playFromStart();
            });
        }

        private void handlePopoverTimerFinished(ObjectTreeElementCell cell) {
            final Image currentImage = getEditor().getEditorImagePane().getCurrentImage();
            final String currentEditorImageUrl = getEditor().getEditorImagePane().getCurrentImageUrl();
            final ImageView imageView = cell.getPopOverImageView();

            if(imageView.getImage() == null
                    || !Objects.equals(currentImageUrl, currentEditorImageUrl)) {
                imageView.setImage(currentImage);
                currentImageUrl = currentEditorImageUrl;
            }

            final Rectangle2D relativeOutline =
                    ((BoundingShapeViewable) cell.getItem()).getRelativeOutlineRectangle();

            if(relativeOutline == null
                    || relativeOutline.getWidth() < MIN_RELATIVE_SIDE_LENGTH
                    || relativeOutline.getHeight() < MIN_RELATIVE_SIDE_LENGTH) {
                return;
            }

            clipImageview((BoundingShapeViewable) cell.getItem(), relativeOutline, currentImage, imageView, POPOVER_SCREEN_RATIO);

            cell.getPopOver().show(cell);
        }

        private void applyOnMouseExitedListener(ObjectTreeElementCell cell) {
            cell.setOnMouseExited(event -> {
                if(!cell.isEmpty() && (cell.getContextMenu() == null || !cell.getContextMenu().isShowing())) {
                    cell.setHighlightStatusIncludingChildren(false);
                }

                cell.getPopOver().hide();
                popoverDelayTransition.stop();
            });
        }
    }

    private static void clipImageview(BoundingShapeViewable boundingShapeViewable, Rectangle2D relativeOutline, Image image, ImageView imageView, Double screenRatio) {
        final Rectangle2D outline = new Rectangle2D(relativeOutline.getMinX() * image.getWidth(),
                relativeOutline.getMinY() * image.getHeight(),
                relativeOutline.getWidth() * image.getWidth(),
                relativeOutline.getHeight() * image.getHeight());

        imageView.setViewport(outline);

        double scaleWidth = outline.getWidth();
        double scaleHeight = outline.getHeight();

        if(screenRatio != null) {
            final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            final double sideLength = screenRatio * Math.min(
                    screenBounds.getWidth(), screenBounds.getHeight());

            if(outline.getWidth() > outline.getHeight()) {
                scaleWidth = sideLength;
                scaleHeight = outline.getHeight() * scaleWidth / outline.getWidth();
            } else {
                scaleHeight = sideLength;
                scaleWidth = outline.getWidth() * scaleHeight / outline.getHeight();
            }

            imageView.setFitWidth(scaleWidth);
            imageView.setFitHeight(scaleHeight);
        }

        if(boundingShapeViewable instanceof BoundingPolygonView boundingPolygonView) {
            final List<Double> points = boundingPolygonView
                    .getMinMaxScaledPoints(scaleWidth, scaleHeight);

            final Polygon polygon = new Polygon();
            polygon.getPoints().setAll(points);

            imageView.setClip(polygon);
        }
    }
}
