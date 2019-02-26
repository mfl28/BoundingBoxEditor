import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;

/**
 * Responsible for event-handling between the model and view classes.
 */
//TODO: should probably extend EventHandler
public class Controller {
    private static final String PROGRAM_NAME_EXTENSION = "Bounding Box Editor";
    private static final String PROGRAM_NAME_EXTENSION_SEPARATOR = " - ";
    private static final String OPEN_FOLDER_ERROR_TITLE = "Error while opening image folder";
    private static final String OPEN_FOLDER_ERROR_HEADER = "The selected folder is not a valid image folder.";
    private static final String SAVE_BOUNDING_BOX_DATA_TITLE = "Save bounding box data";
    private static final String CSV_FILE_DESCRIPTION = "CSV file";
    private static final String TXT_FILE_DESCRIPTION = "TXT file";
    private static final String CSV_EXTENSION = "*.csv";
    private static final String TXT_EXTENSION = "*.txt";
    private static final String SAVE_BOUNDING_BOX_DATA_ERROR_TITLE = "Error while saving bounding box data";
    private static final String SAVE_BOUNDING_BOX_DATA_ERROR_HEADER = "Could not save bounding box data into the specified file.";

    private final Stage stage;
    private final View view = new View(this);
    private final Model model = new Model();

    /**
     * Used to generate random colors for the bounding-box categories.
     */
    private final Random random = new Random();

    Controller(final Stage stage) {
        this.stage = stage;
        stage.setTitle(PROGRAM_NAME_EXTENSION);
        setModelListeners();

    }

