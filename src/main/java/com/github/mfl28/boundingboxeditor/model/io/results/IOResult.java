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
package com.github.mfl28.boundingboxeditor.model.io.results;

import java.util.List;

/**
 * Responsible for holding information about a finished IO-operation (e.g. loading or
 * saving of image-annotations).
 */
public abstract class IOResult {
    private final int nrSuccessfullyProcessedItems;
    private final List<IOErrorInfoEntry> errorTableEntries;
    private final OperationType operationType;
    private long timeTakenInMilliseconds = 0;

    /**
     * Creates a new io-operation result.
     *
     * @param operationType                specifies the result's operation-type
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     *                                     about where and which errors occurred during the operation.
     */
    protected IOResult(OperationType operationType, int nrSuccessfullyProcessedItems,
                       List<IOErrorInfoEntry> errorTableEntries) {
        this.operationType = operationType;
        this.nrSuccessfullyProcessedItems = nrSuccessfullyProcessedItems;
        this.errorTableEntries = errorTableEntries;
    }

    /**
     * Returns the type of operation the result belongs to.
     *
     * @return the operation type
     */
    public OperationType getOperationType() {
        return operationType;
    }

    /**
     * Returns the number of successfully processed items during the io-operation.
     *
     * @return the number of successfully processed items
     */
    public int getNrSuccessfullyProcessedItems() {
        return nrSuccessfullyProcessedItems;
    }

    /**
     * Returns the time (in milliseconds) that the operation took to complete.
     *
     * @return the duration in milliseconds
     */
    public long getTimeTakenInMilliseconds() {
        return timeTakenInMilliseconds;
    }

    /**
     * Sets the time (in milliseconds) that the operation took to complete.
     *
     * @param timeTakenInMilliseconds the duration in milliseconds
     */
    public void setTimeTakenInMilliseconds(long timeTakenInMilliseconds) {
        this.timeTakenInMilliseconds = timeTakenInMilliseconds;
    }

    /**
     * Returns a list of objects containing information of where and which errors occurred during
     * the operation.
     *
     * @return the list of error-entries
     */
    public List<IOErrorInfoEntry> getErrorTableEntries() {
        return errorTableEntries;
    }

    public enum OperationType {
        ANNOTATION_IMPORT, ANNOTATION_SAVING, IMAGE_METADATA_LOADING, BOUNDING_BOX_PREDICTION,
        MODEL_NAME_FETCHING
    }

}
