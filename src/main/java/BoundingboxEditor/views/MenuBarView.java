package BoundingboxEditor.views;

import BoundingboxEditor.Controller;
import javafx.scene.control.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

class MenuBarView extends MenuBar implements View {
    private static final String FILE_MENU_TEXT = "_File";
    private static final String VIEW_MENU_TEXT = "_View";
    private static final String OPEN_FOLDER_TEXT = "_Open Folder...";
    private static final String SAVE_TEXT = "_Save...";
    private static final String FIT_WINDOW_TEXT = "_Fit Window";
    private static final String SHOW_IMAGE_EXPLORER = "Image Explorer";
    private static final String EXIT_TEXT = "E_xit";

    private final MenuItem fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT);
    private final MenuItem fileSaveItem = new MenuItem(SAVE_TEXT);
    private final MenuItem fileExitItem = new MenuItem(EXIT_TEXT);
    private final CheckMenuItem viewFitWindowItem = new CheckMenuItem(FIT_WINDOW_TEXT);
    private final CheckMenuItem showImageExplorer = new CheckMenuItem(SHOW_IMAGE_EXPLORER);

    MenuBarView() {
        getMenus().addAll(createFileMenu(), createViewMenu());
        showImageExplorer.setSelected(true);
        viewFitWindowItem.setSelected(true);
    }

    @Override
    public void connectToController(final Controller controller) {
        fileOpenFolderItem.setOnAction(controller::onRegisterOpenFolderAction);
        fileSaveItem.setOnAction(controller::onRegisterSaveAction);
        viewFitWindowItem.setOnAction(controller::onRegisterFitWindowAction);
        fileExitItem.setOnAction(controller::onRegisterExitAction);
    }

    CheckMenuItem getShowImageExplorerMenuItem() {
        return showImageExplorer;
    }

    CheckMenuItem getViewFitWindowItem(){
        return viewFitWindowItem;
    }

    private Menu createFileMenu() {
        Menu fileMenu = new Menu(FILE_MENU_TEXT);
        fileMenu.getItems().addAll(fileOpenFolderItem, fileSaveItem, fileExitItem);
        return fileMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu(VIEW_MENU_TEXT);
        SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
        separatorMenuItem.setContent(new Separator());
        viewMenu.getItems().addAll(viewFitWindowItem, separatorMenuItem, showImageExplorer);
        return viewMenu;
    }
}
