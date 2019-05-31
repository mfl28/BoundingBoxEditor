package BoundingboxEditor.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;

public class ImageExplorerPanelView extends VBox implements View {
    private static final double SIDE_PANEL_SPACING = 5;
    private static final String IMAGE_EXPLORER_ID = "image-explorer-panel";
    private static final String IMAGE_EXPLORER_SIDE_PANEL_STYLE = "image-explorer-side-panel";
    private static final String IMAGE_EXPLORER_LABEL_TEXT = "Image Explorer";
    private static final String FILE_SEARCH_PROMPT_TEXT = "Search File";
    private static final String FILE_SEARCH_BOX_ID = "file-search-box";

    private final TextField fileSearchField = new TextField();
    private final ImageGalleryView imageGallery = new ImageGalleryView();

    ImageExplorerPanelView() {
        getChildren().addAll(
                new Label(IMAGE_EXPLORER_LABEL_TEXT),
                createFileSearchBox(),
                imageGallery);
        getStyleClass().add(IMAGE_EXPLORER_SIDE_PANEL_STYLE);

        setSpacing(SIDE_PANEL_SPACING);
        setId(IMAGE_EXPLORER_ID);

        setUpInternalListeners();
    }

    public void setImageGalleryItems(ObservableList<File> imageFiles) {
        imageGallery.setItems(imageFiles);
    }

    public ImageGalleryView getImageGallery() {
        return imageGallery;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        fileSearchField.textProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                imageGallery.getItems().stream()
                        .filter(item -> item.getName().startsWith(newValue))
                        .findAny()
                        .ifPresent(item -> {
                            // We have to temporarily set a fixed cell size, otherwise
                            // the scroll-to point cannot be calculated.
                            imageGallery.setFixedCellSize(150);
                            imageGallery.getSelectionModel().select(item);
                            imageGallery.scrollTo(item);
                            // Disable fixed cell-size.
                            imageGallery.setFixedCellSize(0);
                        });
            }
        }));
    }

    private HBox createFileSearchBox() {
        HBox.setHgrow(fileSearchField, Priority.ALWAYS);

        fileSearchField.setPromptText(FILE_SEARCH_PROMPT_TEXT);
        fileSearchField.setFocusTraversable(false);

        Region searchIcon = new Region();
        searchIcon.setId("search-icon");

        Label searchLabel = new Label();
        searchLabel.setGraphic(searchIcon);
        searchLabel.setId("search-icon-label");

        HBox fileSearchBox = new HBox(searchLabel, fileSearchField);
        fileSearchBox.setAlignment(Pos.CENTER);
        fileSearchBox.setSpacing(0);
        fileSearchBox.setId(FILE_SEARCH_BOX_ID);

        return fileSearchBox;
    }
}
