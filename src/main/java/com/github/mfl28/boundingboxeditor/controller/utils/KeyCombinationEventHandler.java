/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.controller.utils;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.function.Function;

public class KeyCombinationEventHandler {
    private final KeyCombination keyCombination;
    private final EventHandler<KeyEvent> onPressedHandler;
    private final EventHandler<KeyEvent> onReleasedHandler;
    private final Function<KeyEvent, Boolean> releaseMatcher;

    public KeyCombinationEventHandler(
            KeyCombination keyCombination,
            EventHandler<KeyEvent> onPressedHandler,
            EventHandler<KeyEvent> onReleasedHandler,
            Function<KeyEvent, Boolean> releaseMatcher) {
        this.keyCombination = keyCombination;
        this.onPressedHandler = onPressedHandler;
        this.onReleasedHandler = onReleasedHandler;
        this.releaseMatcher = releaseMatcher;
    }

    public KeyCombinationEventHandler(
            KeyCombination keyCombination,
            EventHandler<KeyEvent> onPressedHandler,
            EventHandler<KeyEvent> onReleasedHandler) {
        this(
                keyCombination,
                onPressedHandler,
                onReleasedHandler,
                null
        );
    }


    public KeyCombination getKeyCombination() {
        return keyCombination;
    }

    public boolean hasOnPressedHandler() {
        return onPressedHandler != null;
    }

    public boolean hasOnReleasedHandler() {
        return onReleasedHandler != null;
    }

    public void onPressed(KeyEvent event) {
        if(this.onPressedHandler != null) {
            this.onPressedHandler.handle(event);
        }
    }

    public void onReleased(KeyEvent event) {
        if(this.onReleasedHandler != null) {
            this.onReleasedHandler.handle(event);
        }
    }

    public boolean matchPressed(KeyEvent event) {
        return keyCombination.match(event);
    }

    public boolean matchReleased(KeyEvent event) {
        if(releaseMatcher != null) {
            return releaseMatcher.apply(event);
        } else {
            return keyCombination.match(event);
        }
    }

    public boolean handlesPressed(KeyEvent event) {
        return hasOnPressedHandler() && matchPressed(event);
    }

    public boolean handlesReleased(KeyEvent event) {
        return hasOnReleasedHandler() && matchReleased(event);
    }
}
