package BoundingBoxEditor.model.io;

import BoundingBoxEditor.model.BoundingBoxCategory;
import BoundingBoxEditor.model.ImageMetaData;
import BoundingBoxEditor.model.Model;
import BoundingBoxEditor.utils.ColorUtils;
import javafx.beans.property.DoubleProperty;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements the loading of xml-files containing image-annotations in the
 * 'PASCAL Visual Object Classes (Pascal VOC)'-format.
 *
 * @see <a href="http://host.robots.ox.ac.uk/pascal/VOC/">Pascal VOC</a>
 */
public class PVOCLoadStrategy implements ImageAnnotationLoadStrategy {
    private static final boolean INCLUDE_SUBDIRECTORIES = false;
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private Set<String> fileNamesToLoad;
    private Map<String, BoundingBoxCategory> existingBoundingBoxCategories;
    private Map<String, Integer> boundingBoxCountPerCategory;

    @Override
    public IOResult load(Model model, Path path, DoubleProperty progress) throws IOException {
        long startTime = System.nanoTime();

        this.fileNamesToLoad = model.getImageFileNameSet();
        this.boundingBoxCountPerCategory = new ConcurrentHashMap<>(model.getCategoryToAssignedBoundingBoxesCountMap());
        this.existingBoundingBoxCategories = new ConcurrentHashMap<>(model.getBoundingBoxCategories().stream()
                .collect(Collectors.toMap(BoundingBoxCategory::getName, Function.identity())));

        try(Stream<Path> fileStream = Files.walk(path, INCLUDE_SUBDIRECTORIES ? Integer.MAX_VALUE : 1)) {
            List<File> annotationFiles = fileStream
                    .filter(pathItem -> pathItem.getFileName().toString().endsWith(".xml"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            List<IOResult.ErrorInfoEntry> unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());

            int totalNrOfFiles = annotationFiles.size();
            AtomicInteger nrProcessedFiles = new AtomicInteger(0);

            List<ImageAnnotation> imageAnnotations = annotationFiles.parallelStream()
                    .map(file -> {
                        progress.set(1.0 * nrProcessedFiles.incrementAndGet() / totalNrOfFiles);

                        try {
                            return parseAnnotationFile(file);
                        } catch(SAXException | IOException | InvalidAnnotationFileFormatException
                                | ParserConfigurationException | AnnotationToNonExistentImageException e) {
                            unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(file.getName(), e.getMessage()));
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            model.getBoundingBoxCategories().setAll(existingBoundingBoxCategories.values());
            model.getCategoryToAssignedBoundingBoxesCountMap().putAll(boundingBoxCountPerCategory);
            model.updateImageAnnotations(imageAnnotations);

            long estimatedTime = System.nanoTime() - startTime;

            return new IOResult(
                    IOResult.OperationType.ANNOTATION_IMPORT,
                    imageAnnotations.size(),
                    TimeUnit.MILLISECONDS.convert(Duration.ofNanos(estimatedTime)),
                    unParsedFileErrorMessages
            );
        }
    }

    private ImageAnnotation parseAnnotationFile(File file) throws SAXException, IOException,
            InvalidAnnotationFileFormatException, ParserConfigurationException {
        final Document document = documentBuilderFactory.newDocumentBuilder().parse(file);
        document.normalize();

        ImageMetaData imageMetaData = parseImageMetaData(document);

        if(!fileNamesToLoad.contains(imageMetaData.getFileName())) {
            throw new AnnotationToNonExistentImageException("The image file does not belong to the currently loaded images.");
        }

        List<BoundingBoxData> boundingBoxData = parseBoundingBoxData(document);

        return new ImageAnnotation(imageMetaData, boundingBoxData);
    }

    private ImageMetaData parseImageMetaData(Document document) throws InvalidAnnotationFileFormatException {
        String folderName = parseTextElement(document, "folder");
        String fileName = parseTextElement(document, "filename");
        double width = parseDoubleElement(document, "width");
        double height = parseDoubleElement(document, "height");
        int depth = parseIntElement(document, "depth");

        return new ImageMetaData(fileName, folderName, width, height, depth);
    }

    private List<BoundingBoxData> parseBoundingBoxData(Document document) throws InvalidAnnotationFileFormatException {
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
        NodeList childElements = objectElement.getChildNodes();

        String categoryName = "";
        double xMin = Double.NaN;
        double xMax = Double.NaN;
        double yMin = Double.NaN;
        double yMax = Double.NaN;
        List<String> tags = new ArrayList<>();
        List<BoundingBoxData> parts = new ArrayList<>();

        for(int i = 0; i != childElements.getLength(); ++i) {
            Node currentChild = childElements.item(i);

            if(currentChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element currentElement = (Element) currentChild;

            switch(currentElement.getTagName()) {
                case "name":
                    categoryName = currentElement.getTextContent();

                    if(categoryName == null || categoryName.isBlank()) {
                        throw new InvalidAnnotationFileFormatException("Blank category-name");
                    }

                    break;
                case "bndbox":
                    xMin = parseDoubleElement(currentElement, "xmin");
                    xMax = parseDoubleElement(currentElement, "xmax");
                    yMin = parseDoubleElement(currentElement, "ymin");
                    yMax = parseDoubleElement(currentElement, "ymax");
                    break;
                case "pose":
                    String poseValue = currentElement.getTextContent();

                    if(poseValue != null && !poseValue.equalsIgnoreCase("unspecified")) {
                        tags.add("pose: " + poseValue.toLowerCase());
                    }

                    break;
                case "truncated":
                    if(Integer.parseInt(currentElement.getTextContent()) == 1) {
                        tags.add("truncated");
                    }

                    break;
                case "occluded":
                    if(Integer.parseInt(currentElement.getTextContent()) == 1) {
                        tags.add("occluded");
                    }

                    break;
                case "difficult":
                    if(Integer.parseInt(currentElement.getTextContent()) == 1) {
                        tags.add("difficult");
                    }

                    break;
                case "part":
                    parts.add(parseBoundingBoxElement(currentElement));
                    break;
                case "actions":
                    tags.addAll(parseActions(currentElement));
            }
        }

        if(categoryName.isEmpty()) {
            throw new InvalidAnnotationFileFormatException("Missing \"name\"-element.");
        }

        if(Double.isNaN(xMin) || Double.isNaN(xMax) || Double.isNaN(yMin) || Double.isNaN(yMax)) {
            throw new InvalidAnnotationFileFormatException("Missing \"bndbox\"-element.");
        }

        BoundingBoxCategory category = existingBoundingBoxCategories.computeIfAbsent(categoryName,
                key -> new BoundingBoxCategory(key, ColorUtils.createRandomColor()));

        boundingBoxCountPerCategory.merge(category.getName(), 1, Integer::sum);

        BoundingBoxData boundingBoxData = new BoundingBoxData(category, xMin, yMin, xMax, yMax, tags);

        if(!parts.isEmpty()) {
            boundingBoxData.setParts(parts);
        }

        return boundingBoxData;
    }

    private List<String> parseActions(Element element) {
        NodeList childList = element.getChildNodes();
        List<String> actions = new ArrayList<>();

        for(int i = 0; i != childList.getLength(); ++i) {
            Node childNode = childList.item(i);

            if(childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element childElement = (Element) childNode;

            if(Integer.parseInt(childElement.getTextContent()) == 1) {
                actions.add("action: " + childElement.getTagName());
            }
        }

        return actions;
    }

    private String parseTextElement(Document document, String tagName) throws InvalidAnnotationFileFormatException {
        Node textNode = document.getElementsByTagName(tagName).item(0);

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

    private int parseIntElement(Document document, String tagName) throws InvalidAnnotationFileFormatException {
        Node intNode = document.getElementsByTagName(tagName).item(0);

        if(intNode == null) {
            throw new InvalidAnnotationFileFormatException("Missing \"" + tagName + "\"-element.");
        }

        return Integer.parseInt(intNode.getTextContent());
    }

    private static class AnnotationToNonExistentImageException extends RuntimeException {
        AnnotationToNonExistentImageException(String message) {
            super(message);
        }
    }

    private static class InvalidAnnotationFileFormatException extends RuntimeException {
        InvalidAnnotationFileFormatException(String message) {
            super(message);
        }
    }
}
