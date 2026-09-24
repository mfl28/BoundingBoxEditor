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

import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.controlsfx.dialog.ProgressDialog;

public class ServiceProgressDialog extends ProgressDialog implements ProgressViewer {
    private final ObjectProperty<Window> parentWindow = new SimpleObjectProperty<>();
    private final Worker<?> worker;

    public ServiceProgressDialog(Worker<?> worker) {
        super(worker);
        this.worker = worker;
        MainView.applyDialogStyle(this);
        setUpInternalListeners();
    }

    /**
     * Adds a cancel button to the dialog. Closing the dialog while the worker is still running
     * (via this button, the escape key or the window's close button) cancels the worker.
     */
    public void enableCancellation() {
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        setOnHidden(event -> {
            if(worker.isRunning()) {
                worker.cancel();
            }
        });
    }

    @Override
    public void setOwnerParentWindow(Window parentWindow) {
        this.parentWindow.set(parentWindow);
        initOwner(parentWindow);
    }

    @Override
    public void hideProgress() {
        UiUtils.closeProgressDialog(this);
    }

    private void setUpInternalListeners() {
        setOnShown(event -> {
            if(parentWindow.get() != null && parentWindow.get().isShowing()) {
                setX(parentWindow.get().getX() + (parentWindow.get().getWidth() - getWidth()) / 2.0);
                setY(parentWindow.get().getY() + (parentWindow.get().getHeight() - getHeight()) / 2.0);
            }
        });
    }
}
