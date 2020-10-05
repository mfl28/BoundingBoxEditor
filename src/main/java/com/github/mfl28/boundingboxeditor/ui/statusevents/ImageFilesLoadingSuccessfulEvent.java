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
package com.github.mfl28.boundingboxeditor.ui.statusevents;

import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;

import java.io.File;

/**
 * Represents the event of a successful loading of image-files from a folder.
 */
public class ImageFilesLoadingSuccessfulEvent extends StatusEvent {
    /**
     * Creates a new status-event signifying the successful loading of image-files.
     *
     * @param ioResult             result of operation
     * @param loadedImageDirectory the directory from which the image-files were loaded
     */
    public ImageFilesLoadingSuccessfulEvent(IOResult ioResult, File loadedImageDirectory) {
        super("Successfully loaded " + ioResult.getNrSuccessfullyProcessedItems() + " image-file" +
                      (ioResult.getNrSuccessfullyProcessedItems() != 1 ? "s" : "") + " from folder " +
                      loadedImageDirectory.getPath()
                      + " in "
                      + secondsFormat.format(ioResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.");
    }
}
