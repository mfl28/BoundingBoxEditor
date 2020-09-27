package boundingboxeditor.ui;

import boundingboxeditor.controller.Controller;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

/**
 * The UI-element serving as a container for UI-elements at the top of
 * a {@link MainView} object.
 */
class HeaderView extends VBox implements View {
    private static final String HEADER_STYLE = "header-view";

    private final MenuBarView menuBar = new MenuBarView();
    private final Separator separator = new Separator();

    /**
     * Creates a new header-view object.
     */
    HeaderView() {
        getChildren().addAll(menuBar, separator);
        getStyleClass().add(HEADER_STYLE);
    }

    @Override
    public void connectToController(final Controller controller) {
        menuBar.connectToController(controller);
    }

    /**
     * Returns the menu-item responsible for allowing the user to
     * request the importing of image-annotations from a folder.
     *
     * @return the menu-item
     */
    MenuItem getFileImportAnnotationsItem() {
        return menuBar.getFileImportAnnotationsMenu();
    }

    /**
     * Return the horizontal separator at the bottom of this header.
     *
     * @return the separator
     */
    Separator getSeparator() {
        return separator;
    }

    /**
     * Returns the toggle-able menu-item responsible for switching the
     * visibility of the {@link ImageFileExplorerView} object on and off.
     *
     * @return the menu-item
     */
    CheckMenuItem getViewShowImagesPanelItem() {
        return menuBar.getViewShowImagesPanelItem();
    }

    CheckMenuItem getViewMaximizeImagesItem() {
        return menuBar.getViewMaximizeImagesItem();
    }
}
