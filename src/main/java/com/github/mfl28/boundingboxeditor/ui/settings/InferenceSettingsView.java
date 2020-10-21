package com.github.mfl28.boundingboxeditor.ui.settings;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.ui.View;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.apache.commons.lang3.math.NumberUtils;
import org.controlsfx.control.ToggleSwitch;

public class InferenceSettingsView extends GridPane implements View, ApplyButtonChangeProvider {
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
    private static final String RESIZE_IMAGES_LABEL_TEXT = "Resize images";
    private static final String RESIZE_IMAGES_WIDTH_LABEL_TEXT = "Width";
    private static final String RESIZE_IMAGES_HEIGHT_LABEL_TEXT = "Height";
    private static final String RESIZE_IMAGES_KEEP_RATIO_LABEL_TEXT = "Keep ratio";
    private static final String MERGE_CATEGORIES_LABEL_TEXT = "Merge categories";
    private static final String SUBGROUP_TITLE_LABEL_ID = "subgroup-title-label";
    private static final String SETTINGS_SUBGROUP_BOX_ID = "settings-subgroup-box";
    private static final String SERVERS_SUBGROUP_TITLE = "Torch serve";
    private static final String PREDICTION_SUBGROUP_TITLE = "Prediction";
    private static final String PREPROCESSING_SUBGROUP_TITLE = "Preprocessing";
    private static final String INFERENCE_ENABLE_TOOLTIP = "Enable bounding box predictions";
    private static final String SELECT_MODEL_TOOLTIP =
            "Select the prediction model from the models registered with the server";
    private static final String MODEL_TOOLTIP = "Model to be used for predictions";
    private static final String MINIMUM_SCORE_TOOLTIP = "Score threshold for bounding box predictions";
    private static final String MERGE_CATEGORIES_TOOLTIP =
            "Merge categories of predictions with existing categories case-insensitively";
    private static final String RESIZE_IMAGES_TOOLTIP = "Resize images before performing prediction";
    private static final String INFERENCE_PORT_TOOLTIP = "Inference server port";
    private static final String INFERENCE_ADDRESS_TOOLTIP = "Inference server address";
    private static final String MANAGEMENT_PORT_TOOLTIP = "Management server port";
    private static final String MANAGEMENT_ADDRESS_TOOLTIP = "Management server address";
    private static final PseudoClass invalidValuePseudoClass = PseudoClass.getPseudoClass("invalid-value");
    private final Button selectModelButton = new Button("Select");
    private final Label selectedModelLabel = new Label(NO_MODEL_SELECTED_TEXT);
    private final ToggleSwitch inferenceEnabledControl = new ToggleSwitch();
    private final TextField inferenceAddressField = new TextField();
    private final TextField inferencePortField = new TextField();
    private final TextField managementAddressField = new TextField();
    private final TextField managementPortField = new TextField();
    private final Spinner<Double> minimumScoreControl = new Spinner<>(0.0, 1.0, 0.0, 0.05);
    private final CheckBox resizeImagesControl = new CheckBox();
    private final TextField imageResizeWidthField = new TextField();
    private final TextField imageResizeHeightField = new TextField();
    private final CheckBox keepImageRatioControl = new CheckBox();
    private final CheckBox mergeCategoriesControl = new CheckBox();

