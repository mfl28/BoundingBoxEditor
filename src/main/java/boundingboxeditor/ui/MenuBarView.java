package boundingboxeditor.ui;

import boundingboxeditor.controller.Controller;
import boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

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
    private static final String SAVE_TEXT = "_Export Annotations";
    private static final String MAXIMIZE_IMAGES_TEXT = "_Maximize Images";
    private static final String SHOW_IMAGE_FILE_EXPLORER_TEXT = "_Show Images Panel";
    private static final String EXIT_TEXT = "E_xit";
    private static final String MAIN_MENU_BAR_ID = "main-menu-bar";
    private static final String ANNOTATION_IMPORT_TEXT = "_Import Annotations";
    private static final String SAVE_ICON_ID = "save-icon";
    private static final String OPEN_FOLDER_ICON_ID = "folder-icon";
    private static final String EXIT_ICON_ID = "exit-icon";
    private static final String FILE_IMPORT_ICON_ID = "file-import-icon";
    private static final String PASCAL_VOC_FORMAT_IMPORT_TEXT = "Pascal-VOC format...";
    private static final String YOLO_FORMAT_IMPORT_TEXT = "YOLO format...";
    private static final String PASCAL_VOC_FORMAT_EXPORT_TEXT = "Pascal-VOC format...";
    private static final String YOLO_FORMAT_EXPORT_TEXT = "YOLO format...";
    private static final String JSON_FORMAT_EXPORT_TEXT = "JSON format...";
    private static final String JSON_FORMAT_IMPORT_TEXT = "JSON format...";

    private final MenuItem fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT, createIconRegion(OPEN_FOLDER_ICON_ID));
    private final Menu fileExportMenu = new Menu(SAVE_TEXT, createIconRegion(SAVE_ICON_ID));
    private final MenuItem PVOCExportMenuItem = new MenuItem(PASCAL_VOC_FORMAT_EXPORT_TEXT);
    private final MenuItem YOLOExportMenuItem = new MenuItem(YOLO_FORMAT_EXPORT_TEXT);
    private final MenuItem JSONExportMenuItem = new MenuItem(JSON_FORMAT_EXPORT_TEXT);

    private final Menu fileImportAnnotationsItem =
            new Menu(ANNOTATION_IMPORT_TEXT, createIconRegion(FILE_IMPORT_ICON_ID));
    private final MenuItem PVOCImportMenuItem = new MenuItem(PASCAL_VOC_FORMAT_IMPORT_TEXT);
    private final MenuItem YOLORImportMenuItem = new MenuItem(YOLO_FORMAT_IMPORT_TEXT);
    private final MenuItem JSONImportMenuItem = new MenuItem(JSON_FORMAT_IMPORT_TEXT);
    private final MenuItem fileExitItem = new MenuItem(EXIT_TEXT, createIconRegion(EXIT_ICON_ID));
    private final CheckMenuItem viewMaximizeImagesItem = new CheckMenuItem(MAXIMIZE_IMAGES_TEXT);
    private final CheckMenuItem viewShowImagesPanelItem = new CheckMenuItem(SHOW_IMAGE_FILE_EXPLORER_TEXT);

    /**
     * Creates a new menu-bar UI-element.
     */
    MenuBarView() {
        getMenus().addAll(createFileMenu(), createViewMenu());
        setId(MAIN_MENU_BAR_ID);
        viewShowImagesPanelItem.setSelected(true);
        viewMaximizeImagesItem.setSelected(true);

        fileExportMenu.getItems().addAll(PVOCExportMenuItem, YOLOExportMenuItem, JSONExportMenuItem);
        fileImportAnnotationsItem.getItems().addAll(PVOCImportMenuItem,
                                                    YOLORImportMenuItem, JSONImportMenuItem);
    }

    @Override
    public void connectToController(final Controller controller) {
        fileOpenFolderItem.setOnAction(action ->
                                               controller.onRegisterOpenImageFolderAction());
        PVOCExportMenuItem.setOnAction(action ->
                                               controller.onRegisterSaveAnnotationsAction(
                                                       ImageAnnotationSaveStrategy.Type.PASCAL_VOC));
        YOLOExportMenuItem.setOnAction(action ->
                                               controller.onRegisterSaveAnnotationsAction(
                                                       ImageAnnotationSaveStrategy.Type.YOLO));
        JSONExportMenuItem.setOnAction(action ->
                                               controller.onRegisterSaveAnnotationsAction(
                                                       ImageAnnotationSaveStrategy.Type.JSON));
        PVOCImportMenuItem.setOnAction(action ->
                                               controller.onRegisterImportAnnotationsAction(
                                                       ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        YOLORImportMenuItem.setOnAction(action ->
                                                controller.onRegisterImportAnnotationsAction(
                                                        ImageAnnotationLoadStrategy.Type.YOLO));
        JSONImportMenuItem.setOnAction(action ->
                                               controller.onRegisterImportAnnotationsAction(
                                                       ImageAnnotationLoadStrategy.Type.JSON));
        fileExitItem.setOnAction(action ->
                                         controller.onRegisterExitAction());
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
    CheckMenuItem getViewShowImagesPanelItem() {
        return viewShowImagesPanelItem;
    }

    /**
     * Returns the check-menu-item which allows the user to switch the image-size mode.
     *
     * @return the menu-item
     */
    CheckMenuItem getViewMaximizeImagesItem() {
        return viewMaximizeImagesItem;
    }

    private Menu createFileMenu() {
        Menu fileMenu = new Menu(FILE_MENU_TEXT);

        fileMenu.getItems().addAll(
                fileOpenFolderItem,
                fileExportMenu,
                fileImportAnnotationsItem,
                fileExitItem
        );

        return fileMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu(VIEW_MENU_TEXT);

        viewMenu.getItems().addAll(
                viewMaximizeImagesItem,
                new SeparatorMenuItem(),
                viewShowImagesPanelItem
        );

        return viewMenu;
    }

    private Region createIconRegion(String cssId) {
        Region region = new Region();
        region.setId(cssId);
        return region;
    }
}
