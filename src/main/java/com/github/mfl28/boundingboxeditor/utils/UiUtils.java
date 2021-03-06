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
package com.github.mfl28.boundingboxeditor.utils;

import javafx.scene.control.ButtonType;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import org.controlsfx.dialog.ProgressDialog;

/***
 * A class that comprises general ui utility-functions.
 */
public class UiUtils {
    private UiUtils() {
        throw new IllegalStateException("UiUtils class");
    }

    /***
     * Creates a pane that fills any available horizontal space
     * in it's parent.
     * @return pane
     */
    public static Pane createHSpacer() {
        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Creates a Tooltip.
     *
     * @param text the text content of the tooltip
     * @return the tooltip
     */
    public static Tooltip createTooltip(String text) {
        return new Tooltip(text);
    }

    /**
     * Creates a Tooltip with a key combination.
     *
     * @param text           the text content of the tooltip
     * @param keyCombination a key combination which will be displayed besides the text
     * @return the tooltip
     */
    public static Tooltip createTooltip(String text, KeyCombination keyCombination) {
        return new Tooltip((text.isEmpty() ? "" : (text + " "))
                                   + "(" + keyCombination.getDisplayText() + ")");
    }

    /**
     * Creates a Tooltip showing the key combination to focus the node it is installed on.
     *
     * @param keyCombination a key combination which will be displayed besides the text
     * @return the tooltip
     */
    public static Tooltip createFocusTooltip(KeyCombination keyCombination) {
        return new Tooltip("(" + keyCombination.getDisplayText() + " to focus)");
    }

    /**
     * Creates a text formatter that only matches decimal number inputs.
     *
     * @return the formatter
     */
    public static TextFormatter<String> createDecimalFormatter() {
        return new TextFormatter<>(change -> {
            if(change.getText().matches("[0-9]*")) {
                return change;
            }

            return null;
        });
    }

    public static TextFormatter<String> createFloatFormatter() {
        return new TextFormatter<>(change -> {
            if(!change.getControlNewText().equals(".") && change.getControlNewText().matches("[0-9]*\\.?[0-9]*")) {
                return change;
            }

            return null;
        });
    }

    public static void closeProgressDialog(ProgressDialog dialog) {
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.close();
        dialog.getDialogPane().getButtonTypes().remove(ButtonType.CLOSE);
    }
}
