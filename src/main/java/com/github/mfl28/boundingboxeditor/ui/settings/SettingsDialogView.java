package com.github.mfl28.boundingboxeditor.ui.settings;

import com.github.mfl28.boundingboxeditor.controller.Controller;
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
    private static final String SETTINGS_DIALOG_PANE_ID = "settings-dialog-pane";
    private static final String SETTINGS_TITLE = "Settings";
    private static final String INFERENCE_CATEGORY_NAME = "Inference";
    private static final double SPLITPANE_DIVIDER_POSITION = 0.15;
    private static final String UI_CATEGORY_NAME = "UI";
    private final ObservableList<String> settingCategories = FXCollections.observableArrayList();
    private final Map<String, Node> categoryToContentMap = new HashMap<>();
    private final ListView<String> settingCategoriesView = new ListView<>(settingCategories);
    private final VBox contentBox = new VBox();
    private final InferenceSettingsView inferenceSettings = new InferenceSettingsView();
    private final UISettingsView uiSettingsView = new UISettingsView();

    public SettingsDialogView() {
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

        setUpSettingCategories();

        setUpInternalListeners();
        settingCategoriesView.getSelectionModel().selectFirst();
    }

    public InferenceSettingsView getInferenceSettings() {
        return inferenceSettings;
    }

    public UISettingsView getUiSettings() {
        return uiSettingsView;
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

        inferenceSettings.connectToController(controller);
    }

    private void setUpSettingCategories() {
        addCategoryContentPair(INFERENCE_CATEGORY_NAME, inferenceSettings);
        addCategoryContentPair(UI_CATEGORY_NAME, uiSettingsView);
    }

    private <T extends Node & ApplyButtonChangeProvider> void addCategoryContentPair(String category, T contentNode) {
        final ScrollPane scrollPane = new ScrollPane(contentNode);
        scrollPane.setFitToWidth(true);

        categoryToContentMap.put(category, scrollPane);
        settingCategories.add(category);
        contentNode.registerPropertyListeners((Button) getDialogPane().lookupButton(ButtonType.APPLY));
    }

    private void setUpInternalListeners() {
        settingCategoriesView.getSelectionModel()
                             .selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            contentBox.getChildren().setAll(categoryToContentMap.get(newValue));
        });

        setOnShowing(event -> {
            settingCategoriesView.getSelectionModel().selectFirst();
            getDialogPane().lookupButton(ButtonType.APPLY).setDisable(true);
        });
    }
}
