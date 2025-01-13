/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.ui.EditorSettingsConfig;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

public class EditorSettingsView extends GridPane implements ApplyButtonChangeProvider {
    private static final String GRID_PANE_STYLE_CLASS = "grid-pane";
    private static final String SIMPLIFY_RELATIVE_DISTANCE_TOLERANCE_TEXT = "Simplification tolerance";
    private static final String SIMPLIFY_RELATIVE_DISTANCE_TOLERANCE_TOOLTIP =
            "Set tolerance for polygon simplification (lower tolerance means less simplification)";
    private static final String POLYGONS_ROW_TITLE = "Polygons";
    private static final String POLYGONS_ROW_TITLE_ID = "subgroup-title-label";
    private static final String POLYGONS_ROW_BOX_ID = "settings-subgroup-box";
    private static final String AUTO_SIMPLIFY_LABEL_TEXT = "Auto-simplify freehand-drawn";
    private static final String AUTO_SIMPLIFY_POPOVER_TEXT = "Automatically simplify polygons created using freehand-drawing";

    private static final String SETTINGS_ENTRY_BOX_STYLE_CLASS = "settings-entry-box";
    private final CheckBox autoSimplifyPolygonsControl = new CheckBox();
    private final Slider simplifyToleranceControl = new Slider(0.0, 1.0, 0.1);

    public EditorSettingsView() {
        getStyleClass().add(GRID_PANE_STYLE_CLASS);
        setUpContent();
        final ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(new ColumnConstraints(), columnConstraints);
    }

    public void setDisplayedSettingsFromEditorSettingsConfig(EditorSettingsConfig config) {
        autoSimplifyPolygonsControl.setSelected(config.isAutoSimplifyPolygons());
        simplifyToleranceControl.setValue(config.getSimplifyRelativeDistanceTolerance());
    }

    public void applyDisplayedSettingsToEditorSettingsConfig(EditorSettingsConfig config) {
        config.setAutoSimplifyPolygons(autoSimplifyPolygonsControl.isSelected());
        config.setSimplifyRelativeDistanceTolerance(simplifyToleranceControl.getValue());
    }

    public CheckBox getAutoSimplifyPolygonsControl() {
        return autoSimplifyPolygonsControl;
    }

    public Slider getSimplifyToleranceControl() {
        return simplifyToleranceControl;
    }

    @Override
    public void registerPropertyListeners(Button applyButton) {
        autoSimplifyPolygonsControl.selectedProperty().addListener(
                (observable, oldValue, newValue) -> applyButton.setDisable(false));
        simplifyToleranceControl.valueProperty()
                .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
    }

    private void setUpContent() {
        add(UiUtils.createSettingsTitleRow(POLYGONS_ROW_TITLE, POLYGONS_ROW_TITLE_ID, POLYGONS_ROW_BOX_ID),
                0, 0, 2, 1);

        final Label autoSimplifyLabel = new Label(AUTO_SIMPLIFY_LABEL_TEXT);
        Tooltip.install(autoSimplifyLabel, UiUtils.createTooltip(
                AUTO_SIMPLIFY_POPOVER_TEXT));

        addRow(1, autoSimplifyLabel, autoSimplifyPolygonsControl);

        final Label simplifyToleranceLabel = new Label(SIMPLIFY_RELATIVE_DISTANCE_TOLERANCE_TEXT);
        Tooltip.install(simplifyToleranceLabel, UiUtils.createTooltip(
                SIMPLIFY_RELATIVE_DISTANCE_TOLERANCE_TOOLTIP));

        simplifyToleranceControl.setShowTickMarks(true);
        simplifyToleranceControl.setShowTickLabels(true);
        simplifyToleranceControl.setMajorTickUnit(0.5);
        simplifyToleranceControl.setMinorTickCount(4);
        simplifyToleranceControl.setSnapToTicks(true);
        simplifyToleranceControl.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double object) {
                if(object < 0.25) {
                    return "Low";
                }

                if(object < 0.75) {
                    return "Medium";
                }

                return "High";
            }

            @Override
            public Double fromString(String string) {
                return switch(string) {
                    case "Medium" -> 0.5;
                    case "High" -> 1.0;
                    default -> 0.0;
                };
            }
        });

        final HBox box = new HBox(simplifyToleranceControl);
        box.getStyleClass().add(SETTINGS_ENTRY_BOX_STYLE_CLASS);
        addRow(2, simplifyToleranceLabel, box);
    }
}
