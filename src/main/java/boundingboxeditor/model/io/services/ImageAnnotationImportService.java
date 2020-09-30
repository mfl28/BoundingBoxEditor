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
package boundingboxeditor.model.io.services;

import boundingboxeditor.model.data.ObjectCategory;
import boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import boundingboxeditor.model.io.ImageAnnotationLoader;
import boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

public class ImageAnnotationImportService extends Service<ImageAnnotationImportResult> {
    private final ObjectProperty<File> source = new SimpleObjectProperty<>(this, "source");
    private final ObjectProperty<ImageAnnotationLoadStrategy.Type> importFormat =
            new SimpleObjectProperty<>(this, "importFormat");
    private final ObjectProperty<Set<String>> importableFileNames = new SimpleObjectProperty<>(this,
                                                                                               "importableFileNames");
    private final ObjectProperty<Map<String, ObjectCategory>> categoryNameToCategoryMap =
            new SimpleObjectProperty<>(this, "categoryNameToCategoryMap");

    public Set<String> getImportableFileNames() {
        return importableFileNames.get();
    }

    public void setImportableFileNames(Set<String> importableFileNames) {
        this.importableFileNames.set(importableFileNames);
    }

    public ObjectProperty<Set<String>> importableFileNamesProperty() {
        return importableFileNames;
    }

    public Map<String, ObjectCategory> getCategoryNameToCategoryMap() {
        return categoryNameToCategoryMap.get();
    }

    public void setCategoryNameToCategoryMap(Map<String, ObjectCategory> categoryNameToCategoryMap) {
        this.categoryNameToCategoryMap.set(categoryNameToCategoryMap);
    }

    public File getSource() {
        return source.get();
    }

    public void setSource(File source) {
        this.source.set(source);
    }

    public ImageAnnotationLoadStrategy.Type getImportFormat() {
        return importFormat.get();
    }

    public void setImportFormat(ImageAnnotationLoadStrategy.Type importFormat) {
        this.importFormat.set(importFormat);
    }

    @Override
    protected Task<ImageAnnotationImportResult> createTask() {
        return new Task<>() {
            @Override
            protected ImageAnnotationImportResult call() throws Exception {
                ImageAnnotationLoader loader = new ImageAnnotationLoader(importFormat.get());
                loader.progressProperty()
                      .addListener((observable, oldValue, newValue) -> updateProgress(newValue.doubleValue(), 1.0));
                return loader.load(Paths.get(source.get().getPath()), importableFileNames.get(),
                                   categoryNameToCategoryMap.get());
            }
        };
    }
}
