package com.github.mfl28.boundingboxeditor.ui.settings;

import com.github.mfl28.boundingboxeditor.ui.UISettingsConfig;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class UISettingsView extends GridPane implements ApplyButtonChangeProvider {
    private static final String GRID_PANE_STYLE_CLASS = "grid-pane";
    private final CheckBox showObjectPopoverControl = new CheckBox();

    public UISettingsView() {
        getStyleClass().add(GRID_PANE_STYLE_CLASS);
        setUpContent();
        final ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(new ColumnConstraints(), columnConstraints);
    }

    public void setDisplayedSettingsFromUISettingsConfig(UISettingsConfig config) {
        showObjectPopoverControl.setSelected(config.isShowObjectPopover());
    }

    public void applyDisplayedSettingsToUISettingsConfig(UISettingsConfig config) {
        config.setShowObjectPopover(showObjectPopoverControl.isSelected());
    }

    @Override
    public void registerPropertyListeners(Button applyButton) {
        showObjectPopoverControl.selectedProperty()
                                .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
    }

    public CheckBox getShowObjectPopoverControl() {
        return showObjectPopoverControl;
    }

    private void setUpContent() {
        addRow(0, new Label("Show object popover"), showObjectPopoverControl);
    }
}
