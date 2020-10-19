package com.github.mfl28.boundingboxeditor.model.io.restclients;

import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.List;

public interface BoundingBoxPredictorClient {
    static BoundingBoxPredictorClient create(BoundingBoxPredictorClientConfig clientConfig) {
        if(clientConfig.getServiceType().equals(ServiceType.TORCH_SERVE)) {
            return new TorchServeRestClient(clientConfig);
        }

        throw new InvalidParameterException();
    }

    List<BoundingBoxPredictionEntry> predict(InputStream input) throws PredictionClientException;

    List<TorchServeRestClient.ModelEntry> models() throws PredictionClientException;

    enum ServiceType {TORCH_SERVE}
}
