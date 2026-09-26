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
package com.github.mfl28.boundingboxeditor.model.io.results;

import java.util.List;

/**
 * The result of checking the connection to the inference server.
 */
public class ServerConnectionCheckResult extends IOResult {
    private final String serverName;

    public ServerConnectionCheckResult(int nrSuccessfullyProcessedItems, List<IOErrorInfoEntry> errorTableEntries,
                                       String serverName) {
        super(OperationType.SERVER_CONNECTION_CHECK, nrSuccessfullyProcessedItems, errorTableEntries);
        this.serverName = serverName;
    }

    /**
     * Returns the name of the checked server type, e.g. "LitServe".
     *
     * @return the name
     */
    public String getServerName() {
        return serverName;
    }
}
