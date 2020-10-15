package com.github.mfl28.boundingboxeditor.model.io;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorchServeRestClient implements BoundingBoxPredictorClient {
    private static final String MODELS_RESOURCE_NAME = "models";
    private static final String SERVER_MODELS_READ_ERROR_MESSAGE =
            "Could not read models from supplied torch serve management server.";
    private static final String DATA_BODY_PART_NAME = "data";
    private static final String PREDICTIONS_RESOURCE_NAME = "predictions";
    private static final String SERVER_PREDICTION_POST_ERROR_MESSAGE =
            "Could not get prediction from supplied inference server";
    private final Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
    private final BoundingBoxPredictorClientConfig clientConfig;

    public TorchServeRestClient(BoundingBoxPredictorClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        client.register(MultiPartFeature.class);
    }

    public String ping() {
        return client.target(clientConfig.getInferenceAddress())
                     .path("ping")
                     .request(MediaType.APPLICATION_JSON)
                     .get()
                     .readEntity(String.class);
    }

    public List<ModelEntry> models() throws PredictionClientException {
        WebTarget managementTarget;

        try {
            managementTarget = client.target(clientConfig.getManagementAddress());
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new PredictionClientException("Invalid torch serve management address and/or port.");
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

        String modelsJson;

        try {
            modelsJson = response.readEntity(String.class);
        } catch(ProcessingException | IllegalStateException e) {
            throw new PredictionClientException(SERVER_MODELS_READ_ERROR_MESSAGE);
        }

        List<ModelEntry> models;

        try {
            models = new Gson().fromJson(modelsJson, ModelsWrapper.class).getModels();
        } catch(JsonSyntaxException e) {
            throw new PredictionClientException("Invalid torch serve management server response format for \""
                                                        + MODELS_RESOURCE_NAME + "\" resource.");
        }

        return models;
    }

    @Override
    public List<BoundingBoxPredictionEntry> predict(InputStream input) throws PredictionClientException {
        final MultiPart multiPart = new FormDataMultiPart().bodyPart(new StreamDataBodyPart(DATA_BODY_PART_NAME,
                                                                                            input));

        WebTarget predictionTarget;

        try {
            predictionTarget = client.target(clientConfig.getInferenceAddress());
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new PredictionClientException("Invalid torch serve inference address and/or port.");
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

        String predictionJson;

        try {
            predictionJson = response.readEntity(String.class);
        } catch(ProcessingException | IllegalStateException e) {
            throw new PredictionClientException(SERVER_PREDICTION_POST_ERROR_MESSAGE);
        }

        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(BoundingBoxPredictionEntry.class,
                                     (JsonDeserializer<BoundingBoxPredictionEntry>) (json, type, context) ->
                                     {
                                         final JsonObject jsonObject = json.getAsJsonObject();
                                         double score = jsonObject.get("score").getAsDouble();

                                         Map<String, List<Double>> categoryToBoundingBox = new HashMap<>();

                                         for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                                             if(!entry.getKey().equals("score") && entry.getValue().isJsonArray()) {
                                                 categoryToBoundingBox.put(entry.getKey(),
                                                                           context.deserialize(entry.getValue(),
                                                                                               new TypeToken<List<Double>>() {}
                                                                                                       .getType()));
                                             }
                                         }

                                         return new BoundingBoxPredictionEntry(categoryToBoundingBox, score);

                                     }).create();

        List<BoundingBoxPredictionEntry> boundingBoxPredictionEntries;

        try {
            boundingBoxPredictionEntries =
                    gson.fromJson(predictionJson, new TypeToken<List<BoundingBoxPredictionEntry>>() {}.getType());
        } catch(JsonSyntaxException e) {
            throw new PredictionClientException("Invalid torch serve inference server response format for \"" +
                                                        PREDICTIONS_RESOURCE_NAME + "\" resource.");
        }

        return boundingBoxPredictionEntries;
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
