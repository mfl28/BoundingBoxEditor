package boundingboxeditor.model.io;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.Model;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.utils.ColorUtils;
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
    private static final String MISSING_ELEMENT_PREFIX = "Missing element: ";
    private static final String INVALID_OBJECT_ELEMENT_DUPL_ERROR = "Invalid \"object\"-element: " +
            "Contains \"bndbox\"- and \"polygon\"-elements.";
    private static final String INVALID_OBJECT_ELEMENT_MISSING_ERROR = "Invalid \"object\"-element: " +
            "Missing \"bndbox\"- or \"polygon\"-element.";
    private static final String INVALID_POLYGON_ELEMENT_ERROR = "Invalid \"polygon\"-element.";
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private Set<String> fileNamesToLoad;
    private Map<String, ObjectCategory> existingObjectCategories;
    private Map<String, Integer> boundingShapeCountPerCategory;
    private List<IOResult.ErrorInfoEntry> unParsedFileErrorMessages;

    @Override
    public IOResult load(Model model, Path path, DoubleProperty progress) throws IOException {
        long startTime = System.nanoTime();

        this.fileNamesToLoad = model.getImageFileNameSet();
        this.boundingShapeCountPerCategory = new ConcurrentHashMap<>(model.getCategoryToAssignedBoundingShapesCountMap());
        this.existingObjectCategories = new ConcurrentHashMap<>(model.getObjectCategories().stream()
                .collect(Collectors.toMap(ObjectCategory::getName, Function.identity())));

        try(Stream<Path> fileStream = Files.walk(path, INCLUDE_SUBDIRECTORIES ? Integer.MAX_VALUE : 1)) {
            List<File> annotationFiles = fileStream
                    .filter(pathItem -> pathItem.getFileName().toString().endsWith(".xml"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());

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

            model.getObjectCategories().setAll(existingObjectCategories.values());
            model.getCategoryToAssignedBoundingShapesCountMap().putAll(boundingShapeCountPerCategory);
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
            ParserConfigurationException {
        final Document document = documentBuilderFactory.newDocumentBuilder().parse(file);
        document.normalize();

        ImageMetaData imageMetaData = parseImageMetaData(document);

        if(!fileNamesToLoad.contains(imageMetaData.getFileName())) {
            throw new AnnotationToNonExistentImageException("The image file does not belong to the currently loaded images.");
        }

        List<BoundingShapeData> boundingBoxData = parseBoundingBoxData(document, file.getName());

        if(boundingBoxData.isEmpty()) {
            // No image annotation will be constructed if it does not contain any bounding boxes.
            return null;
        }

        return new ImageAnnotation(imageMetaData, boundingBoxData);
    }

    private ImageMetaData parseImageMetaData(Document document) {
        String folderName = parseTextElement(document, "folder");
        String fileName = parseTextElement(document, "filename");
        double width = parseDoubleElement(document, "width");
        double height = parseDoubleElement(document, "height");
        int depth = parseIntElement(document, "depth");

        return new ImageMetaData(fileName, folderName, width, height, depth);
    }

    private List<BoundingShapeData> parseBoundingBoxData(Document document, String filename) throws InvalidAnnotationFileFormatException {
        NodeList objectElements = document.getElementsByTagName("object");

        List<BoundingShapeData> boundingShapeData = new ArrayList<>();

        for(int i = 0; i != objectElements.getLength(); ++i) {
            Node objectNode = objectElements.item(i);

            if(objectNode.getNodeType() == Node.ELEMENT_NODE) {
                try {
                    BoundingShapeData boundingBoxData = parseBoundingShapeElement((Element) objectNode, filename);
                    boundingShapeData.add(boundingBoxData);
                } catch(InvalidAnnotationFileFormatException e) {
                    unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(filename, e.getMessage()));
                }
            }
        }

        return boundingShapeData;
    }

    private BoundingShapeData parseBoundingShapeElement(Element objectElement, String filename) {
        NodeList childElements = objectElement.getChildNodes();

        BoundingShapeDataParseResult boxDataParseResult = new BoundingShapeDataParseResult();

        // At first, parse all child elements except parts. In this way if errors occur,
        // no parts will be parsed.
        parseNonPartElements(childElements, boxDataParseResult);

        if(boxDataParseResult.getCategoryName() == null) {
            throw new InvalidAnnotationFileFormatException(MISSING_ELEMENT_PREFIX + "name");
        }

        if(boxDataParseResult.isBoundingBox() && boxDataParseResult.isBoundingPolygon()) {
            throw new InvalidAnnotationFileFormatException(INVALID_OBJECT_ELEMENT_DUPL_ERROR);
        } else if(!boxDataParseResult.isBoundingBox() && !boxDataParseResult.isBoundingPolygon()) {
            throw new InvalidAnnotationFileFormatException(INVALID_OBJECT_ELEMENT_MISSING_ERROR);
        } else if(boxDataParseResult.isBoundingBox()) {
            if(boxDataParseResult.getxMin() == null || boxDataParseResult.getxMax() == null
                    || boxDataParseResult.getyMin() == null || boxDataParseResult.getyMax() == null) {
                throw new InvalidAnnotationFileFormatException(MISSING_ELEMENT_PREFIX + "bndbox");
            }
        } else {
            if(boxDataParseResult.getPoints() == null) {
                throw new InvalidAnnotationFileFormatException(INVALID_POLYGON_ELEMENT_ERROR);
            }
        }


        ObjectCategory category = existingObjectCategories.computeIfAbsent(boxDataParseResult.getCategoryName(),
                key -> new ObjectCategory(key, ColorUtils.createRandomColor()));

        boundingShapeCountPerCategory.merge(category.getName(), 1, Integer::sum);

        BoundingShapeData boundingShapeData;

        if(boxDataParseResult.isBoundingBox()) {
            boundingShapeData = new BoundingBoxData(category, boxDataParseResult.getxMin(),
                    boxDataParseResult.getyMin(), boxDataParseResult.getxMax(),
                    boxDataParseResult.getyMax(), boxDataParseResult.getTags());
        } else {
            boundingShapeData = new BoundingPolygonData(category, boxDataParseResult.getPoints(),
                    boxDataParseResult.getTags());
        }

        // Now parse parts.
        parsePartElements(childElements, boxDataParseResult, filename);

        if(!boxDataParseResult.getParts().isEmpty()) {
            boundingShapeData.setParts(boxDataParseResult.getParts());
        }

        return boundingShapeData;
    }

    private void parseBoundingShapeDataTag(Element tagElement, BoundingShapeDataParseResult boxDataParseResult) {
        switch(tagElement.getTagName()) {
            case "name":
                String categoryName = tagElement.getTextContent();

                if(categoryName == null || categoryName.isBlank()) {
                    throw new InvalidAnnotationFileFormatException("Blank object name");
                }

                boxDataParseResult.setCategoryName(categoryName);
                break;
            case "bndbox":
                boxDataParseResult.setBoundingBox(true);
                boxDataParseResult.setxMin(parseDoubleElement(tagElement, "xmin"));
                boxDataParseResult.setxMax(parseDoubleElement(tagElement, "xmax"));
                boxDataParseResult.setyMin(parseDoubleElement(tagElement, "ymin"));
                boxDataParseResult.setyMax(parseDoubleElement(tagElement, "ymax"));
                break;
            case "polygon":
                boxDataParseResult.setBoundingPolygon(true);
                boxDataParseResult.setPoints(parsePointList(tagElement));
                break;
            case "pose":
                String poseValue = tagElement.getTextContent();

                if(poseValue != null && !poseValue.equalsIgnoreCase("unspecified")) {
                    boxDataParseResult.getTags().add("pose: " + poseValue.toLowerCase());
                }

                break;
            case "truncated":
                if(Integer.parseInt(tagElement.getTextContent()) == 1) {
                    boxDataParseResult.getTags().add("truncated");
                }

                break;
            case "occluded":
                if(Integer.parseInt(tagElement.getTextContent()) == 1) {
                    boxDataParseResult.getTags().add("occluded");
                }

                break;
            case "difficult":
                if(Integer.parseInt(tagElement.getTextContent()) == 1) {
                    boxDataParseResult.getTags().add("difficult");
                }

                break;
            case "actions":
                boxDataParseResult.getTags().addAll(parseActions(tagElement));
                break;
            default: // Unknown tags are ignored!
        }
    }

    private void parsePart(Element tagElement, BoundingShapeDataParseResult boxDataParseResult, String filename) {
        try {
            boxDataParseResult.getParts().add(parseBoundingShapeElement(tagElement, filename));
        } catch(InvalidAnnotationFileFormatException e) {
            unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(filename, e.getMessage()));
        }
    }

    private void parseNonPartElements(NodeList childElements, BoundingShapeDataParseResult boxDataParseResult) {
        for(int i = 0; i != childElements.getLength(); ++i) {
            Node currentChild = childElements.item(i);

            if(currentChild.getNodeType() == Node.ELEMENT_NODE) {
                Element currentElement = (Element) currentChild;

                if(!currentElement.getTagName().equals("part")) {
                    parseBoundingShapeDataTag((Element) currentChild, boxDataParseResult);
                }
            }
        }
    }

    private void parsePartElements(NodeList childElements, BoundingShapeDataParseResult boxDataParseResult, String filename) {
        for(int i = 0; i != childElements.getLength(); ++i) {
            Node currentChild = childElements.item(i);

            if(currentChild.getNodeType() == Node.ELEMENT_NODE) {
                Element currentElement = (Element) currentChild;

                if(currentElement.getTagName().equals("part")) {
                    parsePart((Element) currentChild, boxDataParseResult, filename);
                }
            }
        }
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

    private String parseTextElement(Document document, String tagName) {
        Node textNode = document.getElementsByTagName(tagName).item(0);

        if(textNode == null) {
            throw new InvalidAnnotationFileFormatException(MISSING_ELEMENT_PREFIX + tagName);
        }

        return textNode.getTextContent();
    }

    private List<Double> parsePointList(Element element) throws InvalidAnnotationFileFormatException {
        NodeList xNodes = element.getElementsByTagName("x");
        NodeList yNodes = element.getElementsByTagName("y");

        if(xNodes.getLength() == 0 || yNodes.getLength() == 0 || xNodes.getLength() != yNodes.getLength()) {
            throw new InvalidAnnotationFileFormatException("Invalid polygon element.");
        }

        List<Double> points = new ArrayList<>();

        for(int i = 0; i != xNodes.getLength(); ++i) {
            points.add(Double.parseDouble(xNodes.item(i).getTextContent()));
            points.add(Double.parseDouble(yNodes.item(i).getTextContent()));
        }

        return points;
    }

    private double parseDoubleElement(Document document, String tagName) throws InvalidAnnotationFileFormatException {
        Node doubleNode = document.getElementsByTagName(tagName).item(0);

        if(doubleNode == null) {
            throw new InvalidAnnotationFileFormatException(MISSING_ELEMENT_PREFIX + tagName);
        }

        return Double.parseDouble(doubleNode.getTextContent());
    }

    private double parseDoubleElement(Element element, String tagName) {
        Node doubleNode = element.getElementsByTagName(tagName).item(0);

        if(doubleNode == null) {
            throw new InvalidAnnotationFileFormatException(MISSING_ELEMENT_PREFIX + tagName);
        }

        return Double.parseDouble(doubleNode.getTextContent());
    }

    private int parseIntElement(Document document, String tagName) {
        Node intNode = document.getElementsByTagName(tagName).item(0);

        if(intNode == null) {
            throw new InvalidAnnotationFileFormatException(MISSING_ELEMENT_PREFIX + tagName);
        }

        return Integer.parseInt(intNode.getTextContent());
    }

    private static class BoundingShapeDataParseResult {
        private String categoryName;
        private Double xMin;
        private Double xMax;
        private Double yMin;
        private Double yMax;
        private List<String> tags = new ArrayList<>();
        private List<BoundingShapeData> parts = new ArrayList<>();
        private List<Double> points = null;
        private boolean isBoundingBox = false;
        private boolean isBoundingPolygon = false;


        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public Double getxMin() {
            return xMin;
        }

        public void setxMin(Double xMin) {
            this.xMin = xMin;
        }

        public Double getxMax() {
            return xMax;
        }

        public void setxMax(Double xMax) {
            this.xMax = xMax;
        }

        public Double getyMin() {
            return yMin;
        }

        public void setyMin(Double yMin) {
            this.yMin = yMin;
        }

        public Double getyMax() {
            return yMax;
        }

        public void setyMax(Double yMax) {
            this.yMax = yMax;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public List<BoundingShapeData> getParts() {
            return parts;
        }

        public void setParts(List<BoundingShapeData> parts) {
            this.parts = parts;
        }

        public List<Double> getPoints() {
            return points;
        }

        public void setPoints(List<Double> points) {
            this.points = points;
        }

        public boolean isBoundingBox() {
            return isBoundingBox;
        }

        public void setBoundingBox(boolean boundingBox) {
            isBoundingBox = boundingBox;
        }

        public boolean isBoundingPolygon() {
            return isBoundingPolygon;
        }

        public void setBoundingPolygon(boolean boundingPolygon) {
            isBoundingPolygon = boundingPolygon;
        }
    }

    @SuppressWarnings("serial")
    private static class AnnotationToNonExistentImageException extends RuntimeException {
        AnnotationToNonExistentImageException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    private static class InvalidAnnotationFileFormatException extends RuntimeException {
        InvalidAnnotationFileFormatException(String message) {
            super(message);
        }
    }
}
