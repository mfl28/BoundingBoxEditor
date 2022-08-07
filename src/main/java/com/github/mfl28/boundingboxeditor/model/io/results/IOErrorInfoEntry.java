/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.io.results;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * Holds information about an error that occurred during an io-operation.
 */
public class IOErrorInfoEntry {
    private final StringProperty sourceName;
    private final StringProperty errorDescription;

    /**
     * Creates a new error-information entry.
     *
     * @param sourceName       the name of the source of the error
     * @param errorDescription a description of the error that occurred
     */
    public IOErrorInfoEntry(String sourceName, String errorDescription) {
        this.sourceName = new SimpleStringProperty(sourceName);
        this.errorDescription = new SimpleStringProperty(errorDescription);
    }

    /**
     * Returns the name of the source of the error.
     *
     * @return the source-name
     */
    public String getSourceName() {
        return sourceName.get();
    }

    /**
     * Set the name of the source of the error.
     *
     * @param sourceName the name of the source
     */
    public void setSourceName(String sourceName) {
        this.sourceName.set(sourceName);
    }

    /**
     * Returns the error source-name property.
     *
     * @return source-name property
     */
    public StringProperty sourceNameProperty() {
        return sourceName;
    }

    /**
     * Returns the description of the error that occurred.
     *
     * @return the error-description
     */
    public String getErrorDescription() {
        return errorDescription.get();
    }

    /**
     * Sets a description of the error that occurred.
     *
     * @param errorDescription the error-description
     */
    public void setErrorDescription(String errorDescription) {
        this.errorDescription.set(errorDescription);
    }

    /**
     * Returns the description-property of the error that occurred.
     *
     * @return the error-description-property
     */
    public StringProperty errorDescriptionProperty() {
        return errorDescription;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceName.get(), errorDescription.get());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(!(o instanceof IOErrorInfoEntry)) {
            return false;
        }

        IOErrorInfoEntry that = (IOErrorInfoEntry) o;

        return Objects.equals(sourceName.get(), that.sourceName.get()) &&
                Objects.equals(errorDescription.get(), that.errorDescription.get());
    }
}
