package com.github.mfl28.boundingboxeditor.model.io.restclients;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GsonMessageBodyHandler implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    private final Gson gson = new GsonBuilder().registerTypeAdapter(BoundingBoxPredictionEntry.class,
                                                                    new BoundingBoxPredictionEntryDeserializer())
                                               .create();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        try(final InputStreamReader inputStreamReader = new InputStreamReader(entityStream, StandardCharsets.UTF_8)) {
            return gson.fromJson(inputStreamReader, genericType);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        try(final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(entityStream,
                                                                                 StandardCharsets.UTF_8)) {
            gson.toJson(o, genericType, outputStreamWriter);
        }
    }

    private static class BoundingBoxPredictionEntryDeserializer
            implements JsonDeserializer<BoundingBoxPredictionEntry> {
        private static final String SCORE_FIELD_NAME = "score";

        @Override
        public BoundingBoxPredictionEntry deserialize(JsonElement json, Type typeOfT,
                                                      JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            double score = jsonObject.get(SCORE_FIELD_NAME).getAsDouble();

            final Map<String, List<Double>> categoryToBoundingBox = new HashMap<>();

            for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if(!entry.getKey().equals(SCORE_FIELD_NAME) && entry.getValue().isJsonArray()) {
                    categoryToBoundingBox.put(entry.getKey(),
                                              context.deserialize(entry.getValue(),
                                                                  new TypeToken<List<Double>>() {}
                                                                          .getType()));
                }
            }

            return new BoundingBoxPredictionEntry(categoryToBoundingBox, score);
        }
    }
}
