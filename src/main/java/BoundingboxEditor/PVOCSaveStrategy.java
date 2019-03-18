package BoundingboxEditor;

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
    //private static final String IMAGE_DEPTH_ELEMENT_NAME = "depth";
    private static final String BOUNDING_BOX_ENTRY_ELEMENT_NAME = "object";
    private static final String BOUNDING_BOX_CATEGORY_NAME = "name";
    private static final String BOUNDING_BOX_SIZE_GROUP_NAME = "bndBox";

    private static final DecimalFormat floatingpointFormat = new DecimalFormat("#.##");
    private static final String XML_FILE_EXTENSION = ".xml";

    @Override
    public void save(final Collection<ImageAnnotationDataElement> dataSet, final Path path) throws Exception {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
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

        File outputFile = new File(path.toString().concat("\\").concat(fileNameWithoutExtension).concat(XML_FILE_EXTENSION));

        StreamResult streamResult = new StreamResult(outputFile);

        transformer.transform(domSource, streamResult);
    }

    private void appendHeaderFromImageAnnotationDataElement(final Document document, final Node root, final ImageAnnotationDataElement dataElement) {
        final Element folderElement = document.createElement(FOLDER_ELEMENT_NAME);
        folderElement.appendChild(document.createTextNode(dataElement.getContainingFolderName()));
        root.appendChild(folderElement);

        final Element fileNameElement = document.createElement(FILENAME_ELEMENT_NAME);
        fileNameElement.appendChild(document.createTextNode(dataElement.getImageFileName()));
        root.appendChild(fileNameElement);

        final Element pathElement = document.createElement(PATH_ELEMENT_NAME);
        pathElement.appendChild(document.createTextNode(dataElement.getImagePath().toString()));
        root.appendChild(pathElement);

        final Element sizeElement = document.createElement(IMAGE_SIZE_ELEMENT_NAME);
        root.appendChild(sizeElement);

        final Element widthElement = document.createElement(IMAGE_WIDTH_ELEMENT_NAME);
        widthElement.appendChild(document.createTextNode(Double.valueOf(floatingpointFormat.format(dataElement.getImageWidth())).toString()));
        sizeElement.appendChild(widthElement);

        final Element heightElement = document.createElement(IMAGE_HEIGHT_ELEMENT_NAME);
        heightElement.appendChild(document.createTextNode(Double.valueOf(floatingpointFormat.format(dataElement.getImageHeight())).toString()));
        sizeElement.appendChild(heightElement);

//        final Element depthElement = document.createElement(IMAGE_DEPTH_ELEMENT_NAME);
//        depthElement.appendChild(document.createTextNode(Double.valueOf(floatingpointFormat.format(dataElement.getImageDepth())).toString()));
//        sizeElement.appendChild(depthElement);
    }


    private Element createXmlElementFromBoundingBox(final Document document, final BoundingBoxElement boundingBox) {
        final Element object = document.createElement(BOUNDING_BOX_ENTRY_ELEMENT_NAME);

        final Element name = document.createElement(BOUNDING_BOX_CATEGORY_NAME);
        name.appendChild(document.createTextNode(boundingBox.getCategoryName()));
        object.appendChild(name);

        final Element bndBox = document.createElement(BOUNDING_BOX_SIZE_GROUP_NAME);
        object.appendChild(bndBox);

        final Element xmin = document.createElement("xmin");
        xmin.appendChild(document.createTextNode(Double.valueOf(floatingpointFormat.format(boundingBox.getxMin())).toString()));
        bndBox.appendChild(xmin);

        final Element xmax = document.createElement("xmax");
        xmax.appendChild(document.createTextNode(Double.valueOf(floatingpointFormat.format(boundingBox.getxMax())).toString()));
        bndBox.appendChild(xmax);

        final Element ymin = document.createElement("ymin");
        ymin.appendChild(document.createTextNode(Double.valueOf(floatingpointFormat.format(boundingBox.getyMin())).toString()));
        bndBox.appendChild(ymin);

        final Element ymax = document.createElement("ymax");
        ymax.appendChild(document.createTextNode(Double.valueOf(floatingpointFormat.format(boundingBox.getyMax())).toString()));
        bndBox.appendChild(ymax);

        return object;
    }
}
