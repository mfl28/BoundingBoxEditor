/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.io;

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingPolygonData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Bounds;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Saves rectangular bounding-box annotations in the YOLO-format described at
 * <a href="https://github.com/AlexeyAB/Yolo_mark/issues/60#issuecomment-401854885">...</a>
 */
public class YOLOSaveStrategy implements ImageAnnotationSaveStrategy {
    private static final DecimalFormat DECIMAL_FORMAT =
            new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static final String YOLO_ANNOTATION_FILE_EXTENSION = ".txt";
    private static final String OBJECT_DATA_FILE_NAME = "object.data";
    private Path saveFolderPath;
    private List<String> categories;

    @Override
    public ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination,
                                            DoubleProperty progress) {
        this.saveFolderPath = destination;
        this.categories = annotations.categoryNameToBoundingShapeCountMap().entrySet().stream()
                                     .filter(stringIntegerEntry -> stringIntegerEntry.getValue() > 0)
                                     .map(Map.Entry::getKey)
                                     .sorted()
                                     .toList();

        List<IOErrorInfoEntry> unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());

        try {
            createObjectDataFile();
        } catch(IOException e) {
            unParsedFileErrorMessages.add(new IOErrorInfoEntry(OBJECT_DATA_FILE_NAME, e.getMessage()));
        }

        int totalNrOfAnnotations = annotations.imageAnnotations().size();
        AtomicInteger nrProcessedAnnotations = new AtomicInteger(0);

        annotations.imageAnnotations().parallelStream().forEach(annotation -> {
            try {
                createAnnotationFile(annotation);
            } catch(IOException e) {
                unParsedFileErrorMessages
                        .add(new IOErrorInfoEntry(annotation.getImageFileName(), e.getMessage()));
            }

            progress.set(1.0 * nrProcessedAnnotations.incrementAndGet() / totalNrOfAnnotations);
        });

        return new ImageAnnotationExportResult(
                totalNrOfAnnotations - unParsedFileErrorMessages.size(),
                unParsedFileErrorMessages
        );
    }

    private void createObjectDataFile() throws IOException {
        try(BufferedWriter fileWriter = Files.newBufferedWriter(saveFolderPath
                                                                        .resolve(OBJECT_DATA_FILE_NAME))) {
            for(int i = 0; i < categories.size() - 1; ++i) {
                fileWriter.write(categories.get(i));
                fileWriter.newLine();
            }

            if(!categories.isEmpty()) {
                fileWriter.write(categories.get(categories.size() - 1));
            }
        }
    }

    private void createAnnotationFile(ImageAnnotation annotation) throws IOException {
        String imageFileName = annotation.getImageFileName();
        String imageFileNameWithoutExtension = imageFileName.substring(0, imageFileName.lastIndexOf('.'));

        try(BufferedWriter fileWriter = Files.newBufferedWriter(saveFolderPath
                                                                        .resolve(imageFileNameWithoutExtension +
                                                                                         YOLO_ANNOTATION_FILE_EXTENSION))) {
            List<BoundingShapeData> boundingShapeDataList = annotation.getBoundingShapeData();

            for(int i = 0; i < boundingShapeDataList.size() - 1; ++i) {
                BoundingShapeData boundingShapeData = boundingShapeDataList.get(i);

                if(boundingShapeData instanceof BoundingBoxData boundingBoxData) {
                    fileWriter.write(createBoundingBoxDataEntry(boundingBoxData));
                    fileWriter.newLine();
                } else if(boundingShapeData instanceof BoundingPolygonData boundingPolygonData) {
                    fileWriter.write(createBoundingPolygonDataEntry(boundingPolygonData));
                    fileWriter.newLine();
                }
            }

            if(!boundingShapeDataList.isEmpty()) {
                BoundingShapeData lastShapeData = boundingShapeDataList.get(boundingShapeDataList.size() - 1);

                if(lastShapeData instanceof BoundingBoxData boundingBoxData) {
                    fileWriter.write(createBoundingBoxDataEntry(boundingBoxData));
                } else if(lastShapeData instanceof BoundingPolygonData boundingPolygonData) {
                    fileWriter.write(createBoundingPolygonDataEntry(boundingPolygonData));
                }
            }
        }
    }

    private String createBoundingBoxDataEntry(BoundingBoxData boundingBoxData) {
        int categoryIndex = categories.indexOf(boundingBoxData.getCategoryName());

        Bounds relativeBounds = boundingBoxData.getRelativeBoundsInImage();

        String xMidRelative = DECIMAL_FORMAT.format(relativeBounds.getCenterX());
        String yMidRelative = DECIMAL_FORMAT.format(relativeBounds.getCenterY());
        String widthRelative = DECIMAL_FORMAT.format(relativeBounds.getWidth());
        String heightRelative = DECIMAL_FORMAT.format(relativeBounds.getHeight());

        return StringUtils.join(List.of(categoryIndex, xMidRelative, yMidRelative, widthRelative, heightRelative), " ");
    }

    private String createBoundingPolygonDataEntry(BoundingPolygonData boundingPolygonData) {
        int categoryIndex = categories.indexOf(boundingPolygonData.getCategoryName());

        List<Double> relativePoints = boundingPolygonData.getRelativePointsInImage();
        List<String> relativePointsAsStrings = new ArrayList<>();

        for(int i = 0; i + 1 < relativePoints.size(); i += 2){
            String xRelative = DECIMAL_FORMAT.format(relativePoints.get(i));
            String yRelative = DECIMAL_FORMAT.format(relativePoints.get(i + 1));
            relativePointsAsStrings.add(StringUtils.join(List.of(xRelative, yRelative), " "));
        }

        return StringUtils.join(List.of(categoryIndex, StringUtils.join(relativePointsAsStrings, " ")), " ");
    }
}
