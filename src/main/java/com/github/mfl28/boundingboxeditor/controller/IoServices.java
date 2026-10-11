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
package com.github.mfl28.boundingboxeditor.controller;

import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import com.github.mfl28.boundingboxeditor.model.io.services.*;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import com.github.mfl28.boundingboxeditor.ui.ServiceProgressDialog;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The background services that import and export annotations, load image folders and talk to the inference server.
 * Each service gets a single progress dialog that is reused for every run (ControlsFX progress dialogs never detach
 * from their worker, so creating one per run would leave stale dialogs that reappear on every later run). A failed
 * run shows its exception.
 */
class IoServices {
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE = "Saving Annotations";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER = "Saving in progress...";
    private static final String LOADING_ANNOTATIONS_PROGRESS_DIALOG_TITLE = "Loading";
    private static final String LOADING_ANNOTATIONS_PROGRESS_DIALOG_HEADER = "Loading annotations...";
    private static final String IMAGE_FILES_LOADING_PROGRESS_DIALOG_TITLE = "Loading Images";
    private static final String IMAGE_FILES_LOADING_PROGRESS_DIALOG_HEADER = "Loading image meta-data";
    private static final String BOUNDING_BOX_PREDICTION_PROGRESS_DIALOG_TITLE = "Predicting";
    private static final String BOUNDING_BOX_PREDICTION_PROGRESS_DIALOG_HEADER = "Predicting bounding boxes";
    private static final String FETCHING_MODELS_PROGRESS_DIALOG_TITLE = "Fetching Models";
    private static final String FETCHING_MODELS_PROGRESS_DIALOG_HEADER = "Fetching model names from server";
    private static final String CONNECTION_CHECK_PROGRESS_DIALOG_TITLE = "Checking Connection";
    private static final String CONNECTION_CHECK_PROGRESS_DIALOG_HEADER = "Checking the inference server";

    private final ImageAnnotationExportService annotationExportService = new ImageAnnotationExportService();
    private final ImageAnnotationImportService annotationImportService = new ImageAnnotationImportService();
    private final ImageMetaDataLoadingService imageMetaDataLoadingService = new ImageMetaDataLoadingService();
    private final BoundingBoxPredictorService boundingBoxPredictorService = new BoundingBoxPredictorService();
    private final ModelNameFetchService modelNameFetchService = new ModelNameFetchService();
    private final ServerConnectionCheckService serverConnectionCheckService = new ServerConnectionCheckService();
    private final DialogService dialogService;
    private final Stage stage;

    /**
     * What to do with the services' results.
     */
    interface ResultHandlers {
        void onAnnotationsExported(WorkerStateEvent event);

        void onAnnotationsImported(WorkerStateEvent event);

        void onImageMetaDataLoaded(WorkerStateEvent event);

        void onBoundingBoxesPredicted(WorkerStateEvent event);

        void onModelNamesFetched(WorkerStateEvent event);

        void onServerConnectionChecked(WorkerStateEvent event);

        /**
         * Called when a service failed, before its exception is shown.
         *
         * @param service the service
         */
        void onFailed(IoService<?> service);
    }

    IoServices(DialogService dialogService, Stage stage, ResultHandlers handlers) {
        this.dialogService = dialogService;
        this.stage = stage;
        final Consumer<IoService<?>> onFailed = handlers::onFailed;

        setUp(annotationExportService, SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE,
              SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER, handlers::onAnnotationsExported, onFailed, false);
        setUp(annotationImportService, LOADING_ANNOTATIONS_PROGRESS_DIALOG_TITLE,
              LOADING_ANNOTATIONS_PROGRESS_DIALOG_HEADER, handlers::onAnnotationsImported, onFailed, false);
        setUp(imageMetaDataLoadingService, IMAGE_FILES_LOADING_PROGRESS_DIALOG_TITLE,
              IMAGE_FILES_LOADING_PROGRESS_DIALOG_HEADER, handlers::onImageMetaDataLoaded, onFailed, false);
        setUp(boundingBoxPredictorService, BOUNDING_BOX_PREDICTION_PROGRESS_DIALOG_TITLE,
              BOUNDING_BOX_PREDICTION_PROGRESS_DIALOG_HEADER, handlers::onBoundingBoxesPredicted, onFailed, true);
        setUp(modelNameFetchService, FETCHING_MODELS_PROGRESS_DIALOG_TITLE,
              FETCHING_MODELS_PROGRESS_DIALOG_HEADER, handlers::onModelNamesFetched, onFailed, true);
        setUp(serverConnectionCheckService, CONNECTION_CHECK_PROGRESS_DIALOG_TITLE,
              CONNECTION_CHECK_PROGRESS_DIALOG_HEADER, handlers::onServerConnectionChecked, onFailed, true);
    }

    /**
     * Starts exporting annotations.
     *
     * @param chainedOperation run after the export succeeded, may be null
     */
    void startExport(File destination, ImageAnnotationSaveStrategy.Type exportFormat, ImageAnnotationData data,
                     Runnable chainedOperation) {
        annotationExportService.reset();
        annotationExportService.setDestination(destination);
        annotationExportService.setExportFormat(exportFormat);
        annotationExportService.setAnnotationData(data);
        annotationExportService.setChainedOperation(chainedOperation);
        annotationExportService.restart();
    }

    /**
     * Starts importing annotations of the loaded image files.
     */
    void startImport(File source, ImageAnnotationLoadStrategy.Type importFormat, Set<String> importableFileNames,
                     Map<String, ObjectCategory> categoryNameToCategory) {
        annotationImportService.reset();
        annotationImportService.setSource(source);
        annotationImportService.setImportFormat(importFormat);
        annotationImportService.setImportableFileNames(importableFileNames);
        annotationImportService.setCategoryNameToCategoryMap(categoryNameToCategory);
        annotationImportService.restart();
    }

    ImageAnnotationExportService getAnnotationExportService() {
        return annotationExportService;
    }

    ImageAnnotationImportService getAnnotationImportService() {
        return annotationImportService;
    }

    ImageMetaDataLoadingService getImageMetaDataLoadingService() {
        return imageMetaDataLoadingService;
    }

    BoundingBoxPredictorService getBoundingBoxPredictorService() {
        return boundingBoxPredictorService;
    }

    ModelNameFetchService getModelNameFetchService() {
        return modelNameFetchService;
    }

    ServerConnectionCheckService getServerConnectionCheckService() {
        return serverConnectionCheckService;
    }

    private void setUp(IoService<?> service, String progressDialogTitle, String progressDialogHeader,
                       EventHandler<WorkerStateEvent> onSucceeded, Consumer<IoService<?>> onFailed,
                       boolean cancellable) {
        final ServiceProgressDialog progressDialog =
                dialogService.createServiceProgressDialog(service, progressDialogTitle, progressDialogHeader);
        progressDialog.setOwnerParentWindow(stage);

        if(cancellable) {
            progressDialog.enableCancellation();
        }

        service.setProgressViewer(progressDialog);
        service.setOnSucceeded(onSucceeded);
        service.setOnFailed(event -> {
            onFailed.accept(service);

            final Throwable exception = event.getSource().getException();

            if(exception != null) {
                dialogService.displayExceptionDialog(exception, stage);
            }
        });
    }
}
