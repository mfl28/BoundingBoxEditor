package com.github.mfl28.boundingboxeditor.model.io.results;

import java.util.List;

public class ModelNameFetchResult extends IOResult {
    private final List<String> modelNames;

    public ModelNameFetchResult(int nrSuccessfullyProcessedItems, List<IOErrorInfoEntry> errorTableEntries,
                                List<String> modelNames) {
        super(OperationType.MODEL_NAME_FETCHING, nrSuccessfullyProcessedItems, errorTableEntries);
        this.modelNames = modelNames;
    }

    public List<String> getModelNames() {
        return modelNames;
    }
}
