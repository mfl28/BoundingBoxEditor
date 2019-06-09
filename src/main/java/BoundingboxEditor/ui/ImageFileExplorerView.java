package BoundingboxEditor.ui;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;

/**
 * UI-element that contains the controls to view, select and search image-files.
 *
 * @see VBox
 * @see View
 */
public class ImageFileExplorerView extends VBox implements View {
    private static final String IMAGE_FILE_EXPLORER_STYLE = "image-file-explorer";
    private static final String IMAGE_FILE_EXPLORER_TABLE_TEXT = "Image Explorer";
    private static final String IMAGE_FILE_SEARCH_PROMPT_TEXT = "Search File";
    private static final String IMAGE_FILE_SEARCH_BOX_ID = "image-file-search-box";
    private static final String IMAGE_FILE_SEARCH_ICON_LABEL_ID = "search-icon-label";
    private static final String IMAGE_FILE_SEARCH_ICON_ID = "search-icon";

    private final TextField imageFileSearchField = new TextField();
    private final ImageFileListView imageFileListView = new ImageFileListView();

    /**
     * Creates a new image-file-explorer UI-element.
     */
    ImageFileExplorerView() {
        getChildren().addAll(
                new Label(IMAGE_FILE_EXPLORER_TABLE_TEXT),
                createImageFileSearchBox(),
                imageFileListView);

        getStyleClass().add(IMAGE_FILE_EXPLORER_STYLE);

        setUpInternalListeners();
    }

    /**
     * Sets the image-files to display in the {@link ImageFileListView} member.
     *
     * @param imageFiles the list of image-files
     */
    public void setImageFiles(ObservableList<File> imageFiles) {
        imageFileListView.setItems(imageFiles);
    }

    /**
     * Returns the {@link ImageFileListView} member.
     *
     * @return the {@link ImageFileListView}
     */
    public ImageFileListView getImageFileListView() {
        return imageFileListView;
    }

    /**
     * Returns the image-file search {@link TextField} member.
     *
     * @return the image-file search-field
     */
    TextField getImageFileSearchField() {
        return imageFileSearchField;
    }

    private HBox createImageFileSearchBox() {
        HBox.setHgrow(imageFileSearchField, Priority.ALWAYS);

        imageFileSearchField.setPromptText(IMAGE_FILE_SEARCH_PROMPT_TEXT);
        imageFileSearchField.setFocusTraversable(false);

        Region searchIcon = new Region();
        searchIcon.setId(IMAGE_FILE_SEARCH_ICON_ID);

        Label searchLabel = new Label();
        searchLabel.setGraphic(searchIcon);
        searchLabel.setId(IMAGE_FILE_SEARCH_ICON_LABEL_ID);

        HBox imageFileSearchBox = new HBox(searchLabel, imageFileSearchField);
        imageFileSearchBox.setId(IMAGE_FILE_SEARCH_BOX_ID);

        return imageFileSearchBox;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        imageFileSearchField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                imageFileListView.getItems().stream()
                        .filter(item -> item.getName().startsWith(newValue))
                        .findAny()
                        .ifPresent(item -> {
                            // We have to temporarily set a fixed cell size, otherwise
                            // the scroll-to point will not be calculated correctly.
                            imageFileListView.setFixedCellSize(150);
                            imageFileListView.getSelectionModel().select(item);
                            imageFileListView.scrollTo(item);
                            // Disable fixed cell-size.
                            imageFileListView.setFixedCellSize(0);
                        });
            }
        }));

        imageFileSearchField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue) {
                imageFileSearchField.setText(null);
            }
        });
    }
}
