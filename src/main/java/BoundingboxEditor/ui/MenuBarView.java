package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import javafx.scene.control.*;

/**
 * Represents the main menu-bar UI-element of the application.
 *
 * @see MenuBar
 * @see View
 */
class MenuBarView extends MenuBar implements View {
    private static final String FILE_MENU_TEXT = "_File";
    private static final String VIEW_MENU_TEXT = "_View";
    private static final String OPEN_FOLDER_TEXT = "_Open Folder...";
    private static final String SAVE_TEXT = "_Save...";
    private static final String FIT_WINDOW_TEXT = "_Fit Window";
    private static final String SHOW_IMAGE_FILE_EXPLORER_TEXT = "_Image Explorer";
    private static final String EXIT_TEXT = "E_xit";
    private static final String MAIN_MENU_BAR_ID = "main-menu-bar";
    private static final String ANNOTATION_IMPORT_TEXT = "Import Annotations...";

    private final MenuItem fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT);
    private final MenuItem fileSaveItem = new MenuItem(SAVE_TEXT);
    private final MenuItem fileImportAnnotationsItem = new MenuItem(ANNOTATION_IMPORT_TEXT);
    private final MenuItem fileExitItem = new MenuItem(EXIT_TEXT);
    private final CheckMenuItem viewFitWindowItem = new CheckMenuItem(FIT_WINDOW_TEXT);
    private final CheckMenuItem viewShowImageFileExplorerItem = new CheckMenuItem(SHOW_IMAGE_FILE_EXPLORER_TEXT);

    /**
     * Creates a new menu-bar UI-element.
     */
    MenuBarView() {
        getMenus().addAll(createFileMenu(), createViewMenu());
        setId(MAIN_MENU_BAR_ID);
        viewShowImageFileExplorerItem.setSelected(true);
        viewFitWindowItem.setSelected(true);
    }

    @Override
    public void connectToController(final Controller controller) {
        fileOpenFolderItem.setOnAction(action -> controller.onRegisterOpenImageFolderAction());
        fileSaveItem.setOnAction(action -> controller.onRegisterSaveAnnotationsAction());
        fileImportAnnotationsItem.setOnAction(action -> controller.onRegisterImportAnnotationsAction());
        fileExitItem.setOnAction(action -> controller.onRegisterExitAction());
    }

    /**
     * Returns the menu-item which allows the user to request the import of image-annotations.
     *
     * @return the menu-item
     */
    MenuItem getFileImportAnnotationsItem() {
        return fileImportAnnotationsItem;
    }

    /**
     * Returns the check-menu-item which allows the user to switch the visibility of the
     * {@link ImageFileExplorerView} object.
     *
     * @return the menu-item
     */
    CheckMenuItem getViewShowImageFileExplorerItem() {
        return viewShowImageFileExplorerItem;
    }

    /**
     * Returns the check-menu-item which allows the user to switch the image-size mode.
     *
     * @return the menu-item
     */
    CheckMenuItem getViewFitWindowItem() {
        return viewFitWindowItem;
    }

    private Menu createFileMenu() {
        Menu fileMenu = new Menu(FILE_MENU_TEXT);
        fileMenu.getItems().addAll(fileOpenFolderItem, fileSaveItem, fileImportAnnotationsItem, fileExitItem);
        return fileMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu(VIEW_MENU_TEXT);
        viewMenu.getItems().addAll(viewFitWindowItem, new SeparatorMenuItem(), viewShowImageFileExplorerItem);
        return viewMenu;
    }
}
