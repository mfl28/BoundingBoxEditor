/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

@Tag("unit")
class KeyCombinationEventHandlerTests {
    @Test
    void checkKeyCombinationEventHandlerWithNonNullHandlers() {
        KeyCodeCombination testCombination = new KeyCodeCombination(KeyCode.A, KeyCombination.SHIFT_DOWN);

        AtomicInteger numPressedHandled = new AtomicInteger();
        AtomicInteger numReleasedHandled = new AtomicInteger();

        KeyCombinationEventHandler keyCombinationEventHandler = new KeyCombinationEventHandler(
                testCombination,
                event -> numPressedHandled.addAndGet(1),
                event -> numReleasedHandled.addAndGet(1)
        );

        Assertions.assertTrue(keyCombinationEventHandler.hasOnPressedHandler());
        Assertions.assertTrue(keyCombinationEventHandler.hasOnReleasedHandler());
        Assertions.assertEquals(keyCombinationEventHandler.getKeyCombination(), testCombination);

        keyCombinationEventHandler.onPressed(null);
        Assertions.assertEquals(1, numPressedHandled.get());

        keyCombinationEventHandler.onPressed(null);
        Assertions.assertEquals(2, numPressedHandled.get());

        keyCombinationEventHandler.onReleased(null);
        Assertions.assertEquals(1, numReleasedHandled.get());
    }

    @Test
    void checkKeyCombinationEventHandlerWithNullHandlers() {
        KeyCodeCombination testCombination = new KeyCodeCombination(KeyCode.A, KeyCombination.SHIFT_DOWN);

        KeyCombinationEventHandler keyCombinationEventHandler = new KeyCombinationEventHandler(
                testCombination, null, null
        );

        Assertions.assertFalse(keyCombinationEventHandler.hasOnPressedHandler());
        Assertions.assertFalse(keyCombinationEventHandler.hasOnReleasedHandler());
        Assertions.assertEquals(keyCombinationEventHandler.getKeyCombination(), testCombination);

        keyCombinationEventHandler.onPressed(null);
        keyCombinationEventHandler.onReleased(null);
    }

    @Test
    void checkSingleFireKeyCombinationEventHandlerWithNonNullHandlers() {
        KeyCodeCombination testCombination = new KeyCodeCombination(KeyCode.A, KeyCombination.SHIFT_DOWN);

        AtomicInteger numPressedHandled = new AtomicInteger();
        AtomicInteger numReleasedHandled = new AtomicInteger();

        SingleFireKeyCombinationEventHandler keyCombinationEventHandler = new SingleFireKeyCombinationEventHandler(
                testCombination,
                event -> numPressedHandled.addAndGet(1),
                event -> numReleasedHandled.addAndGet(1)
        );

        Assertions.assertTrue(keyCombinationEventHandler.hasOnPressedHandler());
        Assertions.assertTrue(keyCombinationEventHandler.hasOnReleasedHandler());
        Assertions.assertEquals(keyCombinationEventHandler.getKeyCombination(), testCombination);

        keyCombinationEventHandler.onPressed(null);
        Assertions.assertEquals(1, numPressedHandled.get());

        keyCombinationEventHandler.onPressed(null);
        Assertions.assertEquals(1, numPressedHandled.get());

        keyCombinationEventHandler.onReleased(null);
        Assertions.assertEquals(1, numReleasedHandled.get());

        keyCombinationEventHandler.onPressed(null);
        Assertions.assertEquals(2, numPressedHandled.get());
    }

    @Test
    void checkSingleFireKeyCombinationEventHandlerWithNullHandlers() {
        KeyCodeCombination testCombination = new KeyCodeCombination(KeyCode.A, KeyCombination.SHIFT_DOWN);

        SingleFireKeyCombinationEventHandler keyCombinationEventHandler = new SingleFireKeyCombinationEventHandler(
                testCombination, null, null
        );

        Assertions.assertTrue(keyCombinationEventHandler.hasOnPressedHandler());
        Assertions.assertTrue(keyCombinationEventHandler.hasOnReleasedHandler());
        Assertions.assertEquals(keyCombinationEventHandler.getKeyCombination(), testCombination);

        keyCombinationEventHandler.onPressed(null);
        keyCombinationEventHandler.onReleased(null);
    }
}
