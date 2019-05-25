package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import javafx.scene.control.*;

class MenuBarView extends MenuBar implements View {
    private static final String FILE_MENU_TEXT = "_File";
    private static final String VIEW_MENU_TEXT = "_View";
    private static final String OPEN_FOLDER_TEXT = "_Open Folder...";
    private static final String SAVE_TEXT = "_Save...";
    private static final String FIT_WINDOW_TEXT = "_Fit Window";
    private static final String SHOW_IMAGE_EXPLORER = "_Image Explorer";
    private static final String EXIT_TEXT = "E_xit";
    private static final String MAIN_MENU_BAR_ID = "main-menu-bar";

    private final MenuItem fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT);
    private final MenuItem fileSaveItem = new MenuItem(SAVE_TEXT);
    private final MenuItem fileImportAnnotationsItem = new MenuItem("Import Annotations...");
    private final MenuItem fileExitItem = new MenuItem(EXIT_TEXT);
    private final CheckMenuItem viewFitWindowItem = new CheckMenuItem(FIT_WINDOW_TEXT);
    private final CheckMenuItem viewShowImageExplorerItem = new CheckMenuItem(SHOW_IMAGE_EXPLORER);

    MenuBarView() {
        getMenus().addAll(createFileMenu(), createViewMenu());
        setId(MAIN_MENU_BAR_ID);
        viewShowImageExplorerItem.setSelected(true);
        viewFitWindowItem.setSelected(true);
    }

    @Override
    public void connectToController(final Controller controller) {
        fileOpenFolderItem.setOnAction(action -> controller.onRegisterOpenFolderAction());
        fileSaveItem.setOnAction(action -> controller.onRegisterSaveAction());
        fileImportAnnotationsItem.setOnAction(action -> controller.onRegisterImportAnnotationsAction());
        fileExitItem.setOnAction(action -> controller.onRegisterExitAction());
    }

    public MenuItem getFileImportAnnotationsItem() {
        return fileImportAnnotationsItem;
    }

    CheckMenuItem getViewShowImageExplorerItem() {
        return viewShowImageExplorerItem;
    }

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
        viewMenu.getItems().addAll(viewFitWindowItem, new SeparatorMenuItem(), viewShowImageExplorerItem);
        return viewMenu;
    }
}
