/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import javafx.scene.control.ButtonBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

/**
 * The {@link DialogService} showing the application's JavaFX dialogs.
 */
public class JavaFxDialogService implements DialogService {
    @Override
    public void displayErrorAlert(String title, String content, Window owner) {
        MainView.displayErrorAlert(title, content, owner);
    }

    @Override
    public ButtonBar.ButtonData displayYesNoCancelDialogAndGetResult(String title, String content, Window owner) {
        return MainView.displayYesNoCancelDialogAndGetResult(title, content, owner);
    }

    @Override
    public ButtonBar.ButtonData displayYesNoDialogAndGetResult(String title, String content, Window owner) {
        return MainView.displayYesNoDialogAndGetResult(title, content, owner);
    }

    @Override
    public File displayDirectoryChooserAndGetChoice(String title, Stage stage, File initialDirectory) {
        return MainView.displayDirectoryChooserAndGetChoice(title, stage, initialDirectory);
    }

    @Override
    public File displayFileChooserAndGetChoice(String title, Window window, File initialDirectory,
                                               String initialFileName, FileChooser.ExtensionFilter extensionFilter,
                                               MainView.FileChooserType type) {
        return MainView.displayFileChooserAndGetChoice(title, window, initialDirectory, initialFileName,
                extensionFilter, type);
    }

    @Override
    public void displayIOResultErrorInfoAlert(IOResult ioResult, Window owner) {
        MainView.displayIOResultErrorInfoAlert(ioResult, owner);
    }

    @Override
    public <T> Optional<T> displayChoiceDialogAndGetResult(T defaultChoice, Collection<T> choices, String title,
                                                           String header, String content, Window owner) {
        return MainView.displayChoiceDialogAndGetResult(defaultChoice, choices, title, header, content, owner);
    }

    @Override
    public void displayExceptionDialog(Throwable throwable, Window owner) {
        MainView.displayExceptionDialog(throwable, owner);
    }

    @Override
    public void displayTextInfoDialog(String title, String header, String content, Window owner) {
        MainView.displayTextInfoDialog(title, header, content, owner);
    }
}
