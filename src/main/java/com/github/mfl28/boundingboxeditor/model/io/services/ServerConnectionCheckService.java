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
package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.restclients.PredictionClientException;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ServerConnectionCheckResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

import java.util.Collections;
import java.util.List;

/**
 * Checks that the inference server is reachable and ready, off the JavaFX thread.
 */
public class ServerConnectionCheckService extends IoService<ServerConnectionCheckResult> {
    private final ObjectProperty<BoundingBoxPredictorClient> client = new SimpleObjectProperty<>(this, "client");

    public void setClient(BoundingBoxPredictorClient client) {
        this.client.set(client);
    }

    @Override
    protected Task<ServerConnectionCheckResult> createTask() {
        final BoundingBoxPredictorClient checkedClient = client.get();

        return new Task<>() {
            @Override
            protected ServerConnectionCheckResult call() {
                try {
                    checkedClient.checkConnection();
                } catch(PredictionClientException e) {
                    return new ServerConnectionCheckResult(0,
                            List.of(new IOErrorInfoEntry(checkedClient.getName(), e.getMessage())),
                            checkedClient.getName());
                }

                return new ServerConnectionCheckResult(1, Collections.emptyList(), checkedClient.getName());
            }
        };
    }
}
