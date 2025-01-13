/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.io.restclients;

import java.util.Objects;

public final class ModelEntry {
    private final String modelName;
    private final String modelUrl;

    public ModelEntry(String modelName, String modelUrl) {
        this.modelName = modelName;
        this.modelUrl = modelUrl;
    }

    public String modelName() {
        return modelName;
    }

    public String modelUrl() {
        return modelUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }

        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        ModelEntry that = (ModelEntry) obj;

        return Objects.equals(this.modelName, that.modelName) &&
                Objects.equals(this.modelUrl, that.modelUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelName, modelUrl);
    }
}
