package com.github.mfl28.boundingboxeditor.ui.settings;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.ui.View;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

public class InferenceSettingsView extends VBox implements View {
    private final Button selectModelButton = new Button("Select");
    private final Label selectedModelLabel = new Label("None selected");
    private final ToggleSwitch inferenceEnabledControl = new ToggleSwitch();
    private final TextField inferenceAddressField = new TextField();
    private final TextField inferencePortField = new TextField();
    private final TextField managementAddressField = new TextField();
    private final TextField managementPortField = new TextField();
    private final Spinner<Double> minimumScoreControl = new Spinner<>(0.0, 1.0, 0.0, 0.05);

    InferenceSettingsView() {
        setUpContent();
    }

    public void setDisplayedSettingsFromConfig(BoundingBoxPredictorClientConfig config) {
        inferenceAddressField.setText(config.getInferenceUrl());
        inferencePortField.setText(Integer.toString(config.getInferencePort()));
        managementAddressField.setText(config.getManagementUrl());
        managementPortField.setText(Integer.toString(config.getManagementPort()));

        if(config.getInferenceModelName() == null) {
            selectedModelLabel.setText("None selected");
        } else {
            selectedModelLabel.setText(config.getInferenceModelName());
        }
    }

    public void applyDisplayedSettingsToConfig(BoundingBoxPredictorClientConfig config) {
        config.setInferenceUrl(inferenceAddressField.getText());
        config.setInferencePort(Integer.parseInt(inferencePortField.getText()));
        config.setManagementUrl(managementAddressField.getText());
        config.setManagementPort(Integer.parseInt(managementPortField.getText()));

        if(selectedModelLabel.getText().equals("None selected")) {
            config.setInferenceModelName(null);
        } else {
            config.setInferenceModelName(selectedModelLabel.getText());
        }
    }

    @Override
    public void connectToController(Controller controller) {
        selectModelButton.setOnAction(action -> controller.onRegisterModelNameFetchingAction());
    }

    public Button getSelectModelButton() {
        return selectModelButton;
    }

    public Label getSelectedModelLabel() {
        return selectedModelLabel;
    }

    public ToggleSwitch getInferenceEnabledControl() {
        return inferenceEnabledControl;
    }

    public TextField getInferenceAddressField() {
        return inferenceAddressField;
    }

    public TextField getInferencePortField() {
        return inferencePortField;
    }

    public TextField getManagementAddressField() {
        return managementAddressField;
    }

    public TextField getManagementPortField() {
        return managementPortField;
    }

    public Spinner<Double> getMinimumScoreControl() {
        return minimumScoreControl;
    }

    private void setUpContent() {
        final GridPane gridPane = new GridPane();

        inferenceEnabledControl.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        inferenceEnabledControl.setSelected(false);
        gridPane.addRow(0, new Label("Enable inference"), new Group(inferenceEnabledControl));

        inferenceAddressField.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        inferenceAddressField.setText("http://localhost");
        inferencePortField.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        inferencePortField.setTextFormatter(UiUtils.createDecimalFormatter());
        inferencePortField.setText("8080");
        final HBox inferenceBox = new HBox(inferenceAddressField, new Label("Port"), inferencePortField);
        inferenceBox.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        inferenceBox.setSpacing(5);
        inferenceBox.setAlignment(Pos.CENTER_LEFT);
        gridPane.addRow(1, new Label("Inference address"), inferenceBox);

        managementAddressField.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        managementAddressField.setText("http://localhost");
        managementPortField.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        managementPortField.setTextFormatter(UiUtils.createDecimalFormatter());
        managementPortField.setText("8081");
        final HBox managementBox = new HBox(managementAddressField, new Label("Port"), managementPortField);
        managementBox.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        managementBox.setSpacing(5);
        managementBox.setAlignment(Pos.CENTER_LEFT);
        gridPane.addRow(2, new Label("Management address"), managementBox);

        selectModelButton.disableProperty().bind(managementAddressField.textProperty().isEmpty()
                                                                       .or(managementPortField.textProperty()
                                                                                              .isEmpty()));

        final HBox modelSelectionBox = new HBox(selectedModelLabel, selectModelButton);
        modelSelectionBox.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        modelSelectionBox.setSpacing(5);
        modelSelectionBox.setAlignment(Pos.CENTER_LEFT);
        gridPane.addRow(3, new Label("Model"), modelSelectionBox);

        minimumScoreControl.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        minimumScoreControl.setEditable(true);
        gridPane.addRow(4, new Label("Minimum score"), minimumScoreControl);

        gridPane.setHgap(10);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(5, 15, 0, 15));

        VBox.setVgrow(gridPane, Priority.ALWAYS);
        getChildren().add(gridPane);
    }
}
