package BoundingboxEditor;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class TopPanelView extends VBox implements View {
    private static final String NEXT_BUTTON_ID = "next-button";
    private static final String PREVIOUS_BUTTON_ID = "previous-button";
    private static final String TOP_PANEL_STYLE = "topBox";
    private static final String FILE_MENU_TEXT = "_File";
    private static final String VIEW_MENU_TEXT = "_View";
    private static final String OPEN_FOLDER_TEXT = "_Open Folder...";
    private static final String SAVE_TEXT = "_Save...";
    private static final String FIT_WINDOW_TEXT = "_Fit Window";
    private static final String SHOW_SETTINGS_BAR_TEXT = "Settings Bar";
    private static final String EXIT_TEXT = "E_xit";
    private static final String NEXT_ICON_PATH = "/icons/arrow_right.png";
    private static final String PREVIOUS_ICON_PATH = "/icons/arrow_left.png";
    private static final double ICON_WIDTH = 20.0;
    private static final double ICON_HEIGHT = 20.0;

    private final MenuItem fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT);
    private final MenuItem fileSaveItem = new MenuItem(SAVE_TEXT);
    private final MenuItem fileExitItem = new MenuItem(EXIT_TEXT);
    private final MenuItem viewFitWindowItem = new MenuItem(FIT_WINDOW_TEXT);
    private final CheckMenuItem viewShowSettingsItem = new CheckMenuItem(SHOW_SETTINGS_BAR_TEXT);
    private final Button nextButton = Utils.createIconButton(getClass().getResource(NEXT_ICON_PATH).toExternalForm(), ICON_WIDTH, ICON_HEIGHT);
    private final Label indexLabel = new Label();
    private final Button previousButton = Utils.createIconButton(getClass().getResource(PREVIOUS_ICON_PATH).toExternalForm(), ICON_WIDTH, ICON_HEIGHT);
    private final ToolBar navigationBar = createNavigationBar();

    TopPanelView() {
        this.getChildren().addAll(createMenuBar(), new Separator(), navigationBar);
        this.getStyleClass().add(TOP_PANEL_STYLE);

        nextButton.setId(NEXT_BUTTON_ID);
        previousButton.setId(PREVIOUS_BUTTON_ID);
    }

    public ToolBar getNavigationBar() {
        return navigationBar;
    }

    public Button getNextButton() {
        return nextButton;
    }

    public Button getPreviousButton() {
        return previousButton;
    }

    public MenuItem getFileOpenFolderItem() {
        return fileOpenFolderItem;
    }

    public CheckMenuItem getViewShowSettingsItem() {
        return viewShowSettingsItem;
    }

    public Label getIndexLabel() {
        return indexLabel;
    }

    @Override
    public void connectToController(final Controller controller) {
        fileOpenFolderItem.setOnAction(controller::onRegisterOpenFolderAction);
        fileSaveItem.setOnAction(controller::onRegisterSaveAction);
        viewFitWindowItem.setOnAction(controller::onRegisterFitWindowAction);
        fileExitItem.setOnAction(controller::onRegisterExitAction);

        nextButton.setOnAction(controller::onRegisterNextAction);
        previousButton.setOnAction(controller::onRegisterPreviousAction);
    }

    private MenuBar createMenuBar() {
        final Menu fileMenu = new Menu(FILE_MENU_TEXT);
        fileMenu.getItems().addAll(fileOpenFolderItem, fileSaveItem, fileExitItem);

        final Menu viewMenu = new Menu(VIEW_MENU_TEXT);
        viewMenu.getItems().addAll(viewFitWindowItem, viewShowSettingsItem);

        final MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, viewMenu);

        return menuBar;
    }

    private ToolBar createNavigationBar() {
        final ToolBar toolBar = new ToolBar();
        toolBar.getItems().addAll(Utils.createHSpacer(), previousButton, indexLabel, nextButton, Utils.createHSpacer());
        toolBar.setVisible(false);
        //FIXME: offset for image controlls, should probably be dynamic
        toolBar.setPadding(new Insets(0, 0, 0, 260));
        return toolBar;
    }
}
