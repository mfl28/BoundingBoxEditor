/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
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
    private static final String CSV_FORMAT_EXPORT_TEXT = "CSV format...";
    private static final String JSON_FORMAT_IMPORT_TEXT = "JSON format...";
    private static final String FILE_MENU_ID = "file-menu";
    private static final String FILE_OPEN_FOLDER_MENU_ITEM_ID = "file-open-folder-menu-item";
    private static final String FILE_EXPORT_ANNOTATIONS_MENU_ID = "file-export-annotations-menu";
    private static final String FILE_IMPORT_ANNOTATIONS_MENU_ID = "file-import-annotations-menu";
    private static final String FILE_EXIT_MENU_ITEM_ID = "file-exit-menu-item";
    private static final String VIEW_MENU_ID = "view-menu";
    private static final String VIEW_MAXIMIZE_IMAGES_MENU_ITEM_ID = "view-maximize-images-menu-item";
    private static final String VIEW_SHOW_IMAGES_PANEL_MENU_ITEM_ID = "view-show-images-panel-menu-item";
    private static final String PVOC_EXPORT_MENU_ITEM_ID = "pvoc-export-menu-item";
    private static final String YOLO_EXPORT_MENU_ITEM_ID = "yolo-export-menu-item";
    private static final String JSON_EXPORT_MENU_ITEM_ID = "json-export-menu-item";
    private static final String CSV_EXPORT_MENU_ITEM_ID = "csv-export-menu-item";
    private static final String PVOC_IMPORT_MENU_ITEM_ID = "pvoc-import-menu-item";
    private static final String YOLO_IMPORT_MENU_ITEM_ID = "yolo-import-menu-item";
    private static final String JSON_IMPORT_MENU_ITEM_ID = "json-import-menu-item";
    private static final String SETTINGS_TEXT = "Se_ttings";
    private static final String SETTINGS_ICON_ID = "settings-icon";
    private static final String FILE_SETTINGS_MENU_ITEM_ID = "file-settings-menu-item";
    private static final String HELP_MENU_TEXT = "_Help";
    private static final String HELP_MENU_ID = "help-menu";
    private static final String DOCUMENTATION_TEXT = "_Documentation";
    private static final String ABOUT_TEXT = "_About";
    private static final String DOCUMENTATION_MENU_ITEM_ID = "documentation-menu-item";
    private static final String ABOUT_MENU_ITEM_ID = "about-menu-item";

    private final MenuItem fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT, createIconRegion(OPEN_FOLDER_ICON_ID));
    private final Menu fileExportAnnotationsMenu = new Menu(SAVE_TEXT, createIconRegion(SAVE_ICON_ID));
    private final MenuItem pvocExportMenuItem = new MenuItem(PASCAL_VOC_FORMAT_EXPORT_TEXT);
    private final MenuItem yoloExportMenuItem = new MenuItem(YOLO_FORMAT_EXPORT_TEXT);
    private final MenuItem jsonExportMenuItem = new MenuItem(JSON_FORMAT_EXPORT_TEXT);
    private final MenuItem csvExportMenuItem = new MenuItem(CSV_FORMAT_EXPORT_TEXT);
    private final MenuItem settingsMenuItem = new MenuItem(SETTINGS_TEXT, createIconRegion(SETTINGS_ICON_ID));

    private final Menu fileImportAnnotationsMenu =
            new Menu(ANNOTATION_IMPORT_TEXT, createIconRegion(FILE_IMPORT_ICON_ID));
    private final MenuItem pvocImportMenuItem = new MenuItem(PASCAL_VOC_FORMAT_IMPORT_TEXT);
    private final MenuItem yoloRImportMenuItem = new MenuItem(YOLO_FORMAT_IMPORT_TEXT);
    private final MenuItem jsonImportMenuItem = new MenuItem(JSON_FORMAT_IMPORT_TEXT);
    private final MenuItem fileExitItem = new MenuItem(EXIT_TEXT, createIconRegion(EXIT_ICON_ID));
    private final CheckMenuItem viewMaximizeImagesItem = new CheckMenuItem(MAXIMIZE_IMAGES_TEXT);
    private final CheckMenuItem viewShowImagesPanelItem = new CheckMenuItem(SHOW_IMAGE_FILE_EXPLORER_TEXT);
    private final MenuItem documentationMenuItem = new MenuItem(DOCUMENTATION_TEXT);
    private final MenuItem aboutMenuItem = new MenuItem(ABOUT_TEXT);

    /**
     * Creates a new menu-bar UI-element.
     */
    MenuBarView() {
        getMenus().addAll(createFileMenu(), createViewMenu(), createHelpMenu());
        setId(MAIN_MENU_BAR_ID);
        viewShowImagesPanelItem.setSelected(true);
        viewMaximizeImagesItem.setSelected(true);

        fileExportAnnotationsMenu.getItems().addAll(pvocExportMenuItem, yoloExportMenuItem, jsonExportMenuItem, csvExportMenuItem);

        pvocExportMenuItem.setId(PVOC_EXPORT_MENU_ITEM_ID);
        yoloExportMenuItem.setId(YOLO_EXPORT_MENU_ITEM_ID);
        jsonExportMenuItem.setId(JSON_EXPORT_MENU_ITEM_ID);
        csvExportMenuItem.setId(CSV_EXPORT_MENU_ITEM_ID);

        fileImportAnnotationsMenu.getItems().addAll(pvocImportMenuItem,
                                                    yoloRImportMenuItem,
                                                    jsonImportMenuItem);

        pvocImportMenuItem.setId(PVOC_IMPORT_MENU_ITEM_ID);
        yoloRImportMenuItem.setId(YOLO_IMPORT_MENU_ITEM_ID);
        jsonImportMenuItem.setId(JSON_IMPORT_MENU_ITEM_ID);
    }

    @Override
    public void connectToController(final Controller controller) {
        fileOpenFolderItem.setOnAction(action ->
                                               controller.onRegisterOpenImageFolderAction());
        pvocExportMenuItem.setOnAction(action ->
                                               controller.onRegisterSaveAnnotationsAction(
                                                       ImageAnnotationSaveStrategy.Type.PASCAL_VOC));
        yoloExportMenuItem.setOnAction(action ->
                                               controller.onRegisterSaveAnnotationsAction(
                                                       ImageAnnotationSaveStrategy.Type.YOLO));
        jsonExportMenuItem.setOnAction(action ->
                                               controller.onRegisterSaveAnnotationsAction(
                                                       ImageAnnotationSaveStrategy.Type.JSON));
        csvExportMenuItem.setOnAction(action ->
										       controller.onRegisterSaveAnnotationsAction(
										               ImageAnnotationSaveStrategy.Type.CSV));
        pvocImportMenuItem.setOnAction(action ->
                                               controller.onRegisterImportAnnotationsAction(
                                                       ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        yoloRImportMenuItem.setOnAction(action ->
                                                controller.onRegisterImportAnnotationsAction(
                                                        ImageAnnotationLoadStrategy.Type.YOLO));
        jsonImportMenuItem.setOnAction(action ->
                                               controller.onRegisterImportAnnotationsAction(
                                                       ImageAnnotationLoadStrategy.Type.JSON));
        fileExitItem.setOnAction(action -> controller.onRegisterExitAction());
        settingsMenuItem.setOnAction(action -> controller.onRegisterSettingsAction());
        documentationMenuItem.setOnAction(action -> controller.onRegisterDocumentationAction());
        aboutMenuItem.setOnAction(action -> controller.onRegisterAboutAction());
    }

    /**
     * Returns the menu-item which allows the user to request the import of image-annotations.
     *
     * @return the menu-item
     */
    MenuItem getFileImportAnnotationsMenu() {
        return fileImportAnnotationsMenu;
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
        fileMenu.setId(FILE_MENU_ID);

        fileMenu.getItems().addAll(
                fileOpenFolderItem,
                fileExportAnnotationsMenu,
                fileImportAnnotationsMenu,
                settingsMenuItem,
                fileExitItem
        );

        fileOpenFolderItem.setId(FILE_OPEN_FOLDER_MENU_ITEM_ID);
        fileExportAnnotationsMenu.setId(FILE_EXPORT_ANNOTATIONS_MENU_ID);
        fileImportAnnotationsMenu.setId(FILE_IMPORT_ANNOTATIONS_MENU_ID);
        settingsMenuItem.setId(FILE_SETTINGS_MENU_ITEM_ID);
        fileExitItem.setId(FILE_EXIT_MENU_ITEM_ID);

        return fileMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu(VIEW_MENU_TEXT);
        viewMenu.setId(VIEW_MENU_ID);

        viewMenu.getItems().addAll(
                viewMaximizeImagesItem,
                new SeparatorMenuItem(),
                viewShowImagesPanelItem
        );

        viewMaximizeImagesItem.setId(VIEW_MAXIMIZE_IMAGES_MENU_ITEM_ID);
        viewShowImagesPanelItem.setId(VIEW_SHOW_IMAGES_PANEL_MENU_ITEM_ID);

        return viewMenu;
    }

    private Menu createHelpMenu() {
        Menu helpMenu = new Menu(HELP_MENU_TEXT);
        helpMenu.setId(HELP_MENU_ID);

        helpMenu.getItems().addAll(
                documentationMenuItem,
                aboutMenuItem
        );

        documentationMenuItem.setId(DOCUMENTATION_MENU_ITEM_ID);
        aboutMenuItem.setId(ABOUT_MENU_ITEM_ID);

        return helpMenu;
    }

    private Region createIconRegion(String cssId) {
        Region region = new Region();
        region.setId(cssId);
        return region;
    }
}
