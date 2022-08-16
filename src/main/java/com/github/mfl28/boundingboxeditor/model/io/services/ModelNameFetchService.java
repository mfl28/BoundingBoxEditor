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
package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.restclients.ModelEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.PredictionClientException;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ModelNameFetchResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModelNameFetchService extends IoService<ModelNameFetchResult> {
    private final ObjectProperty<BoundingBoxPredictorClient> client = new SimpleObjectProperty<>(this, "client");

    public BoundingBoxPredictorClient getClient() {
        return client.get();
    }

    public void setClient(BoundingBoxPredictorClient client) {
        this.client.set(client);
    }

    public ObjectProperty<BoundingBoxPredictorClient> clientProperty() {
        return client;
    }

    @Override
    protected Task<ModelNameFetchResult> createTask() {
        return new Task<>() {
            @Override
            protected ModelNameFetchResult call() {
                List<IOErrorInfoEntry> errorInfoEntries = new ArrayList<>();

                List<ModelEntry> modelEntries;

                try {
                    modelEntries = client.get().models();
                } catch(PredictionClientException e) {
                    errorInfoEntries.add(new IOErrorInfoEntry(client.get().getName(), e.getMessage()));
                    return new ModelNameFetchResult(0, errorInfoEntries, Collections.emptyList());
                }

                List<String> models =
                        modelEntries.stream().map(ModelEntry::getModelName)
                                    .filter(modelName -> !modelName.isBlank())
                                    .toList();

                return new ModelNameFetchResult(1, Collections.emptyList(), models);
            }
        };
    }
}
