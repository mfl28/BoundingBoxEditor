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
package com.github.mfl28.boundingboxeditor.model.data;

import java.io.File;

/**
 * Metadata related to IO operations.
 */
public class IoMetaData {
    private File defaultImageLoadingDirectory = null;
    private File defaultAnnotationSavingDirectory = null;
    private File defaultAnnotationLoadingDirectory = null;

    /**
     * Get the currently set default directory for image loading.
     *
     * @return the directory
     */
    public File getDefaultImageLoadingDirectory() {
        return defaultImageLoadingDirectory;
    }

    /**
     * Set the current default directory for image loading.
     *
     * @param defaultImageLoadingDirectory the directory
     */
    public void setDefaultImageLoadingDirectory(File defaultImageLoadingDirectory) {
        this.defaultImageLoadingDirectory = defaultImageLoadingDirectory;
    }

    /**
     * Get the currently set default directory for annotation saving.
     *
     * @return the directory
     */
    public File getDefaultAnnotationSavingDirectory() {
        return defaultAnnotationSavingDirectory;
    }

    /**
     * Set the current default directory for annotation saving.
     *
     * @param defaultAnnotationSavingDirectory the directory
     */
    public void setDefaultAnnotationSavingDirectory(File defaultAnnotationSavingDirectory) {
        this.defaultAnnotationSavingDirectory = defaultAnnotationSavingDirectory;
    }

    /**
     * Get the currently set default directory for annotation loading.
     *
     * @return the directory
     */
    public File getDefaultAnnotationLoadingDirectory() {
        return defaultAnnotationLoadingDirectory;
    }

    /**
     * Set the current default directory for annotation loading.
     *
     * @param defaultAnnotationLoadingDirectory the directory
     */
    public void setDefaultAnnotationLoadingDirectory(File defaultAnnotationLoadingDirectory) {
        this.defaultAnnotationLoadingDirectory = defaultAnnotationLoadingDirectory;
    }
}
