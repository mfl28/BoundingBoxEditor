package boundingboxeditor.model.io;

import javafx.beans.property.DoubleProperty;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private static final String BOUNDING_BOX_ENTRY_ELEMENT_NAME = "object";
    private static final String BOUNDING_BOX_CATEGORY_NAME = "name";
    private static final String BOUNDING_BOX_SIZE_GROUP_NAME = "bndbox";

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static final String FILE_EXTENSION = ".xml";
    private static final String XMIN_TAG = "xmin";
    private static final String XMAX_TAG = "xmax";
    private static final String YMIN_TAG = "ymin";
    private static final String YMAX_TAG = "ymax";
    private static final String ANNOTATION_FILENAME_EXTENSION = "_A";
    private static final String BOUNDING_BOX_PART_NAME = "part";
    private static final String ACTIONS_TAG_NAME = "actions";
    private static final String IMAGE_DEPTH_ELEMENT_NAME = "depth";
    private static final String BOUNDING_POLYGON_SIZE_GROUP_NAME = "polygon";
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private Path saveFolderPath;

    @Override
    public IOResult save(Collection<ImageAnnotation> annotations, Path saveFolderPath, DoubleProperty progress) {
        long startTime = System.nanoTime();
        this.saveFolderPath = saveFolderPath;

        List<IOResult.ErrorInfoEntry> unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());

        int totalNrOfAnnotations = annotations.size();
        AtomicInteger nrProcessedAnnotations = new AtomicInteger(0);


        annotations.parallelStream().forEach(annotation -> {
            try {
                createXmlFileFromImageAnnotationDataElement(annotation);
            } catch(TransformerException | ParserConfigurationException e) {
                unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(annotation.getImageFileName(), e.getMessage()));
            }

            progress.set(1.0 * nrProcessedAnnotations.incrementAndGet() / totalNrOfAnnotations);
        });

        long duration = System.nanoTime() - startTime;

        return new IOResult(
                IOResult.OperationType.ANNOTATION_SAVING,
                annotations.size() - unParsedFileErrorMessages.size(),
                TimeUnit.MILLISECONDS.convert(Duration.ofNanos(duration)),
                unParsedFileErrorMessages
        );
    }

    private void createXmlFileFromImageAnnotationDataElement(final ImageAnnotation dataElement) throws TransformerException, ParserConfigurationException {
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        final Element annotationElement = document.createElement(ROOT_ELEMENT_NAME);
        document.appendChild(annotationElement);

        appendHeaderFromImageAnnotationDataElement(document, annotationElement, dataElement);

        dataElement.getBoundingShapeData().forEach(boundingShape ->
        {
            if(boundingShape instanceof BoundingBoxData) {
                annotationElement.appendChild(createXmlElementFromBoundingBox(document,
                        BOUNDING_BOX_ENTRY_ELEMENT_NAME, (BoundingBoxData) boundingShape));
            } else if(boundingShape instanceof BoundingPolygonData) {
                annotationElement.appendChild(createXmlElementFromBoundingPolygon(document,
                        BOUNDING_BOX_ENTRY_ELEMENT_NAME, (BoundingPolygonData) boundingShape));
            }
        });

        DOMSource domSource = new DOMSource(document);

        String fileName = dataElement.getImageFileName();
        String annotationFileNameBase = fileName.replace('.', '_');

        File outputFile = saveFolderPath.resolve(annotationFileNameBase + ANNOTATION_FILENAME_EXTENSION + FILE_EXTENSION).toFile();

        StreamResult streamResult = new StreamResult(outputFile);

        transformer.transform(domSource, streamResult);
    }

    private void appendHeaderFromImageAnnotationDataElement(final Document document, final Node root, final ImageAnnotation dataElement) {
        root.appendChild(createStringValueElement(document, FOLDER_ELEMENT_NAME, dataElement.getContainingFolderName()));
        root.appendChild(createStringValueElement(document, FILENAME_ELEMENT_NAME, dataElement.getImageFileName()));

        final Element sizeElement = document.createElement(IMAGE_SIZE_ELEMENT_NAME);
        root.appendChild(sizeElement);

        sizeElement.appendChild(createDoubleValueElement(document, IMAGE_WIDTH_ELEMENT_NAME, dataElement.getImageWidth()));
        sizeElement.appendChild(createDoubleValueElement(document, IMAGE_HEIGHT_ELEMENT_NAME, dataElement.getImageHeight()));
        sizeElement.appendChild(createIntegerValueElement(document, IMAGE_DEPTH_ELEMENT_NAME, dataElement.getImageDepth()));
    }


    private Element createXmlElementFromBoundingBox(final Document document, String elementName, final BoundingBoxData boundingBox) {
        final Element element = document.createElement(elementName);
        element.appendChild(createStringValueElement(document, BOUNDING_BOX_CATEGORY_NAME, boundingBox.getCategoryName()));

        // add tags:
        int difficultValue = 0;
        int occludedValue = 0;
        int truncatedValue = 0;
        String poseString = "Unspecified";
        List<String> actionTags = new ArrayList<>();

        for(String tag : boundingBox.getTags()) {
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

        // add action tags:
        if(!actionTags.isEmpty()) {
            Element actionsElement = document.createElement(ACTIONS_TAG_NAME);
            element.appendChild(actionsElement);

            actionTags.forEach(action ->
                    actionsElement.appendChild(createIntegerValueElement(document, action, 1)));
        }

        final Element bndBox = document.createElement(BOUNDING_BOX_SIZE_GROUP_NAME);
        element.appendChild(bndBox);

        bndBox.appendChild(createDoubleValueElement(document, XMIN_TAG, boundingBox.getXMin()));
        bndBox.appendChild(createDoubleValueElement(document, XMAX_TAG, boundingBox.getXMax()));
        bndBox.appendChild(createDoubleValueElement(document, YMIN_TAG, boundingBox.getYMin()));
        bndBox.appendChild(createDoubleValueElement(document, YMAX_TAG, boundingBox.getYMax()));

        // add parts
        boundingBox.getParts().forEach(part ->
        {
            if(part instanceof BoundingBoxData) {
                element.appendChild(createXmlElementFromBoundingBox(document,
                        BOUNDING_BOX_PART_NAME, (BoundingBoxData) part));
            } else if(part instanceof BoundingPolygonData) {
                element.appendChild(createXmlElementFromBoundingPolygon(document,
                        BOUNDING_BOX_PART_NAME, (BoundingPolygonData) part));
            }
        });

        return element;
    }

    private Element createXmlElementFromBoundingPolygon(final Document document, String elementName, final BoundingPolygonData boundingPolygon) {
        final Element element = document.createElement(elementName);
        element.appendChild(createStringValueElement(document, BOUNDING_BOX_CATEGORY_NAME, boundingPolygon.getCategoryName()));

        final Element polygon = document.createElement(BOUNDING_POLYGON_SIZE_GROUP_NAME);
        element.appendChild(polygon);

        List<Double> polygonPoints = boundingPolygon.getPointsInImage();

        for(int i = 0; i < polygonPoints.size(); i += 2) {
            polygon.appendChild(createDoubleValueElement(document, "x", polygonPoints.get(i)));
            polygon.appendChild(createDoubleValueElement(document, "y", polygonPoints.get(i + 1)));
        }

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
}
