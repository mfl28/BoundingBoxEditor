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
package boundingboxeditor.model.io.results;

import java.util.List;

public class ImageAnnotationExportResult extends IOResult {
    /**
     * Creates a new io-operation result.
     *
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     */
    public ImageAnnotationExportResult(int nrSuccessfullyProcessedItems,
                                       List<IOErrorInfoEntry> errorTableEntries) {
        super(OperationType.ANNOTATION_SAVING, nrSuccessfullyProcessedItems, errorTableEntries);
    }
}
