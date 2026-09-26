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
import javafx.concurrent.Service;
import javafx.scene.control.ButtonBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

/**
 * Shows the (blocking) dialogs the controller needs to interact with the user. Having the controller use this
 * interface instead of static dialog methods allows replacing the dialogs, e.g. with a fake in tests.
 */
public interface DialogService {
    /**
     * Displays an error alert.
     *
     * @param title   the title of the dialog
     * @param content the text-content of the dialog
     * @param owner   the owner window of the dialog
     */
    void displayErrorAlert(String title, String content, Window owner);

    /**
     * Displays a dialog with 'Yes', 'No' and 'Cancel' buttons and returns the chosen option.
     *
     * @param title   the title of the dialog
     * @param content the text-content of the dialog
     * @param owner   the owner window of the dialog
     * @return {@link ButtonBar.ButtonData}.YES/NO/CANCEL_CLOSE
     */
    ButtonBar.ButtonData displayYesNoCancelDialogAndGetResult(String title, String content, Window owner);

    /**
     * Displays a dialog with 'Yes' and 'No' buttons and returns the chosen option.
     *
     * @param title   the title of the dialog
     * @param content the text-content of the dialog
     * @param owner   the owner window of the dialog
     * @return {@link ButtonBar.ButtonData}.YES/NO
     */
    ButtonBar.ButtonData displayYesNoDialogAndGetResult(String title, String content, Window owner);

    /**
     * Displays a directory chooser and returns the chosen directory.
     *
     * @param title            the title of the directory chooser
     * @param stage            the stage on top of which the chooser is shown
     * @param initialDirectory the initially shown directory
     * @return the chosen directory, or null if the user closed the chooser without choosing
     */
    File displayDirectoryChooserAndGetChoice(String title, Stage stage, File initialDirectory);

    /**
     * Displays a file chooser and returns the chosen file.
     *
     * @param title            the title of the file chooser
     * @param window           the window on top of which the chooser is shown
     * @param initialDirectory the initially shown directory
     * @param initialFileName  the initial file name
     * @param extensionFilter  the extension filter to apply
     * @param type             whether a file should be opened or saved
     * @return the chosen file, or null if the user closed the chooser without choosing
     */
    File displayFileChooserAndGetChoice(String title, Window window, File initialDirectory, String initialFileName,
                                        FileChooser.ExtensionFilter extensionFilter, MainView.FileChooserType type);

    /**
     * Displays the errors of an IO-operation.
     *
     * @param ioResult the result of the IO-operation
     * @param owner    the owner window of the dialog
     */
    void displayIOResultErrorInfoAlert(IOResult ioResult, Window owner);

    /**
     * Displays a dialog with a choice box and returns the user's choice.
     *
     * @param defaultChoice the pre-selected choice
     * @param choices       the available choices
     * @param title         the title of the dialog
     * @param header        the header-text of the dialog
     * @param content       the content-text of the dialog
     * @param owner         the owner window of the dialog
     * @param <T>           the type of the choices
     * @return the user's choice, or an empty optional if the user cancelled
     */
    <T> Optional<T> displayChoiceDialogAndGetResult(T defaultChoice, Collection<T> choices, String title,
                                                    String header, String content, Window owner);

    /**
     * Displays a dialog for a thrown exception.
     *
     * @param throwable the exception
     * @param owner     the owner window of the dialog
     */
    void displayExceptionDialog(Throwable throwable, Window owner);

    /**
     * Displays a dialog with information text.
     *
     * @param title   the title of the dialog
     * @param header  the header-text of the dialog
     * @param content the text-content of the dialog
     * @param owner   the owner window of the dialog
     */
    void displayTextInfoDialog(String title, String header, String content, Window owner);

    /**
     * Creates the dialog showing the progress of a background service.
     *
     * @param service the service
     * @param title   the title of the dialog
     * @param header  the header-text of the dialog
     * @return the progress dialog
     */
    ServiceProgressDialog createServiceProgressDialog(Service<? extends IOResult> service, String title,
                                                      String header);
}
