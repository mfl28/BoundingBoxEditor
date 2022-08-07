/*
 * Copyright (C) 2021 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import java.text.DecimalFormat;
import java.text.ParseException;

import com.github.mfl28.boundingboxeditor.ui.EditorSettingsConfig;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

public class EditorSettingsView extends GridPane implements ApplyButtonChangeProvider {
    private static final String GRID_PANE_STYLE_CLASS = "grid-pane";
    private static final String SIMPLIFY_RELATIVE_DISTANCE_TOLERANCE_TEXT = "Distance tolerance";
    private static final String SIMPLIFY_RELATIVE_DISTANCE_TOLERANCE_TOOLTIP =
            "Set the relative distance tolerance for polygon simplification.";
    private final CheckBox autoSimplifyPolygonsControl = new CheckBox();
    private final Spinner<Double> simplifyRelativeDistanceTolerance = new Spinner<>(0.0, 1.0, 0.1, 0.05);

    public EditorSettingsView() {
        getStyleClass().add(GRID_PANE_STYLE_CLASS);
        setUpContent();
        final ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(new ColumnConstraints(), columnConstraints);
    }

    public void setDisplayedSettingsFromEditorSettingsConfig(EditorSettingsConfig config) {
        autoSimplifyPolygonsControl.setSelected(config.isAutoSimplifyPolygons());
        simplifyRelativeDistanceTolerance.getValueFactory().setValue(config.getSimplifyRelativeDistanceTolerance());
    }

    public void applyDisplayedSettingsToEditorSettingsConfig(EditorSettingsConfig config) {
        config.setAutoSimplifyPolygons(autoSimplifyPolygonsControl.isSelected());

        if (!autoSimplifyPolygonsControl.isSelected()) {
            return;
        }

        config.setSimplifyRelativeDistanceTolerance(simplifyRelativeDistanceTolerance.getValue());
    }

    @Override
    public void registerPropertyListeners(Button applyButton) {
        autoSimplifyPolygonsControl.selectedProperty().addListener(
                (observable, oldValue, newValue) -> applyButton.setDisable(false));
        simplifyRelativeDistanceTolerance.valueProperty()
                .addListener((observable, oldValue, newValue) -> applyButton.setDisable(false));
    }

    public Spinner<Double> getSimplifyRelativeDistanceToleranceSpinner() {
        return simplifyRelativeDistanceTolerance;
    }

    private void setUpContent() {
        add(UiUtils.createSettingsTitleRow("Polygons", "subgroup-title-label", "settings-subgroup-box"), 0, 0, 2, 1);

        final Label autoSimplifyPolygonsPopover = new Label("Auto-simplify polygons");
        Tooltip.install(autoSimplifyPolygonsControl, UiUtils.createTooltip(
                "Automatically simplify polygons when freehand drawing."));

        addRow(1, autoSimplifyPolygonsPopover, autoSimplifyPolygonsControl);

        final Label simplifyRelativeDistanceTolerancePopover = new Label(SIMPLIFY_RELATIVE_DISTANCE_TOLERANCE_TEXT);
        Tooltip.install(simplifyRelativeDistanceTolerancePopover, UiUtils.createTooltip(
                SIMPLIFY_RELATIVE_DISTANCE_TOLERANCE_TOOLTIP));

        StringConverter<Double> doubleConverter = new StringConverter<Double>() {
            private final DecimalFormat df = new DecimalFormat("#.##");
            @Override
            public String toString(Double object) {
                if (object == null) {return "";}
                return df.format(object);}
            @Override
            public Double fromString(String string) {
                try {
                    if (string == null) {return null;}
                    string = string.trim();
                    if (string.length() < 1) {return null;}     
                    return df.parse(string).doubleValue();
                } catch (ParseException ex) {throw new RuntimeException(ex);}
                }
        };

        simplifyRelativeDistanceTolerance.getValueFactory().setConverter(doubleConverter);
        simplifyRelativeDistanceTolerance.setEditable(true);
        simplifyRelativeDistanceTolerance.getEditor().setTextFormatter(UiUtils.createFloatFormatter());
        simplifyRelativeDistanceTolerance.visibleProperty().bind(autoSimplifyPolygonsControl.selectedProperty());

        addRow(2, simplifyRelativeDistanceTolerancePopover, simplifyRelativeDistanceTolerance);
    }
}
