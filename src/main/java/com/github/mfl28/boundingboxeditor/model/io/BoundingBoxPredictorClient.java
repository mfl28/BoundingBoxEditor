package com.github.mfl28.boundingboxeditor.model.io;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.List;

public interface BoundingBoxPredictorClient {
    static BoundingBoxPredictorClient create(BoundingBoxPredictorClientConfig clientConfig) {
        if(clientConfig.getServiceType().equals(ServiceType.TORCH_SERVE)) {
            return new TorchServeRestClient(clientConfig);
        }

        throw new InvalidParameterException();
    }

    List<BoundingBoxPredictionEntry> predict(File imageFile);

    enum ServiceType {TORCH_SERVE}
}
