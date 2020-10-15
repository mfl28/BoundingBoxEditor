package com.github.mfl28.boundingboxeditor.ui.settings;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorConfig;
import com.github.mfl28.boundingboxeditor.ui.View;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.beans.binding.Bindings;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

public class InferenceSettingsView extends VBox implements View, ApplyButtonChangeListener {
    private static final String NO_MODEL_SELECTED_TEXT = "None";
    private static final String ENABLE_INFERENCE_LABEL_TEXT = "Enable inference";
    private static final String PORT_LABEL_TEXT = "Port";
    private static final String INFERENCE_ADDRESS_LABEL_TEXT = "Inference address";
    private static final String MANAGEMENT_ADDRESS_LABEL_TEXT = "Management address";
    private static final String MODEL_LABEL_TEXT = "Model";
    private static final String MINIMUM_SCORE_LABEL_TEXT = "Minimum score";
    private static final String GRID_PANE_STYLE_CLASS = "grid-pane";
    private static final String SETTINGS_ENTRY_BOX_STYLE_CLASS = "settings-entry-box";
    private static final String SELECTED_MODEL_LABEL_ID = "selected-model-label";
    private static final String RESIZE_IMAGES_LABEL_TEXT = "Resize Images";
    private static final String RESIZE_IMAGES_WIDTH_LABEL_TEXT = "Width";
    private static final String RESIZE_IMAGES_HEIGHT_LABEL_TEXT = "Height";
    private static final String RESIZE_IMAGES_SIZE_LABEL_TEXT = "Size";
    private static final String RESIZE_IMAGES_KEEP_RATIO_LABEL_TEXT = "Keep Ratio";
    private static final String MERGE_CATEGORIES_LABEL_TEXT = "Merge Categories";
    private final Button selectModelButton = new Button("Select");
    private final Label selectedModelLabel = new Label(NO_MODEL_SELECTED_TEXT);
    private final ToggleSwitch inferenceEnabledControl = new ToggleSwitch();
    private final TextField inferenceAddressField = new TextField();
    private final TextField inferencePortField = new TextField();
    private final TextField managementAddressField = new TextField();
    private final TextField managementPortField = new TextField();
    private final Spinner<Double> minimumScoreControl = new Spinner<>(0.0, 1.0, 0.0, 0.05);
    private final CheckBox resizeImagesControl = new CheckBox();
    private final TextField maxImageWidthField = new TextField();
    private final TextField maxImageHeightField = new TextField();
    private final CheckBox keepImageRatio = new CheckBox();
    private final CheckBox mergeCategoriesControl = new CheckBox();

    InferenceSettingsView() {
        setUpContent();
    }

    public void setDisplayedSettingsFromPredictorClientConfig(BoundingBoxPredictorClientConfig config) {
        inferenceAddressField.setText(config.getInferenceUrl());
        inferencePortField.setText(Integer.toString(config.getInferencePort()));
        managementAddressField.setText(config.getManagementUrl());
        managementPortField.setText(Integer.toString(config.getManagementPort()));

        if(config.getInferenceModelName() == null) {
            selectedModelLabel.setText(NO_MODEL_SELECTED_TEXT);
        } else {
            selectedModelLabel.setText(config.getInferenceModelName());
        }
    }

    public void applyDisplayedSettingsToPredictorClientConfig(BoundingBoxPredictorClientConfig config) {
        config.setInferenceUrl(inferenceAddressField.getText());
        config.setInferencePort(Integer.parseInt(inferencePortField.getText()));
        config.setManagementUrl(managementAddressField.getText());
        config.setManagementPort(Integer.parseInt(managementPortField.getText()));

        if(selectedModelLabel.getText().equals(NO_MODEL_SELECTED_TEXT)) {
            config.setInferenceModelName(null);
        } else {
            config.setInferenceModelName(selectedModelLabel.getText());
        }
    }

    public void applyDisplayedSettingsToPredictorConfig(BoundingBoxPredictorConfig config) {
        config.setMinimumScore(minimumScoreControl.getValue());
        config.setInferenceEnabled(inferenceEnabledControl.isSelected());
        config.setResizeImages(resizeImagesControl.isSelected());
        config.setMaxImageWidth(Integer.parseInt(maxImageWidthField.getText()));
        config.setMaxImageHeight(Integer.parseInt(maxImageHeightField.getText()));
        config.setKeepImageRatio(keepImageRatio.isSelected());
        config.setMergeCategories(mergeCategoriesControl.isSelected());
    }

    public void setDisplayedSettingsFromPredictorConfig(BoundingBoxPredictorConfig config) {
        minimumScoreControl.getEditor().setText(Double.toString(config.getMinimumScore()));
        inferenceEnabledControl.setSelected(config.isInferenceEnabled());
        resizeImagesControl.setSelected(config.isResizeImages());
        maxImageWidthField.setText(Integer.toString(config.getMaxImageWidth()));
        maxImageHeightField.setText(Integer.toString(config.getMaxImageHeight()));
        keepImageRatio.setSelected(config.isKeepImageRatio());
        mergeCategoriesControl.setSelected(config.isMergeCategories());
    }

    @Override
    public void connectToController(Controller controller) {
        selectModelButton.setOnAction(action -> controller.onRegisterModelNameFetchingAction());
    }

