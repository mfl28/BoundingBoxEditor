package com.github.mfl28.boundingboxeditor.model.io.restclients;

import javax.ws.rs.client.Client;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.List;

public interface BoundingBoxPredictorClient {
    static BoundingBoxPredictorClient create(Client client, BoundingBoxPredictorClientConfig clientConfig) {
        if(clientConfig.getServiceType().equals(ServiceType.TORCH_SERVE)) {
            return new TorchServeRestClient(client, clientConfig);
        }

        throw new InvalidParameterException();
    }

    List<BoundingBoxPredictionEntry> predict(InputStream input) throws PredictionClientException;

    List<ModelEntry> models() throws PredictionClientException;

    String getName();

    enum ServiceType {TORCH_SERVE}
}
