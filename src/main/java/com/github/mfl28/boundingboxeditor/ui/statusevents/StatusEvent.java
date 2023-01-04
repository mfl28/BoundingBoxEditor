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
package com.github.mfl28.boundingboxeditor.ui.statusevents;

import com.github.mfl28.boundingboxeditor.ui.StatusBarView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Abstract base class that represents a status event that can be registered and displayed
 * in a {@link StatusBarView StatusBarView}.
 */
public abstract class StatusEvent {
    static final DecimalFormat secondsFormat = new DecimalFormat("0.0##",
                                                                 DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private final String eventMessage;

    /**
     * Base class constructor
     *
     * @param eventMessage the message that should be displayed to describe the event
     */
    StatusEvent(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    /**
     * Returns the event's message.
     *
     * @return the event-message
     */
    public String getEventMessage() {
        return eventMessage;
    }

    @Override
    public String toString() {
        return "StatusEvent: " + eventMessage;
    }
}
