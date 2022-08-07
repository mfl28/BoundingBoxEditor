/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.ui.settings;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.ui.MainView;
import com.github.mfl28.boundingboxeditor.ui.View;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class SettingsDialogView extends Dialog<ButtonType> implements View {
    public static final String SETTINGS_TITLE = "Settings";
    private static final String SETTINGS_DIALOG_PANE_ID = "settings-dialog-pane";
    private static final double SPLITPANE_DIVIDER_POSITION = 0.35;
    private final ObservableList<String> settingCategories = FXCollections.observableArrayList();
    private final Map<String, Node> categoryToContentMap = new HashMap<>();
    private final ListView<String> settingCategoriesView = new ListView<>(settingCategories);
    private final VBox contentBox = new VBox();

    public SettingsDialogView() {
        MainView.applyDialogStyle(this);
        setTitle(SETTINGS_TITLE);

        final SplitPane settingSplitPane = new SplitPane(settingCategoriesView, contentBox);
        settingSplitPane.setDividerPosition(0, SPLITPANE_DIVIDER_POSITION);
        SplitPane.setResizableWithParent(settingCategoriesView, false);

        final DialogPane dialogPane = new DialogPane();
        dialogPane.setId(SETTINGS_DIALOG_PANE_ID);
        dialogPane.setContent(settingSplitPane);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL, ButtonType.APPLY);
        dialogPane.getButtonTypes()
                  .forEach(buttonType -> ((Button) dialogPane.lookupButton(buttonType)).setDefaultButton(false));

        setDialogPane(dialogPane);
        setResizable(true);

        setUpInternalListeners();
    }

    @Override
    public void connectToController(Controller controller) {
        getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, event -> {
            controller.onRegisterSettingsApplyAction(event, ButtonType.APPLY);
            getDialogPane().lookupButton(ButtonType.APPLY).setDisable(true);
            event.consume();
        });

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION,
                                                                   event -> controller
                                                                           .onRegisterSettingsApplyAction(event,
                                                                                                          ButtonType.OK));

        setOnCloseRequest(event -> controller.onRegisterSettingsCancelCloseAction());
    }

    public <T extends Node & ApplyButtonChangeProvider> void addCategoryContentPair(String category, T contentNode) {
        final ScrollPane scrollPane = new ScrollPane(contentNode);
        scrollPane.setFitToWidth(true);

        categoryToContentMap.put(category, scrollPane);
        settingCategories.add(category);
        contentNode.registerPropertyListeners((Button) getDialogPane().lookupButton(ButtonType.APPLY));
    }

    private void setUpInternalListeners() {
        settingCategoriesView.getSelectionModel()
                             .selectedItemProperty().addListener((observable, oldValue, newValue) ->
                                                                         contentBox.getChildren()
                                                                                   .setAll(categoryToContentMap
                                                                                                   .get(newValue)));

        setOnShowing(event -> {
            settingCategoriesView.getSelectionModel().selectFirst();
            getDialogPane().lookupButton(ButtonType.APPLY).setDisable(true);
        });
    }
}
