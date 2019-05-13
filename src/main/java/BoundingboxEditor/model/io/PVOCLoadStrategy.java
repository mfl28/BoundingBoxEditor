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

public class PVOCLoadStrategy implements ImageAnnotationLoadStrategy {
    private static int MAX_DIRECTORY_DEPTH = 1;
    private final DocumentBuilder documentBuilder;

    public PVOCLoadStrategy() throws ParserConfigurationException {
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    @Override
    public List<ImageAnnotationDataElement> load(final Set<String> fileNamesToLoad, final Path path) throws IOException {
        FilePathValidator filePathValidator = new FilePathValidator(fileNamesToLoad);

        List<File> annotationFiles = Files.walk(path, MAX_DIRECTORY_DEPTH)
                .filter(filePathValidator::isValid)
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
        List<BoundingBoxData> boundingBoxData = parseBoundingBoxElements(document);

        return new ImageAnnotationDataElement(imageMetaData, boundingBoxData);
    }

    private ImageMetaData parseImageMetaData(Document document) throws InvalidAnnotationFileFormatException {
        String path = parseTextElement(document, "path");
        double width = parseDoubleElement(document, "width");
        double height = parseDoubleElement(document, "height");

        return new ImageMetaData(path, width, height);
    }

    private List<BoundingBoxData> parseBoundingBoxElements(Document document) throws InvalidAnnotationFileFormatException {
        NodeList objectElements = document.getElementsByTagName("object");

        List<BoundingBoxData> boundingBoxData = new ArrayList<>();

        for(int i = 0; i != objectElements.getLength(); ++i) {
            Node objectNode = objectElements.item(i);

            if(objectNode.getNodeType() == Node.ELEMENT_NODE) {
                boundingBoxData.add(parseBoundingBoxElement((Element) objectNode));
            }
        }

        return boundingBoxData;
    }

    private BoundingBoxData parseBoundingBoxElement(Element objectElement) {
        String categoryName = parseTextElement(objectElement, "name");
        double xMin = parseDoubleElement(objectElement, "xmin");
        double xMax = parseDoubleElement(objectElement, "xmax");
        double yMin = parseDoubleElement(objectElement, "ymin");
        double yMax = parseDoubleElement(objectElement, "ymax");

        return new BoundingBoxData(categoryName, xMin, yMin, xMax, yMax);
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

    private static class FilePathValidator {
        final Set<String> allowedFileNames;

        FilePathValidator(Set<String> allowedFileNames) {
            this.allowedFileNames = allowedFileNames;
        }

        boolean isValid(Path file) {
            String fileName = file.getFileName().toString();
            return fileName.endsWith("_A.xml") &&
                    allowedFileNames.contains(fileName.substring(0, fileName.lastIndexOf('_')));
        }
    }

}
