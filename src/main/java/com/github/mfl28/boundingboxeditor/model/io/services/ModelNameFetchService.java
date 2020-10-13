package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.model.io.TorchServeRestClient;
import com.github.mfl28.boundingboxeditor.model.io.results.ModelNameFetchResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ModelNameFetchService extends Service<ModelNameFetchResult> {
    private final ObjectProperty<BoundingBoxPredictorClientConfig> clientConfig = new SimpleObjectProperty<>();

    public BoundingBoxPredictorClientConfig getClientConfig() {
        return clientConfig.get();
    }

    public ObjectProperty<BoundingBoxPredictorClientConfig> clientConfigProperty() {
        return clientConfig;
    }

    public void setClientConfig(BoundingBoxPredictorClientConfig clientConfig) {
        this.clientConfig.set(clientConfig);
    }

    @Override
    protected Task<ModelNameFetchResult> createTask() {
        return new Task<>() {
            @Override
            protected ModelNameFetchResult call() throws Exception {
                TorchServeRestClient client = new TorchServeRestClient(clientConfig.get());
                List<TorchServeRestClient.ModelEntry> modelEntries = client.models();

                List<String> models =
                        modelEntries.stream().map(TorchServeRestClient.ModelEntry::getModelName).collect(Collectors.toList());

                return new ModelNameFetchResult(1, Collections.emptyList(), models);
            }
        };
    }
}
