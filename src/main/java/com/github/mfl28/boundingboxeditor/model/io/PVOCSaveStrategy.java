/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Bounds;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements the saving of image-annotations to xml-files using the
 * 'PASCAL Visual Object Classes (Pascal VOC)'-format.
 *
 * @see <a href="http://host.robots.ox.ac.uk/pascal/VOC/">Pascal VOC</a>
 */
public class PVOCSaveStrategy implements ImageAnnotationSaveStrategy {
    private static final String ROOT_ELEMENT_NAME = "annotation";
    private static final String FOLDER_ELEMENT_NAME = "folder";
    private static final String FILENAME_ELEMENT_NAME = "filename";
    private static final String IMAGE_SIZE_ELEMENT_NAME = "size";
    private static final String IMAGE_WIDTH_ELEMENT_NAME = "width";
    private static final String IMAGE_HEIGHT_ELEMENT_NAME = "height";
    private static final String BOUNDING_SHAPE_ENTRY_ELEMENT_NAME = "object";
    private static final String BOUNDING_SHAPE_CATEGORY_NAME = "name";
    private static final String BOUNDING_BOX_SIZE_GROUP_NAME = "bndbox";

    private static final DecimalFormat DECIMAL_FORMAT =
            new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static final String FILE_EXTENSION = ".xml";
    private static final String XMIN_TAG = "xmin";
    private static final String XMAX_TAG = "xmax";
    private static final String YMIN_TAG = "ymin";
    private static final String YMAX_TAG = "ymax";
    private static final String ANNOTATION_FILENAME_EXTENSION = "_A";
    private static final String BOUNDING_SHAPE_PART_NAME = "part";
    private static final String ACTIONS_TAG_NAME = "actions";
    private static final String IMAGE_DEPTH_ELEMENT_NAME = "depth";
    private static final String BOUNDING_POLYGON_SIZE_GROUP_NAME = "polygon";
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private Path saveFolderPath;