    InferenceSettingsView() {
        getStyleClass().add(GRID_PANE_STYLE_CLASS);
        setUpContent();
        final ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(new ColumnConstraints(), columnConstraints);
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

    public boolean validateSettings() {
        boolean allValid = true;

        if(!inferenceEnabledControl.isSelected()) {
            return true;
        }

        if(inferenceAddressField.getText().isBlank()) {
            inferenceAddressField.pseudoClassStateChanged(invalidValuePseudoClass, true);
            allValid = false;
        } else {
            inferenceAddressField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        }

        if(!NumberUtils.isParsable(inferencePortField.getText())) {
            inferencePortField.pseudoClassStateChanged(invalidValuePseudoClass, true);
            allValid = false;
        } else {
            inferencePortField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        }

        if(managementAddressField.getText().isBlank()) {
            managementAddressField.pseudoClassStateChanged(invalidValuePseudoClass, true);
            allValid = false;
        } else {
            managementAddressField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        }

        if(!NumberUtils.isParsable(managementPortField.getText())) {
            managementPortField.pseudoClassStateChanged(invalidValuePseudoClass, true);
            allValid = false;
        } else {
            managementPortField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        }

        if(minimumScoreControl.getEditor().getText().isBlank()) {
            minimumScoreControl.getEditor().pseudoClassStateChanged(invalidValuePseudoClass, true);
            allValid = false;
        } else {
            minimumScoreControl.getEditor().pseudoClassStateChanged(invalidValuePseudoClass, false);
        }

        if(resizeImagesControl.isSelected()) {
            if(!NumberUtils.isParsable(imageResizeWidthField.getText())) {
                imageResizeWidthField.pseudoClassStateChanged(invalidValuePseudoClass, true);
                allValid = false;
            } else {
                imageResizeWidthField.pseudoClassStateChanged(invalidValuePseudoClass, false);
            }

            if(!NumberUtils.isParsable(imageResizeHeightField.getText())) {
                imageResizeHeightField.pseudoClassStateChanged(invalidValuePseudoClass, true);
                allValid = false;
            } else {
                imageResizeHeightField.pseudoClassStateChanged(invalidValuePseudoClass, false);
            }
        }

        return allValid;
    }

    public void setAllFieldsValid() {
        inferenceAddressField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        inferencePortField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        managementAddressField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        managementPortField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        minimumScoreControl.getEditor().pseudoClassStateChanged(invalidValuePseudoClass, false);
        imageResizeWidthField.pseudoClassStateChanged(invalidValuePseudoClass, false);
        imageResizeHeightField.pseudoClassStateChanged(invalidValuePseudoClass, false);
    }

    public void applyDisplayedSettingsToPredictorClientConfig(BoundingBoxPredictorClientConfig config) {
        if(!inferenceEnabledControl.isSelected()) {
            return;
        }

        config.setInferenceUrl(inferenceAddressField.getText());
        config.setInferencePort(Integer.parseUnsignedInt(inferencePortField.getText()));
        config.setManagementUrl(managementAddressField.getText());
        config.setManagementPort(Integer.parseUnsignedInt(managementPortField.getText()));

        if(selectedModelLabel.getText().equals(NO_MODEL_SELECTED_TEXT)) {
            config.setInferenceModelName(null);
        } else {
            config.setInferenceModelName(selectedModelLabel.getText());
        }
    }

    public void applyDisplayedSettingsToPredictorConfig(BoundingBoxPredictorConfig config) {
        config.setInferenceEnabled(inferenceEnabledControl.isSelected());

        if(!inferenceEnabledControl.isSelected()) {
            return;
        }

        config.setMinimumScore(minimumScoreControl.getValue());
        config.setMergeCategories(mergeCategoriesControl.isSelected());
        config.setResizeImages(resizeImagesControl.isSelected());

        if(resizeImagesControl.isSelected()) {
            config.setImageResizeWidth(Integer.parseInt(imageResizeWidthField.getText()));
            config.setImageResizeHeight(Integer.parseInt(imageResizeHeightField.getText()));
            config.setImageResizeKeepRatio(keepImageRatioControl.isSelected());
        }
    }

    public void setDisplayedSettingsFromPredictorConfig(BoundingBoxPredictorConfig config) {
        minimumScoreControl.getValueFactory().setValue(config.getMinimumScore());
        inferenceEnabledControl.setSelected(config.isInferenceEnabled());
        resizeImagesControl.setSelected(config.isResizeImages());
        imageResizeWidthField.setText(Integer.toString(config.getImageResizeWidth()));
        imageResizeHeightField.setText(Integer.toString(config.getImageResizeHeight()));
        keepImageRatioControl.setSelected(config.getImageResizeKeepRatio());
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
        imageResizeWidthField.textProperty()
                             .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        imageResizeHeightField.textProperty()
                              .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
        keepImageRatioControl.selectedProperty()
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

    public CheckBox getResizeImagesControl() {
        return resizeImagesControl;
    }

    public TextField getImageResizeWidthField() {
        return imageResizeWidthField;
    }

    public TextField getImageResizeHeightField() {
        return imageResizeHeightField;
    }

    public CheckBox getKeepImageRatioControl() {
        return keepImageRatioControl;
    }

    public CheckBox getMergeCategoriesControl() {
        return mergeCategoriesControl;
    }

    private void setUpContent() {
        int rowIndex = -1;

        addInferenceControlRow(++rowIndex);
        addSubgroupTitleRow(SERVERS_SUBGROUP_TITLE, ++rowIndex);
        addInferenceAddressRow(++rowIndex);
        addManagementAddressRow(++rowIndex);
        addModelSelectionRow(++rowIndex);
        addSubgroupTitleRow(PREDICTION_SUBGROUP_TITLE, ++rowIndex);
        addMinimumPredictionScoreRow(++rowIndex);
        addPredictionMergeCategoryChoiceRow(++rowIndex);
        addSubgroupTitleRow(PREPROCESSING_SUBGROUP_TITLE, ++rowIndex);
        addImageResizePreprocessingSetupRow(++rowIndex);
    }

    private void addInferenceControlRow(int row) {
        inferenceEnabledControl.setSelected(false);
        inferenceEnabledControl.setText("  ");

        final Label inferenceEnabledLabel = new Label(ENABLE_INFERENCE_LABEL_TEXT);
        Tooltip.install(inferenceEnabledLabel,
                        UiUtils.createTooltip(INFERENCE_ENABLE_TOOLTIP));

        final HBox inferenceEnabledControlBox = new HBox(inferenceEnabledLabel,
                                                         inferenceEnabledControl);
        inferenceEnabledControlBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        add(inferenceEnabledControlBox, 0, row, 2, 1);
    }

    private void addInferenceAddressRow(int row) {
        inferencePortField.setTextFormatter(UiUtils.createDecimalFormatter());
        inferencePortField.setPrefColumnCount(4);

        final Label inferencePortLabel = new Label(PORT_LABEL_TEXT);
        Tooltip.install(inferencePortLabel, UiUtils.createTooltip(INFERENCE_PORT_TOOLTIP));

        final HBox inferenceBox = new HBox(inferenceAddressField, inferencePortLabel, inferencePortField);
        inferenceBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        inferenceBox.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        final Label inferenceAddressLabel = new Label(INFERENCE_ADDRESS_LABEL_TEXT);
        Tooltip.install(inferenceAddressLabel, UiUtils.createTooltip(INFERENCE_ADDRESS_TOOLTIP));
        inferenceAddressLabel.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        addRow(row, inferenceAddressLabel, inferenceBox);
    }

    private void addManagementAddressRow(int row) {
        managementPortField.setTextFormatter(UiUtils.createDecimalFormatter());
        managementPortField.setPrefColumnCount(4);

        final Label managementPortLabel = new Label(PORT_LABEL_TEXT);
        Tooltip.install(managementPortLabel, UiUtils.createTooltip(MANAGEMENT_PORT_TOOLTIP));

        final HBox managementBox = new HBox(managementAddressField, managementPortLabel, managementPortField);
        managementBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        managementBox.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        final Label managementAddressLabel = new Label(MANAGEMENT_ADDRESS_LABEL_TEXT);
        Tooltip.install(managementAddressLabel, UiUtils.createTooltip(MANAGEMENT_ADDRESS_TOOLTIP));
        managementAddressLabel.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        addRow(row, managementAddressLabel, managementBox);
    }

    private void addModelSelectionRow(int row) {
        selectModelButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> managementAddressField.getText().isBlank() || managementPortField.getText().isBlank(),
                        managementAddressField.textProperty(),
                        managementPortField.textProperty()));

        Tooltip.install(selectModelButton, UiUtils.createTooltip(SELECT_MODEL_TOOLTIP));

        final HBox modelSelectionBox = new HBox(selectedModelLabel, selectModelButton);
        modelSelectionBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        modelSelectionBox.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        final Label modelLabel = new Label(MODEL_LABEL_TEXT);
        Tooltip.install(modelLabel, UiUtils.createTooltip(MODEL_TOOLTIP));
        modelLabel.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        addRow(row, modelLabel, modelSelectionBox);
        selectedModelLabel.disableProperty().bind(selectedModelLabel.textProperty().isEqualTo(NO_MODEL_SELECTED_TEXT));
        selectedModelLabel.setId(SELECTED_MODEL_LABEL_ID);
    }

