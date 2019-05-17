package BoundingboxEditor.model.io;

import BoundingboxEditor.exceptions.AnnotationToNonExistentImageException;
import BoundingboxEditor.exceptions.InvalidAnnotationFileFormatException;
import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.ui.MainView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private Set<String> fileNamesToLoad;

    @Override
    public List<ImageAnnotationDataElement> load(final Set<String> fileNamesToLoad, final Path path) throws IOException {
        this.fileNamesToLoad = fileNamesToLoad;

        List<File> annotationFiles = Files.walk(path, MAX_DIRECTORY_DEPTH)
                .filter(pathItem -> pathItem.getFileName().toString().endsWith(".xml"))
                .map(Path::toFile)
                .collect(Collectors.toList());

        int numberFiles = annotationFiles.size();
        List<String> unparsedFileErrorMessages = new ArrayList<>();

        List<ImageAnnotationDataElement> imageAnnotations = new ArrayList<>(numberFiles);

//        for(File element : annotationFiles) {
//            try {
//                imageAnnotations.add(parseAnnotationFile(element));
//            }
//            catch(Exception e) {
//                unparsedFileErrorMessages.add("- " + element.getName() + ":\n\tError: " + e.getMessage());
//            }
//        }

        annotationFiles.parallelStream().forEach(file -> {
            try {
                imageAnnotations.add(parseAnnotationFile(file));
            } catch(Exception e) {
                unparsedFileErrorMessages.add("- " + file.getName() + ":\n\tError: " + e.getMessage());
            }
        });

        if(!unparsedFileErrorMessages.isEmpty()) {
            MainView.displayInfoAlert("Annotation import error report", "There were errors while loading annotations.",
                    unparsedFileErrorMessages.size() + " image-annotation file(s) could not be loaded.",
                    String.join("\n", unparsedFileErrorMessages));
        }

        return imageAnnotations;
    }

    private ImageAnnotationDataElement parseAnnotationFile(File file) throws SAXException, IOException,
            InvalidAnnotationFileFormatException, ParserConfigurationException {
        final Document document = documentBuilderFactory.newDocumentBuilder().parse(file);
        document.normalize();

        ImageMetaData imageMetaData = parseImageMetaData(document);

        if(!fileNamesToLoad.contains(imageMetaData.getFileName())) {
            throw new AnnotationToNonExistentImageException("The image file " +
                    imageMetaData.getFileName() +
                    " is not among the currently loaded images.");
        }

        List<BoundingBoxData> boundingBoxData = parseBoundingBoxElements(document);

        return new ImageAnnotationDataElement(imageMetaData, boundingBoxData);
    }

    private ImageMetaData parseImageMetaData(Document document) throws InvalidAnnotationFileFormatException {
        String folderName = parseTextElement(document, "folder");
        String fileName = parseTextElement(document, "filename");
        double width = parseDoubleElement(document, "width");
        double height = parseDoubleElement(document, "height");

        return new ImageMetaData(fileName, folderName, width, height);
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
            throw new InvalidAnnotationFileFormatException("Missing \"" + tagName + "\"-element.");
        }

        return textNode.getTextContent();
    }

    private String parseTextElement(Element element, String tagName) throws InvalidAnnotationFileFormatException {
        Node textNode = element.getElementsByTagName(tagName).item(0);

        if(textNode == null) {
            throw new InvalidAnnotationFileFormatException("Missing \"" + tagName + "\"-element.");
        }

        return textNode.getTextContent();
    }

    private double parseDoubleElement(Document document, String tagName) throws InvalidAnnotationFileFormatException {
        Node doubleNode = document.getElementsByTagName(tagName).item(0);

        if(doubleNode == null) {
            throw new InvalidAnnotationFileFormatException("Missing \"" + tagName + "\"-element.");
        }

        return Double.parseDouble(doubleNode.getTextContent());
    }

    private double parseDoubleElement(Element element, String tagName) throws InvalidAnnotationFileFormatException {
        Node doubleNode = element.getElementsByTagName(tagName).item(0);

        if(doubleNode == null) {
            throw new InvalidAnnotationFileFormatException("Missing \"" + tagName + "\"-element.");
        }

        return Double.parseDouble(doubleNode.getTextContent());
    }
}
