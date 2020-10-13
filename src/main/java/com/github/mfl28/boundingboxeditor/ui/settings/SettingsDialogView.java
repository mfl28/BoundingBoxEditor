package com.github.mfl28.boundingboxeditor.ui.settings;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.ui.MainView;
import com.github.mfl28.boundingboxeditor.ui.View;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class SettingsDialogView extends Dialog<ButtonType> implements View {
    private final ObservableList<String> settingCategories = FXCollections.observableArrayList();
    private final Map<String, Node> categoryToContentMap = new HashMap<>();
    private final ListView<String> settingCategoriesView = new ListView<>(settingCategories);
    private final VBox contentBox = new VBox();
    private final InferenceSettingsView inferenceSettings = new InferenceSettingsView();

    public SettingsDialogView() {
        setTitle("Settings");

        final SplitPane settingSplitPane = new SplitPane(settingCategoriesView, contentBox);
        settingSplitPane.setDividerPosition(0, 0.15);
        SplitPane.setResizableWithParent(settingCategoriesView, false);

        final DialogPane dialogPane = new DialogPane();
        dialogPane.setId("settings-dialog-pane");
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

    @Override
    public void connectToController(Controller controller) {
        getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, event -> {
            controller.onRegisterSettingsApplyAction();
            event.consume();
        });

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION,
                                                                   event -> controller.onRegisterSettingsApplyAction());

        inferenceSettings.connectToController(controller);
    }

    private void setUpSettingCategories() {
        addCategoryContentPair("Inference", inferenceSettings);
    }

    private void addCategoryContentPair(String category, Node contentNode) {
        categoryToContentMap.put(category, contentNode);
        settingCategories.add(category);
    }

    private void setUpInternalListeners() {
        settingCategoriesView.getSelectionModel()
                             .selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            contentBox.getChildren().clear();
            contentBox.getChildren().add(categoryToContentMap.get(newValue));
        });
    }
}
