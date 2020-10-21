package com.github.mfl28.boundingboxeditor.ui.settings;

import com.github.mfl28.boundingboxeditor.ui.UISettingsConfig;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class UISettingsView extends GridPane implements ApplyButtonChangeProvider {
    private static final String GRID_PANE_STYLE_CLASS = "grid-pane";
    private static final String SHOW_OBJECT_POPOVER_LABEL_TEXT = "Show object popover";
    private static final String SHOW_POPOVER_TOOLTIP =
            "Show an image popover when hovering objects in the Annotated Objects tree.";
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
        final Label showPopoverLabel = new Label(SHOW_OBJECT_POPOVER_LABEL_TEXT);
        Tooltip.install(showPopoverLabel, UiUtils.createTooltip(
                SHOW_POPOVER_TOOLTIP));

        addRow(0, showPopoverLabel, showObjectPopoverControl);
    }
}
