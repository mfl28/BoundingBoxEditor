/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.model.io.restclients;

import com.google.gson.JsonSyntaxException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Function;

/**
 * The steps of a request shared by the REST clients of the inference servers: resolving the server address, mapping
 * the JAX-RS client's exceptions to the error messages shown to the user, and reading the response.
 */
final class RestRequests {
    /**
     * The type of a prediction response body.
     */
    static final GenericType<List<BoundingBoxPredictionEntry>> PREDICTIONS_TYPE = new GenericType<>() {};
    private static final String SERVER_ERROR_REASON = " Reason: ";

    private RestRequests() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates the target of a server's address.
     *
     * @param client        the JAX-RS client
     * @param address       the server's address
     * @param errorMessages the server's error messages
     * @return the target
     * @throws PredictionClientException if the address is invalid
     */
    static WebTarget createTarget(Client client, String address, ServerErrorMessages errorMessages)
            throws PredictionClientException {
        try {
            return client.target(address);
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new PredictionClientException(errorMessages.invalidAddress());
        }
    }

    /**
     * Converts an exception thrown while building or sending a request to the matching error.
     *
     * @param exception     the exception
     * @param errorMessages the server's error messages
     * @return the error to report
     */
    static PredictionClientException toRequestException(Exception exception, ServerErrorMessages errorMessages) {
        if(exception.getCause() instanceof ConnectException) {
            return new PredictionClientException(errorMessages.connectionFailed());
        } else if(exception.getCause() instanceof SocketTimeoutException) {
            return new PredictionClientException(errorMessages.timedOut());
        } else {
            return new PredictionClientException(errorMessages.requestFailed());
        }
    }

    /**
     * Reads a successful response and closes it.
     *
     * @param response      the response
     * @param entityReader  reads the response's body
     * @param errorMessages the server's error messages
     * @param <T>           the type of the response's body
     * @return the response's body
     * @throws PredictionClientException if the server did not respond with OK or the body could not be read
     */
    static <T> T readEntity(Response response, Function<Response, T> entityReader,
                            ServerErrorMessages errorMessages) throws PredictionClientException {
        try(response) {
            if(!response.getStatusInfo().equals(Response.Status.OK)) {
                throw new PredictionClientException(errorMessages.requestFailed() + SERVER_ERROR_REASON
                        + response.getStatusInfo().getReasonPhrase());
            }

            return entityReader.apply(response);
        } catch(ProcessingException | IllegalStateException e) {
            throw new PredictionClientException(errorMessages.requestFailed());
        } catch(JsonSyntaxException e) {
            throw new PredictionClientException(errorMessages.invalidResponseFormat());
        }
    }

    /**
     * The error messages reported for one of a server's endpoints.
     */
    record ServerErrorMessages(String invalidAddress, String connectionFailed, String timedOut,
                               String requestFailed, String invalidResponseFormat) {
    }
}