    private void addMinimumPredictionScoreRow(int row) {
        minimumScoreControl.visibleProperty().bind(inferenceEnabledControl.selectedProperty());
        minimumScoreControl.setEditable(true);
        minimumScoreControl.getEditor().setTextFormatter(UiUtils.createFloatFormatter());

        final Label minimumScoreLabel = new Label(MINIMUM_SCORE_LABEL_TEXT);
        Tooltip.install(minimumScoreLabel, UiUtils.createTooltip(MINIMUM_SCORE_TOOLTIP));
        minimumScoreLabel.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        addRow(row, minimumScoreLabel, minimumScoreControl);
    }

    private void addImageResizePreprocessingSetupRow(int row) {
        imageResizeWidthField.setTextFormatter(UiUtils.createDecimalFormatter());
        imageResizeWidthField.setPrefColumnCount(4);
        imageResizeHeightField.setTextFormatter(UiUtils.createDecimalFormatter());
        imageResizeHeightField.setPrefColumnCount(4);
        final HBox imageSizeBox = new HBox(new Label(RESIZE_IMAGES_WIDTH_LABEL_TEXT),
                                           imageResizeWidthField,
                                           new Label(RESIZE_IMAGES_HEIGHT_LABEL_TEXT),
                                           imageResizeHeightField,
                                           new Label(RESIZE_IMAGES_KEEP_RATIO_LABEL_TEXT),
                                           keepImageRatioControl);
        imageSizeBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        imageSizeBox.visibleProperty().bind(resizeImagesControl.selectedProperty());

        final HBox controlBox = new HBox(resizeImagesControl, imageSizeBox);
        controlBox.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        controlBox.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        final Label resizeImagesLabel = new Label(RESIZE_IMAGES_LABEL_TEXT);
        Tooltip.install(resizeImagesLabel, UiUtils.createTooltip(RESIZE_IMAGES_TOOLTIP));
        resizeImagesLabel.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        addRow(row, resizeImagesLabel, controlBox);
    }

    private void addPredictionMergeCategoryChoiceRow(int row) {
        mergeCategoriesControl.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        final Label mergeCategoriesLabel = new Label(MERGE_CATEGORIES_LABEL_TEXT);
        Tooltip.install(mergeCategoriesLabel, UiUtils.createTooltip(MERGE_CATEGORIES_TOOLTIP));
        mergeCategoriesLabel.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        addRow(row, mergeCategoriesLabel, mergeCategoriesControl);
    }

    private void addSubgroupTitleRow(String title, int row) {
        final Label titleLabel = new Label(title);
        titleLabel.setId(SUBGROUP_TITLE_LABEL_ID);
        final Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);
        HBox.setHgrow(separator, Priority.ALWAYS);

        final HBox box = new HBox(titleLabel, separator);
        box.setId(SETTINGS_SUBGROUP_BOX_ID);
        box.visibleProperty().bind(inferenceEnabledControl.selectedProperty());

        add(box, 0, row, 2, 1);
    }
}
