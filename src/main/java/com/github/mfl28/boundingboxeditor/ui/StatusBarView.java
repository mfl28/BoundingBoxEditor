/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.ui.statusevents.StatusEvent;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A UI-element used to display information from {@link StatusEvent}s, as well as the time when the event happened.
 */
public class StatusBarView extends HBox implements View {
    private static final String STATUS_PANEL_ID = "status-panel";
    private static final String EVENT_TIME_STAMP_LABEL_ID = "event-time-stamp-label";

    private final Label timeStampLabel = new Label();
    private final Label eventMessageLabel = new Label();
    private final SavedStatusIndicator savedStatusIndicator = new SavedStatusIndicator();

    /**
     * Creates a new status-bar UI-element used to display information from
     * {@link StatusEvent}s, as well as the time when the event happened.
     */
    StatusBarView() {
        getChildren().addAll(savedStatusIndicator, new HBox(timeStampLabel, eventMessageLabel));
        setId(STATUS_PANEL_ID);
        timeStampLabel.setId(EVENT_TIME_STAMP_LABEL_ID);
        setSpacing(10);

        setAlignment(Pos.CENTER_LEFT);
    }

    public boolean isSavedStatus() {
        return savedStatusIndicator.isSaved();
    }

    public BooleanProperty savedStatusProperty() {
        return savedStatusIndicator.savedProperty();
    }

    /**
     * Sets the {@link StatusEvent} object whose information to display.
     *
     * @param statusEvent the status-event
     */
    public void setStatusEvent(StatusEvent statusEvent) {
        timeStampLabel.setText(
                ZonedDateTime.now(ZoneId.systemDefault()).toLocalTime().truncatedTo(ChronoUnit.MINUTES) + " - ");
        eventMessageLabel.setText(statusEvent.getEventMessage());
    }

    /**
     * Gets the currently displayed event message.
     *
     * @return the event message string
     */
    public String getCurrentEventMessage() {
        return eventMessageLabel.getText();
    }

    public void clear() {
        timeStampLabel.setText(null);
        eventMessageLabel.setText(null);
    }

    private static class SavedStatusIndicator extends Circle {
        private static final String SAVED_STATUS_PSEUDO_CLASS_NAME = "saved";
        private static final PseudoClass SAVED_STATUS_PSEUDO_CLASS =
                PseudoClass.getPseudoClass(SAVED_STATUS_PSEUDO_CLASS_NAME);
        private static final String SAVE_STATUS_INDICATOR_ID = "save-status-indicator";
        private static final int SAVE_STATUS_INDICATOR_RADIUS = 7;

        private final BooleanProperty saved = new BooleanPropertyBase(true) {
            @Override
            public Object getBean() {
                return SavedStatusIndicator.this;
            }

            @Override
            public String getName() {
                return SAVED_STATUS_PSEUDO_CLASS_NAME;
            }

            @Override
            protected void invalidated() {
                pseudoClassStateChanged(SAVED_STATUS_PSEUDO_CLASS, get());
            }
        };

        SavedStatusIndicator() {
            super(SAVE_STATUS_INDICATOR_RADIUS);
            setId(SAVE_STATUS_INDICATOR_ID);
        }

        public boolean isSaved() {
            return saved.get();
        }

        public BooleanProperty savedProperty() {
            return saved;
        }
    }
}