    @Override
    public void registerPropertyListeners(Button applyButton) {
        selectedModelLabel.textProperty()
                          .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        inferenceEnabledControl.selectedProperty()
                               .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        inferenceAddressField.textProperty()
                             .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        inferencePortField.textProperty()
                          .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        managementAddressField.textProperty()
                              .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        managementPortField.textProperty()
                           .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        minimumScoreControl.valueProperty()
                           .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        resizeImagesControl.selectedProperty()
                           .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        maxImageWidthField.textProperty()
                          .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        maxImageHeightField.textProperty()
                           .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        keepImageRatio.selectedProperty()
                      .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        mergeCategoriesControl.selectedProperty()
                              .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
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
        gridPane.getStyleClass().add(GRID_PANE_STYLE_CLASS);

        int rowIndex = -1;

        addInferenceControlRow(gridPane, ++rowIndex);
        addInferenceAddressRow(gridPane, ++rowIndex);
        addManagementAddressRow(gridPane, ++rowIndex);
        addModelSelectionRow(gridPane, ++rowIndex);
        addMinimumPredictionScoreRow(gridPane, ++rowIndex);
        addImageResizePreprocessingChoiceRow(gridPane, ++rowIndex);
        addImageResizePreprocessingSetupRow(gridPane, ++rowIndex);
        addImageResizePreprocessingRatioRow(gridPane, ++rowIndex);
        addPredictionMergeCategoryChoiceRow(gridPane, ++rowIndex);

        VBox.setVgrow(gridPane, Priority.ALWAYS);
        getChildren().add(gridPane);
    }

    private void addInferenceControlRow(GridPane gridPane, int row) {
        inferenceEnabledControl.setSelected(false);
        inferenceEnabledControl.setText("  ");

        final HBox inferenceEnabledControlBox = new HBox(new Label(ENABLE_INFERENCE_LABEL_TEXT),
                                                         inferenceEnabledControl);
        inferenceEnabledControlBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        gridPane.add(inferenceEnabledControlBox, 0, row, 2, 1);
    }

    private void addInferenceAddressRow(GridPane gridPane, int row) {
        inferenceAddressField.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        inferencePortField.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        inferencePortField.setTextFormatter(UiUtils.createDecimalFormatter());
        inferencePortField.setPrefColumnCount(4);
        final HBox inferenceBox = new HBox(inferenceAddressField, new Label(PORT_LABEL_TEXT), inferencePortField);
        inferenceBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        inferenceBox.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        gridPane.addRow(row, new Label(INFERENCE_ADDRESS_LABEL_TEXT), inferenceBox);
    }

    private void addManagementAddressRow(GridPane gridPane, int row) {
        managementAddressField.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        managementPortField.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        managementPortField.setTextFormatter(UiUtils.createDecimalFormatter());
        managementPortField.setPrefColumnCount(4);
        final HBox managementBox = new HBox(managementAddressField, new Label(PORT_LABEL_TEXT), managementPortField);
        managementBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        managementBox.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        gridPane.addRow(row, new Label(MANAGEMENT_ADDRESS_LABEL_TEXT), managementBox);
    }

    private void addModelSelectionRow(GridPane gridPane, int row) {
        selectModelButton.disableProperty().bind(managementAddressField.textProperty().isEmpty()
                                                                       .or(managementPortField.textProperty()
                                                                                              .isEmpty()));

        final HBox modelSelectionBox = new HBox(selectedModelLabel, selectModelButton);
        modelSelectionBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        modelSelectionBox.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        gridPane.addRow(row, new Label(MODEL_LABEL_TEXT), modelSelectionBox);
        selectedModelLabel.disableProperty().bind(selectedModelLabel.textProperty().isEqualTo(NO_MODEL_SELECTED_TEXT));
        selectedModelLabel.setId(SELECTED_MODEL_LABEL_ID);
    }

    private void addMinimumPredictionScoreRow(GridPane gridPane, int row) {
        minimumScoreControl.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        minimumScoreControl.setEditable(true);
        gridPane.addRow(row, new Label(MINIMUM_SCORE_LABEL_TEXT), minimumScoreControl);
    }

    private void addImageResizePreprocessingChoiceRow(GridPane gridPane, int row) {
        gridPane.addRow(row, new Label(RESIZE_IMAGES_LABEL_TEXT), resizeImagesControl);
        resizeImagesControl.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
    }

    private void addImageResizePreprocessingSetupRow(GridPane gridPane, int row) {
        maxImageWidthField.setTextFormatter(UiUtils.createDecimalFormatter());
        maxImageWidthField.setPrefColumnCount(4);
        maxImageHeightField.setTextFormatter(UiUtils.createDecimalFormatter());
        maxImageHeightField.setPrefColumnCount(4);
        final HBox imageMaxSizeBox = new HBox(new Label(RESIZE_IMAGES_WIDTH_LABEL_TEXT),
                                              maxImageWidthField,
                                              new Label(RESIZE_IMAGES_HEIGHT_LABEL_TEXT),
                                              maxImageHeightField);
        imageMaxSizeBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        imageMaxSizeBox.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty())
                                                       .or(Bindings.not(resizeImagesControl.selectedProperty())));
        gridPane.addRow(row, new Label(RESIZE_IMAGES_SIZE_LABEL_TEXT), imageMaxSizeBox);
    }

    private void addImageResizePreprocessingRatioRow(GridPane gridPane, int row) {
        keepImageRatio.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty())
                                                      .or(Bindings.not(resizeImagesControl.selectedProperty())));
        gridPane.addRow(row, new Label(RESIZE_IMAGES_KEEP_RATIO_LABEL_TEXT), keepImageRatio);
    }

    private void addPredictionMergeCategoryChoiceRow(GridPane gridPane, int row) {
        mergeCategoriesControl.disableProperty().bind(Bindings.not(inferenceEnabledControl.selectedProperty()));
        gridPane.addRow(row, new Label(MERGE_CATEGORIES_LABEL_TEXT), mergeCategoriesControl);
    }
}
