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
package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.io.IOOperationTimer;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageMetaDataLoadingResult;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageMetaDataLoadingService extends IoService<ImageMetaDataLoadingResult> {
    private static final String[] ignoredExtensions = {".txt", ".json", ".xml", ".data"};
    private final ObjectProperty<File> source = new SimpleObjectProperty<>(this, "source");
    private final ObjectProperty<List<File>> imageFiles = new SimpleObjectProperty<>(this, "imageFiles");
    private final BooleanProperty reload = new SimpleBooleanProperty(this, "reload");

    public File getSource() {
        return source.get();
    }

    public void setSource(File source) {
        this.source.set(source);
    }

    public void setImageFiles(List<File> imageFiles) {
        this.imageFiles.set(imageFiles);
    }

    public boolean isReload() {
        return reload.get();
    }

    public void setReload(boolean reload) {
        this.reload.set(reload);
    }

    @Override
    protected Task<ImageMetaDataLoadingResult> createTask() {
        return new Task<>() {
            @Override
            protected ImageMetaDataLoadingResult call() throws Exception {
                return IOOperationTimer.time(() -> {
                    final Map<String, ImageMetaData> fileNameToMetaDataMap = new HashMap<>();

                    final List<IOErrorInfoEntry> errorInfoEntries =
                            Collections.synchronizedList(new ArrayList<>());

                    final List<File> filteredFiles = imageFiles.get().stream()
                            .filter(file -> !StringUtils.endsWithAny(file.getName().toLowerCase(Locale.ENGLISH), ignoredExtensions))
                            .toList();

                    int totalNrOfFiles = filteredFiles.size();
                    final AtomicInteger nrProcessedFiles = new AtomicInteger(0);

                    fileNameToMetaDataMap
                            .putAll(filteredFiles.parallelStream()
                                    .collect(HashMap::new, (map, item) -> {
                                updateProgress(1.0 * nrProcessedFiles.incrementAndGet() / totalNrOfFiles, 1.0);
                                try {
                                    map.put(item.getName(), ImageMetaData.fromFile(item));
                                } catch(Exception e) {
                                    errorInfoEntries.add(new IOErrorInfoEntry(item.getName(), e.getMessage()));
                                }
                            }, Map::putAll));

                    final List<File> validImageFiles =
                            filteredFiles.stream().filter(item -> fileNameToMetaDataMap.containsKey(item.getName()))
                                      .toList();

                    return new ImageMetaDataLoadingResult(fileNameToMetaDataMap.size(), errorInfoEntries,
                                                          validImageFiles,
                                                          fileNameToMetaDataMap);
                });
            }
        };
    }
}
