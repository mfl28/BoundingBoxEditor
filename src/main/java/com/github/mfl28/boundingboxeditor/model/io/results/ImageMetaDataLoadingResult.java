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
package com.github.mfl28.boundingboxeditor.model.io.results;

import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ImageMetaDataLoadingResult extends IOResult {
    private final List<File> validFiles;
    private final Map<String, ImageMetaData> fileNameToMetaDataMap;

    /**
     * Creates a new io-operation result.
     *
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     * @param validFiles                   the files that could be parsed
     * @param fileNameToMetaDataMap        maps filenames to parsed meta data
     */
    public ImageMetaDataLoadingResult(int nrSuccessfullyProcessedItems,
                                      List<IOErrorInfoEntry> errorTableEntries,
                                      List<File> validFiles,
                                      Map<String, ImageMetaData> fileNameToMetaDataMap) {
        super(OperationType.IMAGE_METADATA_LOADING, nrSuccessfullyProcessedItems, errorTableEntries);
        this.validFiles = validFiles;
        this.fileNameToMetaDataMap = fileNameToMetaDataMap;
    }

    public List<File> getValidFiles() {
        return validFiles;
    }

    public Map<String, ImageMetaData> getFileNameToMetaDataMap() {
        return fileNameToMetaDataMap;
    }
}
