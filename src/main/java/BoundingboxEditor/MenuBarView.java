package BoundingboxEditor;

import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

class MenuBarView extends MenuBar implements View {
    private static final String FILE_MENU_TEXT = "_File";
    private static final String VIEW_MENU_TEXT = "_View";
    private static final String OPEN_FOLDER_TEXT = "_Open Folder...";
    private static final String SAVE_TEXT = "_Save...";
    private static final String FIT_WINDOW_TEXT = "_Fit Window";
    private static final String SHOW_SETTINGS_BAR_TEXT = "Settings Bar";
    private static final String EXIT_TEXT = "E_xit";

    private final MenuItem fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT);
    private final MenuItem fileSaveItem = new MenuItem(SAVE_TEXT);
    private final MenuItem fileExitItem = new MenuItem(EXIT_TEXT);
    private final MenuItem viewFitWindowItem = new MenuItem(FIT_WINDOW_TEXT);
    private final CheckMenuItem viewShowSettingsItem = new CheckMenuItem(SHOW_SETTINGS_BAR_TEXT);

    MenuBarView() {
        getMenus().addAll(createFileMenu(), createViewMenu());
    }

    @Override
    public void connectToController(final Controller controller) {
        fileOpenFolderItem.setOnAction(controller::onRegisterOpenFolderAction);
        fileSaveItem.setOnAction(controller::onRegisterSaveAction);
        viewFitWindowItem.setOnAction(controller::onRegisterFitWindowAction);
        fileExitItem.setOnAction(controller::onRegisterExitAction);
    }

    private Menu createFileMenu() {
        Menu fileMenu = new Menu(FILE_MENU_TEXT);
        fileMenu.getItems().addAll(fileOpenFolderItem, fileSaveItem, fileExitItem);
        return fileMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu(VIEW_MENU_TEXT);
        viewMenu.getItems().addAll(viewFitWindowItem, viewShowSettingsItem);
        return viewMenu;
    }
}
