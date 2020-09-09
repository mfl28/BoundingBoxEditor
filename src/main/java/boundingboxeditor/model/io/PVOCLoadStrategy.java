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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private Map<String, ImageMetaData> imageMetaDataMap;

    @Override
    public IOResult load(Model model, Path path, DoubleProperty progress) throws IOException {
        this.fileNamesToLoad = model.getImageFileNameSet();
        this.boundingShapeCountPerCategory =
                new ConcurrentHashMap<>(model.getCategoryToAssignedBoundingShapesCountMap());
        this.existingObjectCategories = new ConcurrentHashMap<>(model.getObjectCategories().stream()
                                                                     .collect(Collectors.toMap(ObjectCategory::getName,
                                                                                               Function.identity())));
        this.imageMetaDataMap = model.getImageFileNameToMetaDataMap();

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
                                                                        progress.set(1.0 * nrProcessedFiles
                                                                                .incrementAndGet() / totalNrOfFiles);

                                                                        try {
                                                                            return parseAnnotationFile(file);
                                                                        } catch(SAXException | IOException | InvalidAnnotationFormatException
                                                                                | ParserConfigurationException | AnnotationToNonExistentImageException e) {
                                                                            unParsedFileErrorMessages
                                                                                    .add(new IOResult.ErrorInfoEntry(
                                                                                            file.getName(),
                                                                                            e.getMessage()));
                                                                            return null;
                                                                        }
                                                                    })
                                                                    .filter(Objects::nonNull)
                                                                    .collect(Collectors.toList());

            if(!imageAnnotations.isEmpty()) {
                model.getObjectCategories().setAll(existingObjectCategories.values());
                model.getCategoryToAssignedBoundingShapesCountMap().putAll(boundingShapeCountPerCategory);
                model.updateImageAnnotations(imageAnnotations);
            }

            return new IOResult(
                    IOResult.OperationType.ANNOTATION_IMPORT,
                    imageAnnotations.size(),
                    unParsedFileErrorMessages
            );
        }
    }

    private ImageAnnotation parseAnnotationFile(File file) throws SAXException, IOException,
                                                                  ParserConfigurationException {
        final Document document = documentBuilderFactory.newDocumentBuilder().parse(file);
        document.normalize();

        final ImageMetaData parsedImageMetaData = parseImageMetaData(document);

        if(!fileNamesToLoad.contains(parsedImageMetaData.getFileName())) {
            throw new AnnotationToNonExistentImageException(
                    "The image file does not belong to the currently loaded images.");
        }

        List<BoundingShapeData> boundingShapeData =
                parseBoundingShapeData(document, file.getName(), parsedImageMetaData);

        if(boundingShapeData.isEmpty()) {
            // No image annotation will be constructed if it does not contain any bounding boxes.
            return null;
        }

        ImageMetaData imageMetaData = imageMetaDataMap.getOrDefault(parsedImageMetaData.getFileName(),
                                                                    new ImageMetaData(
                                                                            parsedImageMetaData.getFileName()));

        return new ImageAnnotation(imageMetaData, boundingShapeData);
    }

    private ImageMetaData parseImageMetaData(Document document) {
        String folderName = parseTextElement(document, "folder");
        String fileName = parseTextElement(document, "filename");
        double width = parseDoubleElement(document, "width");
        double height = parseDoubleElement(document, "height");
        int depth = parseIntElement(document, "depth");

        return new ImageMetaData(fileName, folderName, width, height, depth);
    }

    private List<BoundingShapeData> parseBoundingShapeData(Document document, String filename,
                                                           ImageMetaData imageMetaData) {
        NodeList objectElements = document.getElementsByTagName("object");

        List<BoundingShapeData> boundingShapeDataList = new ArrayList<>();

        for(int i = 0; i != objectElements.getLength(); ++i) {
            Node objectNode = objectElements.item(i);

            if(objectNode.getNodeType() == Node.ELEMENT_NODE) {
                try {
                    BoundingShapeData boundingShapeData =
                            parseBoundingShapeElement((Element) objectNode, filename, imageMetaData);
                    boundingShapeDataList.add(boundingShapeData);
                } catch(InvalidAnnotationFormatException e) {
                    unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(filename, e.getMessage()));
                }
            }
        }

        return boundingShapeDataList;
    }

    private BoundingShapeData parseBoundingShapeElement(Element objectElement, String filename,
                                                        ImageMetaData imageMetaData) {
        NodeList childElements = objectElement.getChildNodes();

        BoundingShapeDataParseResult boxDataParseResult = new BoundingShapeDataParseResult();

        // At first, parse all child elements except parts. In this way if errors occur,
        // no parts will be parsed.
        parseNonPartElements(childElements, boxDataParseResult);

        if(boxDataParseResult.getCategoryName() == null) {
            throw new InvalidAnnotationFormatException(MISSING_ELEMENT_PREFIX + "name");
        }

        if(boxDataParseResult.isBoundingBox() && boxDataParseResult.isBoundingPolygon()) {
            throw new InvalidAnnotationFormatException(INVALID_OBJECT_ELEMENT_DUPL_ERROR);
        } else if(!boxDataParseResult.isBoundingBox() && !boxDataParseResult.isBoundingPolygon()) {
            throw new InvalidAnnotationFormatException(INVALID_OBJECT_ELEMENT_MISSING_ERROR);
        } else if(boxDataParseResult.isBoundingBox()) {
            if(boxDataParseResult.getMinX() == null || boxDataParseResult.getMaxX() == null
                    || boxDataParseResult.getMinY() == null || boxDataParseResult.getMaxY() == null) {
                throw new InvalidAnnotationFormatException(MISSING_ELEMENT_PREFIX + "bndbox");
            }
        } else {
            if(boxDataParseResult.getPoints() == null) {
                throw new InvalidAnnotationFormatException(INVALID_POLYGON_ELEMENT_ERROR);
            }
        }

        boxDataParseResult.validateCoordinates(imageMetaData);

        ObjectCategory category = existingObjectCategories.computeIfAbsent(boxDataParseResult.getCategoryName(),
                                                                           key -> new ObjectCategory(key, ColorUtils
                                                                                   .createRandomColor()));

        boundingShapeCountPerCategory.merge(category.getName(), 1, Integer::sum);

        BoundingShapeData boundingShapeData;

        if(boxDataParseResult.isBoundingBox()) {
            boundingShapeData = new BoundingBoxData(category,
                                                    boxDataParseResult.getMinX() / imageMetaData.getImageWidth(),
                                                    boxDataParseResult.getMinY() / imageMetaData.getImageHeight(),
                                                    boxDataParseResult.getMaxX() / imageMetaData.getImageWidth(),
                                                    boxDataParseResult.getMaxY() / imageMetaData.getImageHeight(),
                                                    boxDataParseResult.getTags());
        } else {
            boundingShapeData = new BoundingPolygonData(category,
                                                        BoundingPolygonData.absoluteToRelativePoints(
                                                                boxDataParseResult.getPoints(),
                                                                imageMetaData.getImageWidth(),
                                                                imageMetaData.getImageHeight()),
                                                        boxDataParseResult.getTags());
        }

        // Now parse parts.
        parsePartElements(childElements, boxDataParseResult, filename, imageMetaData);

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
                    throw new InvalidAnnotationFormatException("Blank object name");
                }

                boxDataParseResult.setCategoryName(categoryName);
                break;
            case "bndbox":
                boxDataParseResult.setBoundingBox(true);
                boxDataParseResult.setMinX(parseDoubleElement(tagElement, "xmin"));
                boxDataParseResult.setMaxX(parseDoubleElement(tagElement, "xmax"));
                boxDataParseResult.setMinY(parseDoubleElement(tagElement, "ymin"));
                boxDataParseResult.setMaxY(parseDoubleElement(tagElement, "ymax"));
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

    private void parsePart(Element tagElement, BoundingShapeDataParseResult boxDataParseResult,
                           String filename, ImageMetaData imageMetaData) {
        try {
            boxDataParseResult.getParts().add(parseBoundingShapeElement(tagElement, filename, imageMetaData));
        } catch(InvalidAnnotationFormatException e) {
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

    private void parsePartElements(NodeList childElements, BoundingShapeDataParseResult boxDataParseResult,
                                   String filename, ImageMetaData imageMetaData) {
        for(int i = 0; i != childElements.getLength(); ++i) {
            Node currentChild = childElements.item(i);

            if(currentChild.getNodeType() == Node.ELEMENT_NODE) {
                Element currentElement = (Element) currentChild;

                if(currentElement.getTagName().equals("part")) {
                    parsePart((Element) currentChild, boxDataParseResult, filename, imageMetaData);
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
            throw new InvalidAnnotationFormatException(MISSING_ELEMENT_PREFIX + tagName);
        }

        return textNode.getTextContent();
    }

    private List<Double> parsePointList(Element element) {
        NodeList xNodes = element.getElementsByTagName("x");
        NodeList yNodes = element.getElementsByTagName("y");

        if(xNodes.getLength() == 0 || yNodes.getLength() == 0 || xNodes.getLength() != yNodes.getLength()) {
            throw new InvalidAnnotationFormatException("Invalid polygon element.");
        }

        List<Double> points = new ArrayList<>();

        for(int i = 0; i != xNodes.getLength(); ++i) {
            points.add(Double.parseDouble(xNodes.item(i).getTextContent()));
            points.add(Double.parseDouble(yNodes.item(i).getTextContent()));
        }

        return points;
    }

    private double parseDoubleElement(Document document, String tagName) {
        Node doubleNode = document.getElementsByTagName(tagName).item(0);

        if(doubleNode == null) {
            throw new InvalidAnnotationFormatException(MISSING_ELEMENT_PREFIX + tagName);
        }

        return Double.parseDouble(doubleNode.getTextContent());
    }

    private double parseDoubleElement(Element element, String tagName) {
        Node doubleNode = element.getElementsByTagName(tagName).item(0);

        if(doubleNode == null) {
            throw new InvalidAnnotationFormatException(MISSING_ELEMENT_PREFIX + tagName);
        }

        return Double.parseDouble(doubleNode.getTextContent());
    }

    private int parseIntElement(Document document, String tagName) {
        Node intNode = document.getElementsByTagName(tagName).item(0);

        if(intNode == null) {
            throw new InvalidAnnotationFormatException(MISSING_ELEMENT_PREFIX + tagName);
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

        public Double getMinX() {
            return xMin;
        }

        public void setMinX(Double xMin) {
            this.xMin = xMin;
        }

        public Double getMaxX() {
            return xMax;
        }

        public void setMaxX(Double xMax) {
            this.xMax = xMax;
        }

        public Double getMinY() {
            return yMin;
        }

        public void setMinY(Double yMin) {
            this.yMin = yMin;
        }

        public Double getMaxY() {
            return yMax;
        }

        public void setMaxY(Double yMax) {
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

        void validateCoordinates(ImageMetaData metaData) {
            if(isBoundingBox) {
                if((xMin < 0 || xMin > metaData.getImageWidth()) || (xMax < 0 || xMax > metaData.getImageWidth())
                        || (yMin < 0 || yMin > metaData.getImageHeight()) ||
                        (yMax < 0 || yMax > metaData.getImageHeight())) {
                    throw new InvalidAnnotationFormatException("Invalid bounding-box bounds for the given image size.");
                }
            } else if(isBoundingPolygon) {
                for(int i = 0; i < points.size(); i += 2) {
                    if((points.get(i) < 0 || points.get(i) > metaData.getImageWidth())
                            || (points.get(i + 1) < 0 || points.get(i + 1) > metaData.getImageHeight())) {
                        throw new InvalidAnnotationFormatException(
                                "Invalid bounding-polygon point coordinates for the given image size.");
                    }
                }
            }
        }
    }
}
