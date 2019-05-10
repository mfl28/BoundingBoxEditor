package BoundingboxEditor.model.io;

import BoundingboxEditor.exceptions.InvalidAnnotationFileFormatException;
import BoundingboxEditor.model.ImageMetaData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PVOCLoader implements ImageAnnotationLoader {
    private static int MAX_DIRECTORY_DEPTH = 1;
    private final DocumentBuilder documentBuilder;
    private final Set<String> loadableFileNames;

    public PVOCLoader(Set<String> loadableFileNames) throws ParserConfigurationException {
        this.loadableFileNames = loadableFileNames;
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    @Override
    public List<ImageAnnotationDataElement> load(Path path) throws IOException {
        List<File> annotationFiles = Files.walk(path, MAX_DIRECTORY_DEPTH)
                .filter(this::validateFilename)
                .map(Path::toFile)
                .collect(Collectors.toList());

        int numberFiles = annotationFiles.size();
        int numberNotParsedFiles = 0;

        List<ImageAnnotationDataElement> imageAnnotations = new ArrayList<>(numberFiles);

        for(File element : annotationFiles) {
            try {
                imageAnnotations.add(parseAnnotationFile(element));
            } catch(Exception e) {
                ++numberNotParsedFiles;
            }
        }

        return imageAnnotations;
    }

    private ImageAnnotationDataElement parseAnnotationFile(File file) throws SAXException, IOException, InvalidAnnotationFileFormatException {
        final Document document = documentBuilder.parse(file);
        document.normalize();

        ImageMetaData imageMetaData = parseImageMetaData(document);
        List<BoundingBoxElement> boundingBoxElements = parseBoundingBoxElements(document);

        return new ImageAnnotationDataElement(imageMetaData, boundingBoxElements);
    }

    private ImageMetaData parseImageMetaData(Document document) throws InvalidAnnotationFileFormatException {
        String path = parseTextElement(document, "path");
        double width = parseDoubleElement(document, "width");
        double height = parseDoubleElement(document, "height");

        return new ImageMetaData(path, width, height);
    }

    private List<BoundingBoxElement> parseBoundingBoxElements(Document document) throws InvalidAnnotationFileFormatException {
        NodeList objectElements = document.getElementsByTagName("object");

        List<BoundingBoxElement> boundingBoxElements = new ArrayList<>();

        for(int i = 0; i != objectElements.getLength(); ++i) {
            Node objectNode = objectElements.item(i);

            if(objectNode.getNodeType() == Node.ELEMENT_NODE) {
                boundingBoxElements.add(parseBoundingBoxElement((Element) objectNode));
            }
        }

        return boundingBoxElements;
    }

    private BoundingBoxElement parseBoundingBoxElement(Element objectElement) {
        String categoryName = parseTextElement(objectElement, "name");
        double xMin = parseDoubleElement(objectElement, "xmin");
        double xMax = parseDoubleElement(objectElement, "xmax");
        double yMin = parseDoubleElement(objectElement, "ymin");
        double yMax = parseDoubleElement(objectElement, "ymax");

        return new BoundingBoxElement(categoryName, xMin, yMin, xMax, yMax);
    }


    private String parseTextElement(Document document, String tagName) throws InvalidAnnotationFileFormatException {
        Node textNode = document.getElementsByTagName(tagName).item(0);

        if(textNode == null) {
            throw new InvalidAnnotationFileFormatException("Element " + tagName + " does not exits.");
        }

        return textNode.getTextContent();
    }

    private String parseTextElement(Element element, String tagName) throws InvalidAnnotationFileFormatException {
        Node textNode = element.getElementsByTagName(tagName).item(0);

        if(textNode == null) {
            throw new InvalidAnnotationFileFormatException("Element " + tagName + " does not exits.");
        }

        return textNode.getTextContent();
    }

    private double parseDoubleElement(Document document, String tagName) throws InvalidAnnotationFileFormatException {
        Node doubleNode = document.getElementsByTagName(tagName).item(0);

        if(doubleNode == null) {
            throw new InvalidAnnotationFileFormatException("Element " + tagName + " does not exits.");
        }

        return Double.parseDouble(doubleNode.getTextContent());
    }

    private double parseDoubleElement(Element element, String tagName) throws InvalidAnnotationFileFormatException {
        Node doubleNode = element.getElementsByTagName(tagName).item(0);

        if(doubleNode == null) {
            throw new InvalidAnnotationFileFormatException("Element " + tagName + " does not exits.");
        }

        return Double.parseDouble(doubleNode.getTextContent());
    }

    private boolean validateFilename(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return fileName.endsWith("_A.xml") &&
                loadableFileNames.contains(fileName.substring(0, fileName.lastIndexOf('_')));
    }

}
