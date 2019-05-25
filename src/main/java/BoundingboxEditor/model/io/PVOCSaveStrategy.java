package BoundingboxEditor.model.io;

import javafx.beans.property.DoubleProperty;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PVOCSaveStrategy implements ImageAnnotationSaveStrategy {

    private static final String ROOT_ELEMENT_NAME = "annotation";
    private static final String FOLDER_ELEMENT_NAME = "folder";
    private static final String FILENAME_ELEMENT_NAME = "filename";
    private static final String IMAGE_SIZE_ELEMENT_NAME = "size";
    private static final String IMAGE_WIDTH_ELEMENT_NAME = "width";
    private static final String IMAGE_HEIGHT_ELEMENT_NAME = "height";
    private static final String BOUNDING_BOX_ENTRY_ELEMENT_NAME = "object";
    private static final String BOUNDING_BOX_CATEGORY_NAME = "name";
    private static final String BOUNDING_BOX_SIZE_GROUP_NAME = "bndBox";

    private static final DecimalFormat floatingPointFormat = new DecimalFormat("#.##");
    private static final String FILE_EXTENSION = ".xml";
    private static final String XMIN_TAG = "xmin";
    private static final String XMAX_TAG = "xmax";
    private static final String YMIN_TAG = "ymin";
    private static final String YMAX_TAG = "ymax";
    private static final String ANNOTATION_FILENAME_EXTENSION = "_A";
    private static final String BOUNDING_BOX_PART_NAME = "part";
    private static final Logger logger = LoggerFactory.getLogger(PVOCSaveStrategy.class);
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private Path saveFolderPath;

    @Override
    public IOResult save(final Collection<ImageAnnotationDataElement> dataSet, final Path path, final DoubleProperty progress) {
        this.saveFolderPath = path;

        List<IOResult.ErrorTableEntry> unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());

        int totalNrOfAnnotations = dataSet.size();
        AtomicInteger nrProcessedAnnotations = new AtomicInteger(0);

        long startTime = System.nanoTime();
        dataSet.parallelStream().forEach(annotation -> {
            try {
                createXmlFileFromImageAnnotationDataElement(annotation);
            } catch(TransformerException | ParserConfigurationException e) {
                unParsedFileErrorMessages.add(new IOResult.ErrorTableEntry(annotation.getImageFileName(), e.getMessage()));
            }

            progress.set(1.0 * nrProcessedAnnotations.incrementAndGet() / totalNrOfAnnotations);
        });
        long duration = System.nanoTime() - startTime;

        return new IOResult(dataSet.size() - unParsedFileErrorMessages.size(),
                TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS), unParsedFileErrorMessages);
    }

    private void createXmlFileFromImageAnnotationDataElement(final ImageAnnotationDataElement dataElement) throws TransformerException, ParserConfigurationException {
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        final Element annotationElement = document.createElement(ROOT_ELEMENT_NAME);
        document.appendChild(annotationElement);

        appendHeaderFromImageAnnotationDataElement(document, annotationElement, dataElement);

        dataElement.getBoundingBoxes().forEach(boundingBox ->
                annotationElement.appendChild(createXmlElementFromBoundingBox(document, BOUNDING_BOX_ENTRY_ELEMENT_NAME, boundingBox)));

        DOMSource domSource = new DOMSource(document);

        String fileName = dataElement.getImageFileName();
        String annotationFileNameBase = fileName.replace('.', '_');

        File outputFile = new File(saveFolderPath.toString().concat("\\")
                .concat(annotationFileNameBase)
                .concat(ANNOTATION_FILENAME_EXTENSION)
                .concat(FILE_EXTENSION));

        StreamResult streamResult = new StreamResult(outputFile);

        transformer.transform(domSource, streamResult);
    }

    private void appendHeaderFromImageAnnotationDataElement(final Document document, final Node root, final ImageAnnotationDataElement dataElement) {
        root.appendChild(createStringValueElement(document, FOLDER_ELEMENT_NAME, dataElement.getContainingFolderName()));
        root.appendChild(createStringValueElement(document, FILENAME_ELEMENT_NAME, dataElement.getImageFileName()));

        final Element sizeElement = document.createElement(IMAGE_SIZE_ELEMENT_NAME);
        root.appendChild(sizeElement);

        sizeElement.appendChild(createDoubleValueElement(document, IMAGE_WIDTH_ELEMENT_NAME, dataElement.getImageWidth()));
        sizeElement.appendChild(createDoubleValueElement(document, IMAGE_HEIGHT_ELEMENT_NAME, dataElement.getImageHeight()));
    }


    private Element createXmlElementFromBoundingBox(final Document document, String elementName, final BoundingBoxData boundingBox) {
        final Element element = document.createElement(elementName);
        element.appendChild(createStringValueElement(document, BOUNDING_BOX_CATEGORY_NAME, boundingBox.getCategoryName()));

        // add tags
        List<String> tags = boundingBox.getTags();
        String poseString = tags.stream().filter(tag -> tag.startsWith("pose: ")).findFirst().orElse(null);

        element.appendChild(createIntegerValueElement(document, "difficult", tags.contains("difficult") ? 1 : 0));
        element.appendChild(createIntegerValueElement(document, "occluded", tags.contains("occluded") ? 1 : 0));
        element.appendChild(createStringValueElement(document, "pose", poseString == null ?
                "Unspecified" : StringUtils.capitalize(poseString.substring(6).toLowerCase())));
        element.appendChild(createIntegerValueElement(document, "truncated", tags.contains("truncated") ? 1 : 0));


        final Element bndBox = document.createElement(BOUNDING_BOX_SIZE_GROUP_NAME);
        element.appendChild(bndBox);

        bndBox.appendChild(createDoubleValueElement(document, XMIN_TAG, boundingBox.getXMin()));
        bndBox.appendChild(createDoubleValueElement(document, XMAX_TAG, boundingBox.getXMax()));
        bndBox.appendChild(createDoubleValueElement(document, YMIN_TAG, boundingBox.getYMin()));
        bndBox.appendChild(createDoubleValueElement(document, YMAX_TAG, boundingBox.getYMax()));

        // add parts
        boundingBox.getParts().forEach(part ->
                element.appendChild(createXmlElementFromBoundingBox(document, BOUNDING_BOX_PART_NAME, part)));

        return element;
    }

    private Element createDoubleValueElement(Document document, String tagName, double value) {
        Element element = document.createElement(tagName);
        element.appendChild(document.createTextNode(Double.valueOf(floatingPointFormat.format(value)).toString()));
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
