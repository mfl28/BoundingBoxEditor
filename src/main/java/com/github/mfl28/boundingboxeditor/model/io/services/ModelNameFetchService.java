package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.restclients.ModelEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.PredictionClientException;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ModelNameFetchResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ModelNameFetchService extends Service<ModelNameFetchResult> {
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
                                    .collect(Collectors.toList());

                return new ModelNameFetchResult(1, Collections.emptyList(), models);
            }
        };
    }
}