    public void onRegisterOpenFolderAction(ActionEvent event) {

        final File selectedDirectory = view.getFileFromDirectoryChooser();

        if (selectedDirectory != null) {
            // clear current selection rectangles when new folder is loaded
            view.getSelectionRectangleList().clear();
            final Path inputPath = Paths.get(selectedDirectory.getPath());

            try {
                model.setImageFileListFromPath(inputPath);
            } catch (Exception e) {
                view.displayErrorAlert(OPEN_FOLDER_ERROR_TITLE, OPEN_FOLDER_ERROR_HEADER, e.getMessage());
                return;
            }

            view.getPreviousButton().disableProperty().bind(model.hasPreviousFileBinding().not());
            view.getNextButton().disableProperty().bind(model.hasNextFileBinding().not());
            view.getNavBar().setVisible(true);

            view.setImageView(model.getCurrentImage());
            stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION_SEPARATOR + PROGRAM_NAME_EXTENSION);
            view.getBoundingBoxItemTableView().setItems(model.getBoundingBoxCategories());
            view.getBoundingBoxItemTableView().getSelectionModel().selectFirst();
        }
    }

    public void onRegisterSaveAction(ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(SAVE_BOUNDING_BOX_DATA_TITLE);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(CSV_FILE_DESCRIPTION, CSV_EXTENSION),
                new FileChooser.ExtensionFilter(TXT_FILE_DESCRIPTION, TXT_EXTENSION)
        );

        final File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            if (view.getSelectionRectangle().isVisible()) {
                saveCurrentBoundingBox();
            }

            try {
                model.writeBoundingBoxDataToFile(saveFile);
            } catch (IOException e) {
                // Message text should wrap around.
                view.displayErrorAlert(SAVE_BOUNDING_BOX_DATA_ERROR_TITLE, SAVE_BOUNDING_BOX_DATA_ERROR_HEADER,
                        e.getMessage());
            }
        }
    }

    public void onRegisterNextAction(ActionEvent event) {
        // cancel image loading when clicking next
        // button while the image has not been loaded completely
        view.getCurrentImage().cancel();
        if (view.getSelectionRectangle().isVisible()) {
            saveCurrentBoundingBox();
        }
        view.getBoundingBoxTreeViewRoot().getChildren().clear();
        view.getSelectionRectangleList().clear();
        model.incrementFileIndex();
    }

    public void onRegisterPreviousAction(ActionEvent event) {
        // cancel image loading when clicking previous
        // button while the image has not been loaded completely
        view.getCurrentImage().cancel();
        if (view.getSelectionRectangle().isVisible()) {
            saveCurrentBoundingBox();
        }
        view.getBoundingBoxTreeViewRoot().getChildren().clear();
        view.getSelectionRectangleList().clear();
        model.decrementFileIndex();
    }

    public void onRegisterFitWindowAction(ActionEvent event) {
        // TO BE IMPLEMENTED
    }

    public void onMousePressed(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY) &&
                !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            final Point2D parentCoordinates = view.getImageView().localToParent(event.getX(), event.getY());
            view.getMousePressed().setFromMouseEvent(event);

            final SelectionRectangle rectangle = view.getSelectionRectangle();
            rectangle.setXYWH(parentCoordinates.getX(), parentCoordinates.getY(), 0, 0);
            rectangle.setVisible(true);
        }
    }

    public void onMouseDragged(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY) &&
                !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            final ImageView imageView = view.getImageView();
            final Point2D clampedEventXY = Utils.clampWithinBounds(event, imageView.getBoundsInLocal());
            final DragAnchor mousePressed = view.getMousePressed();

            final Point2D parentCoordinates = imageView.localToParent(Math.min(clampedEventXY.getX(),
                    mousePressed.getX()), Math.min(clampedEventXY.getY(), mousePressed.getY()));

            view.getSelectionRectangle().setXYWH(parentCoordinates.getX(),
                    parentCoordinates.getY(),
                    Math.abs(clampedEventXY.getX() - mousePressed.getX()),
                    Math.abs(clampedEventXY.getY() - mousePressed.getY()));
        }
    }

    public void onMouseReleased(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY) &&
                !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            SelectionRectangle rectangle = view.getSelectionRectangle();
            BoundingBoxCategory selectedBoundingBox = view.getBoundingBoxItemTableView().getSelectionModel().getSelectedItem();

            SelectionRectangle newRectangle = new SelectionRectangle(selectedBoundingBox);
            newRectangle.setXYWH(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
            newRectangle.setVisible(true);
            newRectangle.setStroke(rectangle.getStroke());
            newRectangle.confineTo(view.getImageView().boundsInParentProperty());

            view.getSelectionRectangleList().add(newRectangle);
            rectangle.setVisible(false);
        }
    }

    public void onRegisterAddBoundingBoxItemAction(ActionEvent event) {
        final String boundingBoxItemName = view.getNameInput().getText();

        if (boundingBoxItemName.isEmpty()) {
            view.displayErrorAlert("Category Input Error", null, "Please provide a category name.");
            return;
        }

        if (model.getBoundingBoxCategoryNames().contains(boundingBoxItemName)) {
            view.displayErrorAlert("Category Input Error", null, "The category \"" + boundingBoxItemName + "\" already exists.");
            return;
        }

        final Color boundingBoxItemColor = view.getBoundingBoxColorPicker().getValue();

        model.getBoundingBoxCategories().add(new BoundingBoxCategory(boundingBoxItemName, boundingBoxItemColor));
        model.getBoundingBoxCategoryNames().add(boundingBoxItemName);
        view.getNameInput().clear();

        final var selectionModel = view.getBoundingBoxItemTableView().getSelectionModel();

        // auto select the created category
        selectionModel.selectLast();
        view.getBoundingBoxColorPicker().setValue(Utils.createRandomColor(random));
        // auto scroll to the created category (if it would otherwise be outside the viewport)
        view.getBoundingBoxItemTableView().scrollTo(selectionModel.getSelectedIndex());
    }

    public void handleSceneKeyPress(KeyEvent event) {
        KeyCode keyCode = event.getCode();

        if (keyCode.equals(KeyCode.D)) {
            if (model.getImageFileList() != null && model.isHasNextFile()) {
                onRegisterNextAction(new ActionEvent());
            }
        } else if (keyCode.equals(KeyCode.A)) {
            if (model.getImageFileList() != null && model.isHasPreviousFile()) {
                onRegisterPreviousAction(new ActionEvent());
            }
        } else if (event.isControlDown() && keyCode.equals(KeyCode.F)) {
            view.getSearchField().requestFocus();
        }
    }

    public void onRegisterExitAction(ActionEvent event) {
        Platform.exit();
    }

    public View getView() {
        return view;
    }

    private void setModelListeners() {
        model.fileIndexProperty().addListener((value, oldValue, newValue) -> {
            view.setImageView(model.getCurrentImage());
            stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION);
        });

        // Synchronizes name hashset with bounding box category list when items are deleted.
        model.getBoundingBoxCategories().addListener((ListChangeListener<BoundingBoxCategory>) c -> {
            HashSet<String> boundingBoxCategoryNames = model.getBoundingBoxCategoryNames();
            while (c.next()) {
                c.getRemoved().forEach(boundingBoxCategory ->
                        boundingBoxCategoryNames.remove(boundingBoxCategory.getName()));
            }
        });

    }

    private void saveCurrentBoundingBox() {
        final String currentFilename = Utils.filenameFromUrl(model.getCurrentImageFilePath());
        model.getBoundingBoxData().put(currentFilename,
                view.getSelectionRectangle().getScaledLocalCoordinatesInSiblingImage(view.getImageView()));
    }
}
