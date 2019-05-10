package BoundingboxEditor.model.io;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Collection;

public class PVOCSaveStrategy implements ImageAnnotationsSaveStrategy {

    private static final String ROOT_ELEMENT_NAME = "annotation";
    private static final String FOLDER_ELEMENT_NAME = "folder";
    private static final String FILENAME_ELEMENT_NAME = "filename";
    private static final String PATH_ELEMENT_NAME = "path";
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
    private static final String FILENAME_ANNOTATION_EXTENSION = "_A";

    @Override
    public void save(final Collection<ImageAnnotationDataElement> dataSet, final Path path) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        for(final ImageAnnotationDataElement dataElement : dataSet) {
            createXmlFileFromImageAnnotationDataElement(documentBuilder, transformer, dataElement, path);
        }
    }

    private void createXmlFileFromImageAnnotationDataElement(final DocumentBuilder documentBuilder,
                                                             final Transformer transformer,
                                                             final ImageAnnotationDataElement dataElement,
                                                             final Path path) throws TransformerException {
        final Document document = documentBuilder.newDocument();

        final Element annotationElement = document.createElement(ROOT_ELEMENT_NAME);
        document.appendChild(annotationElement);

        appendHeaderFromImageAnnotationDataElement(document, annotationElement, dataElement);

        for(BoundingBoxElement boundingBox : dataElement.getBoundingBoxes()) {
            annotationElement.appendChild(createXmlElementFromBoundingBox(document, boundingBox));
        }

        DOMSource domSource = new DOMSource(document);

        String fileName = dataElement.getImageFileName();
        String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

        File outputFile = new File(path.toString().concat("\\")
                .concat(fileNameWithoutExtension)
                .concat(FILENAME_ANNOTATION_EXTENSION)
                .concat(FILE_EXTENSION));

        StreamResult streamResult = new StreamResult(outputFile);

        transformer.transform(domSource, streamResult);
    }

    private void appendHeaderFromImageAnnotationDataElement(final Document document, final Node root, final ImageAnnotationDataElement dataElement) {
        root.appendChild(createStringValueElement(document, FOLDER_ELEMENT_NAME, dataElement.getContainingFolderName()));
        root.appendChild(createStringValueElement(document, FILENAME_ELEMENT_NAME, dataElement.getImageFileName()));
        root.appendChild(createStringValueElement(document, PATH_ELEMENT_NAME, dataElement.getImagePath().toString()));

        final Element sizeElement = document.createElement(IMAGE_SIZE_ELEMENT_NAME);
        root.appendChild(sizeElement);

        sizeElement.appendChild(createDoubleValueElement(document, IMAGE_WIDTH_ELEMENT_NAME, dataElement.getImageWidth()));
        sizeElement.appendChild(createDoubleValueElement(document, IMAGE_HEIGHT_ELEMENT_NAME, dataElement.getImageHeight()));
    }


    private Element createXmlElementFromBoundingBox(final Document document, final BoundingBoxElement boundingBox) {
        final Element object = document.createElement(BOUNDING_BOX_ENTRY_ELEMENT_NAME);
        object.appendChild(createStringValueElement(document, BOUNDING_BOX_CATEGORY_NAME, boundingBox.getCategoryName()));

        final Element bndBox = document.createElement(BOUNDING_BOX_SIZE_GROUP_NAME);
        object.appendChild(bndBox);

        bndBox.appendChild(createDoubleValueElement(document, XMIN_TAG, boundingBox.getXMin()));
        bndBox.appendChild(createDoubleValueElement(document, XMAX_TAG, boundingBox.getXMax()));
        bndBox.appendChild(createDoubleValueElement(document, YMIN_TAG, boundingBox.getYMin()));
        bndBox.appendChild(createDoubleValueElement(document, YMAX_TAG, boundingBox.getYMax()));

        return object;
    }

    private Element createDoubleValueElement(Document document, String tagName, double value) {
        Element element = document.createElement(tagName);
        element.appendChild(document.createTextNode(Double.valueOf(floatingPointFormat.format(value)).toString()));
        return element;
    }

    private Element createStringValueElement(Document document, String tagName, String value) {
        Element element = document.createElement(tagName);
        element.appendChild(document.createTextNode(value));
        return element;
    }
}
