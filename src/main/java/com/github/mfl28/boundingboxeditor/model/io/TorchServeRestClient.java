package com.github.mfl28.boundingboxeditor.model.io;

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorchServeRestClient implements BoundingBoxPredictorClient {
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

    public List<ModelEntry> models() {
        String modelsJson =
                client.target(clientConfig.getManagementAddress())
                      .path("models")
                      .request(MediaType.APPLICATION_JSON)
                      .get()
                      .readEntity(String.class);

        return new Gson().fromJson(modelsJson, ModelsWrapper.class).models;
    }

    @Override
    public List<BoundingBoxPredictionEntry> predict(File imageFile) {
       final MultiPart multiPart = new FormDataMultiPart().bodyPart(new FileDataBodyPart("data", imageFile));
       final String predictionJson =
               client.target(clientConfig.getInferenceAddress())
                     .path("predictions").path(clientConfig.getInferenceModelName())
                     .request(MediaType.APPLICATION_JSON)
                     .post(Entity.entity(multiPart, multiPart.getMediaType()))
                     .readEntity(String.class);

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
                                                                                              new TypeToken<List<Double>>(){}.getType()));
                                            }
                                        }

                                        return new BoundingBoxPredictionEntry(categoryToBoundingBox, score);

                                    }).create();

       return gson.fromJson(predictionJson, new TypeToken<List<BoundingBoxPredictionEntry>>(){}.getType());
    }


    public static class PredictionResult {
        private final List<BoundingBoxData> predictedBoundingBoxes;
        private final List<Double> scores;

        public PredictionResult(
                List<BoundingBoxData> predictedBoundingBoxes, List<Double> scores) {
            this.predictedBoundingBoxes = predictedBoundingBoxes;
            this.scores = scores;
        }

        public List<BoundingBoxData> getPredictedBoundingBoxes() {
            return predictedBoundingBoxes;
        }

        public List<Double> getScores() {
            return scores;
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

        @Override
        public String toString() {
            return "ModelEntry{" +
                    "modelName='" + modelName + '\'' +
                    ", modelUrl='" + modelUrl + '\'' +
                    '}';
        }
    }

    private static class ModelsWrapper {
        private List<ModelEntry> models;
    }

}
