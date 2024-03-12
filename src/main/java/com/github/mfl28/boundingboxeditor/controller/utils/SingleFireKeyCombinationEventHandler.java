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

import java.util.concurrent.atomic.AtomicBoolean;

public class SingleFireKeyCombinationEventHandler extends KeyCombinationEventHandler {

    private final AtomicBoolean canFire = new AtomicBoolean(true);

    public SingleFireKeyCombinationEventHandler(KeyCombination keyCombination, EventHandler<KeyEvent> onPressedHandler, EventHandler<KeyEvent> onReleasedHandler) {
        super(keyCombination, onPressedHandler, onReleasedHandler);
    }

    @Override
    public boolean hasOnPressedHandler() {
        return true;
    }

    @Override
    public boolean hasOnReleasedHandler() {
        return true;
    }

    @Override
    public void onPressed(KeyEvent event) {
        if(canFire.compareAndExchange(true, false)) {
            super.onPressed(event);
        }
    }

    @Override
    public void onReleased(KeyEvent event) {
        canFire.set(true);
        super.onReleased(event);
    }
}
