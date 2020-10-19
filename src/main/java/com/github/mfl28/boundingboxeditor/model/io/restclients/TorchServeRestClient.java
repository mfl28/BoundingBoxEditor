package com.github.mfl28.boundingboxeditor.model.io.restclients;

import com.google.gson.JsonSyntaxException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.List;

public class TorchServeRestClient implements BoundingBoxPredictorClient {
    private static final String MODELS_RESOURCE_NAME = "models";
    private static final String SERVER_MODELS_READ_ERROR_MESSAGE =
            "Could not read models from supplied torch serve management server.";
    private static final String DATA_BODY_PART_NAME = "data";
    private static final String PREDICTIONS_RESOURCE_NAME = "predictions";
    private static final String SERVER_PREDICTION_POST_ERROR_MESSAGE =
            "Could not get prediction from supplied inference server";
    private final Client client = ClientBuilder.newBuilder()
                                               .register(MultiPartFeature.class)
                                               .register(GsonMessageBodyHandler.class)
                                               .build();
    private final BoundingBoxPredictorClientConfig clientConfig;

    public TorchServeRestClient(BoundingBoxPredictorClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    public List<BoundingBoxPredictionEntry> predict(InputStream input) throws PredictionClientException {
        final MultiPart multiPart = new FormDataMultiPart().bodyPart(new StreamDataBodyPart(DATA_BODY_PART_NAME,
                                                                                            input));
        WebTarget predictionTarget;

        try {
            predictionTarget = client.target(clientConfig.getInferenceAddress());
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new PredictionClientException("Invalid torch serve inference address or port.");
        }

        Response response;

        try {
            response = predictionTarget.path(PREDICTIONS_RESOURCE_NAME).path(clientConfig.getInferenceModelName())
                                       .request(MediaType.APPLICATION_JSON)
                                       .post(Entity.entity(multiPart, multiPart.getMediaType()));
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException e) {
            if(e.getCause() instanceof ConnectException) {
                throw new PredictionClientException("Could not connect to supplied inference server.");
            } else {
                throw new PredictionClientException(SERVER_PREDICTION_POST_ERROR_MESSAGE);
            }
        }

        if(!response.getStatusInfo().equals(Response.Status.OK)) {
            throw new PredictionClientException(SERVER_PREDICTION_POST_ERROR_MESSAGE);
        }

        try {
            return response.readEntity(new GenericType<>() {});
        } catch(ProcessingException | IllegalStateException e) {
            throw new PredictionClientException(SERVER_PREDICTION_POST_ERROR_MESSAGE);
        } catch(JsonSyntaxException e) {
            throw new PredictionClientException("Invalid torch serve inference server response format for \"" +
                                                        PREDICTIONS_RESOURCE_NAME + "\" resource.");
        }
    }

    @Override
    public List<ModelEntry> models() throws PredictionClientException {
        WebTarget managementTarget;

        try {
            managementTarget = client.target(clientConfig.getManagementAddress());
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new PredictionClientException("Invalid torch serve management address or port.");
        }

        Response response;

        try {
            response = managementTarget
                    .path(MODELS_RESOURCE_NAME)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException e) {
            if(e.getCause() instanceof ConnectException) {
                throw new PredictionClientException("Could not connect to supplied management server.");
            } else {
                throw new PredictionClientException(SERVER_MODELS_READ_ERROR_MESSAGE);
            }
        }

        if(!response.getStatusInfo().equals(Response.Status.OK)) {
            throw new PredictionClientException(SERVER_MODELS_READ_ERROR_MESSAGE);
        }

        try {
            return response.readEntity(ModelsWrapper.class).getModels();
        } catch(ProcessingException | IllegalStateException e) {
            throw new PredictionClientException(SERVER_MODELS_READ_ERROR_MESSAGE);
        } catch(JsonSyntaxException e) {
            throw new PredictionClientException("Invalid torch serve management server response format for \""
                                                        + MODELS_RESOURCE_NAME + "\" resource.");
        }
    }

    public static class ModelEntry {
        private String modelName;
        private String modelUrl;

        public String getModelName() {
            return modelName;
        }

        public String getModelUrl() {
            return modelUrl;
        }
    }

    private static class ModelsWrapper {
        private List<ModelEntry> models;

        public List<ModelEntry> getModels() {
            return models;
        }
    }
}