    @Override
    public ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination,
                                            DoubleProperty progress) {
        this.saveFolderPath = destination;

        List<IOErrorInfoEntry> unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());

        int totalNrOfAnnotations = annotations.imageAnnotations().size();
        AtomicInteger nrProcessedAnnotations = new AtomicInteger(0);


        annotations.imageAnnotations().parallelStream().forEach(annotation -> {
            try {
                createXmlFileFromImageAnnotationDataElement(annotation);
            } catch(TransformerException | ParserConfigurationException e) {
                unParsedFileErrorMessages
                        .add(new IOErrorInfoEntry(annotation.getImageFileName(), e.getMessage()));
            }

            progress.set(1.0 * nrProcessedAnnotations.incrementAndGet() / totalNrOfAnnotations);
        });

        return new ImageAnnotationExportResult(
                annotations.imageAnnotations().size() - unParsedFileErrorMessages.size(),
                unParsedFileErrorMessages
        );
    }

    private void createXmlFileFromImageAnnotationDataElement(final ImageAnnotation dataElement)
            throws TransformerException, ParserConfigurationException {
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();

        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        final Element annotationElement = document.createElement(ROOT_ELEMENT_NAME);
        document.appendChild(annotationElement);

        appendHeaderFromImageAnnotationDataElement(document, annotationElement, dataElement);

        dataElement.getBoundingShapeData().forEach(boundingShape ->
                                                           annotationElement.appendChild(
                                                                   createXmlElementFromBoundingShapeData(document,
                                                                                                         BOUNDING_SHAPE_ENTRY_ELEMENT_NAME,
                                                                                                         boundingShape,
                                                                                                         dataElement
                                                                                                                 .getImageMetaData()))
        );

        DOMSource domSource = new DOMSource(document);

        String fileName = dataElement.getImageFileName();
        String annotationFileNameBase = fileName.replace('.', '_');

        File outputFile =
                saveFolderPath.resolve(annotationFileNameBase + ANNOTATION_FILENAME_EXTENSION + FILE_EXTENSION)
                              .toFile();

        StreamResult streamResult = new StreamResult(outputFile);

        transformer.transform(domSource, streamResult);
    }

    private void appendHeaderFromImageAnnotationDataElement(final Document document, final Node root,
                                                            final ImageAnnotation dataElement) {
        root.appendChild(
                createStringValueElement(document, FOLDER_ELEMENT_NAME, dataElement.getContainingFolderName()));
        root.appendChild(createStringValueElement(document, FILENAME_ELEMENT_NAME, dataElement.getImageFileName()));

        final Element sizeElement = document.createElement(IMAGE_SIZE_ELEMENT_NAME);
        root.appendChild(sizeElement);

        sizeElement
                .appendChild(createDoubleValueElement(document, IMAGE_WIDTH_ELEMENT_NAME, dataElement.getImageWidth()));
        sizeElement.appendChild(
                createDoubleValueElement(document, IMAGE_HEIGHT_ELEMENT_NAME, dataElement.getImageHeight()));
        sizeElement.appendChild(
                createIntegerValueElement(document, IMAGE_DEPTH_ELEMENT_NAME, dataElement.getImageDepth()));
    }

    private Element createXmlElementFromBoundingShapeData(final Document document, String elementName,
                                                          final BoundingShapeData boundingShapeData,
                                                          final ImageMetaData imageMetaData) {
        final Element element = document.createElement(elementName);
        element.appendChild(
                createStringValueElement(document, BOUNDING_SHAPE_CATEGORY_NAME, boundingShapeData.getCategoryName()));

        // Add tags:
        int difficultValue = 0;
        int occludedValue = 0;
        int truncatedValue = 0;
        String poseString = "Unspecified";
        List<String> actionTags = new ArrayList<>();

        for(String tag : boundingShapeData.getTags()) {
            String lowerCaseTag = tag.toLowerCase();

            if(lowerCaseTag.startsWith("pose:")) {
                poseString = StringUtils.capitalize(lowerCaseTag.substring(5).stripLeading());
            } else if(lowerCaseTag.startsWith("action:")) {
                actionTags.add(lowerCaseTag.substring(7).stripLeading());
            } else if(lowerCaseTag.equals("difficult")) {
                difficultValue = 1;
            } else if(lowerCaseTag.equals("occluded")) {
                occludedValue = 1;
            } else if(lowerCaseTag.equals("truncated")) {
                truncatedValue = 1;
            }
        }

        element.appendChild(createIntegerValueElement(document, "difficult", difficultValue));
        element.appendChild(createIntegerValueElement(document, "occluded", occludedValue));
        element.appendChild(createStringValueElement(document, "pose", poseString));
        element.appendChild(createIntegerValueElement(document, "truncated", truncatedValue));

        // Add action tags:
        if(!actionTags.isEmpty()) {
            Element actionsElement = document.createElement(ACTIONS_TAG_NAME);
            element.appendChild(actionsElement);

            actionTags.forEach(action ->
                                       actionsElement.appendChild(createIntegerValueElement(document, action, 1)));
        }

        // Add coordinates:
        element.appendChild(boundingShapeData.accept(new XmlElementVisitor(document, imageMetaData.getImageWidth(),
                                                                           imageMetaData.getImageHeight())));

        // Add parts:
        boundingShapeData.getParts().forEach(part ->
                                                     element.appendChild(createXmlElementFromBoundingShapeData(document,
                                                                                                               BOUNDING_SHAPE_PART_NAME,
                                                                                                               part,
                                                                                                               imageMetaData))
        );

        return element;
    }

    private Element createDoubleValueElement(Document document, String tagName, double value) {
        Element element = document.createElement(tagName);
        element.appendChild(document.createTextNode(DECIMAL_FORMAT.format(value)));
        return element;
    }

    private Element createIntegerValueElement(Document document, String tagName, int value) {
        Element element = document.createElement(tagName);
        element.appendChild(document.createTextNode(Integer.toString(value)));
        return element;
    }

    private Element createStringValueElement(Document document, String tagName, String value) {
        Element element = document.createElement(tagName);
        element.appendChild(document.createTextNode(value));
        return element;
    }

    private class XmlElementVisitor implements BoundingShapeDataVisitor<Element> {
        private final Document document;
        private final double imageWidth;
        private final double imageHeight;

        public XmlElementVisitor(Document document, double imageWidth, double imageHeight) {
            this.document = document;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        @Override
        public Element visit(BoundingBoxData boundingBoxData) {
            Element coordinateElement = document.createElement(BOUNDING_BOX_SIZE_GROUP_NAME);

            Bounds absoluteBounds = boundingBoxData.getAbsoluteBoundsInImage(imageWidth, imageHeight);

            coordinateElement.appendChild(createDoubleValueElement(document, XMIN_TAG, absoluteBounds.getMinX()));
            coordinateElement.appendChild(createDoubleValueElement(document, XMAX_TAG, absoluteBounds.getMaxX()));
            coordinateElement.appendChild(createDoubleValueElement(document, YMIN_TAG, absoluteBounds.getMinY()));
            coordinateElement.appendChild(createDoubleValueElement(document, YMAX_TAG, absoluteBounds.getMaxY()));

            return coordinateElement;
        }

        @Override
        public Element visit(BoundingPolygonData boundingPolygonData) {
            Element coordinateElement = document.createElement(BOUNDING_POLYGON_SIZE_GROUP_NAME);

            List<Double> absolutePoints = boundingPolygonData.getAbsolutePointsInImage(imageWidth, imageHeight);

            for(int i = 0; i < absolutePoints.size(); i += 2) {
                coordinateElement.appendChild(createDoubleValueElement(document, "x", absolutePoints.get(i)));
                coordinateElement.appendChild(createDoubleValueElement(document, "y", absolutePoints.get(i + 1)));
            }

            return coordinateElement;
        }

        @Override
        public Element visit(BoundingFreehandShapeData boundingFreehandShapeData) {
            Element coordinateElement = document.createElement("path");

            List<Double> absolutePoints = boundingFreehandShapeData.getAbsolutePathPoints(imageWidth, imageHeight);

            for(int i = 0; i < absolutePoints.size(); i += 2) {
                coordinateElement.appendChild(createDoubleValueElement(document, "x", absolutePoints.get(i)));
                coordinateElement.appendChild(createDoubleValueElement(document, "y", absolutePoints.get(i + 1)));
            }

            return coordinateElement;
        }
    }
}
