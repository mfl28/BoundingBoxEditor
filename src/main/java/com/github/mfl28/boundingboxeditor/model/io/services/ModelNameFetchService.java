package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.model.io.PredictionClientException;
import com.github.mfl28.boundingboxeditor.model.io.TorchServeRestClient;
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
    private final ObjectProperty<BoundingBoxPredictorClientConfig> clientConfig = new SimpleObjectProperty<>();

    public BoundingBoxPredictorClientConfig getClientConfig() {
        return clientConfig.get();
    }

    public void setClientConfig(BoundingBoxPredictorClientConfig clientConfig) {
        this.clientConfig.set(clientConfig);
    }

    public ObjectProperty<BoundingBoxPredictorClientConfig> clientConfigProperty() {
        return clientConfig;
    }

    @Override
    protected Task<ModelNameFetchResult> createTask() {
        return new Task<>() {
            @Override
            protected ModelNameFetchResult call() {
                TorchServeRestClient client = new TorchServeRestClient(clientConfig.get());

                List<IOErrorInfoEntry> errorInfoEntries = new ArrayList<>();

                List<TorchServeRestClient.ModelEntry> modelEntries;

                try {
                    modelEntries = client.models();
                } catch(PredictionClientException e) {
                    errorInfoEntries.add(new IOErrorInfoEntry("Torch serve", e.getMessage()));
                    return new ModelNameFetchResult(0, errorInfoEntries, Collections.emptyList());
                }

                List<String> models =
                        modelEntries.stream().map(TorchServeRestClient.ModelEntry::getModelName)
                                    .collect(Collectors.toList());

                return new ModelNameFetchResult(1, Collections.emptyList(), models);
            }
        };
    }
}
