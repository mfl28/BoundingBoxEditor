package boundingboxeditor.model.io;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.Model;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.utils.ColorUtils;
import javafx.beans.property.DoubleProperty;

import java.io.BufferedReader;
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
 * Loads rectangular bounding-box annotations in the YOLO-format described at
 * https://github.com/AlexeyAB/Yolo_mark/issues/60#issuecomment-401854885
 */
public class YOLOLoadStrategy implements ImageAnnotationLoadStrategy {
    public static final String INVALID_BOUNDING_BOX_COORDINATES_MESSAGE = "Invalid bounding-box coordinates on line ";
    private static final boolean INCLUDE_SUBDIRECTORIES = false;
    private static final String OBJECT_DATA_FILE_NAME = "object.data";
    private static final String YOLO_IMAGE_EXTENSION = ".jpg";
    private final List<String> categories = new ArrayList<>();
    private final List<IOResult.ErrorInfoEntry> unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());
    private Set<String> fileNamesToLoad;
    private Map<String, ObjectCategory> nameToObjectCategoryMap;
    private Map<String, Integer> boundingShapeCountPerCategory;
    private Map<String, ImageMetaData> imageMetaDataMap;

    @Override
    public IOResult load(Model model, Path path, DoubleProperty progress) throws IOException {
        this.fileNamesToLoad = model.getImageFileNameSet();
        this.boundingShapeCountPerCategory = new ConcurrentHashMap<>(model.getCategoryToAssignedBoundingShapesCountMap());
        this.nameToObjectCategoryMap = new ConcurrentHashMap<>(model.getObjectCategories().stream()
                .collect(Collectors.toMap(ObjectCategory::getName, Function.identity())));
        this.imageMetaDataMap = model.getImageFileNameToMetaDataMap();

        try {
            loadObjectCategories(path);
        } catch(Exception e) {
            unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(OBJECT_DATA_FILE_NAME, e.getMessage()));
            return new IOResult(IOResult.OperationType.ANNOTATION_IMPORT, 0, unParsedFileErrorMessages);
        }

        if(categories.isEmpty()) {
            unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(OBJECT_DATA_FILE_NAME, "Does not contain any category names."));
            return new IOResult(IOResult.OperationType.ANNOTATION_IMPORT, 0, unParsedFileErrorMessages);
        }

        try(Stream<Path> fileStream = Files.walk(path, INCLUDE_SUBDIRECTORIES ? Integer.MAX_VALUE : 1)) {
            List<File> annotationFiles = fileStream
                    .filter(pathItem -> pathItem.getFileName().toString().endsWith(".txt"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            int totalNrOfFiles = annotationFiles.size();
            AtomicInteger nrProcessedFiles = new AtomicInteger(0);

            List<ImageAnnotation> imageAnnotations = annotationFiles.parallelStream()
                    .map(file -> {
                        progress.set(1.0 * nrProcessedFiles.incrementAndGet() / totalNrOfFiles);

                        try {
                            return loadAnnotationFromFile(file);
                        } catch(InvalidAnnotationFormatException | AnnotationToNonExistentImageException | IOException e) {
                            unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(file.getName(), e.getMessage()));
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            model.getObjectCategories().setAll(nameToObjectCategoryMap.values());
            model.getCategoryToAssignedBoundingShapesCountMap().putAll(boundingShapeCountPerCategory);
            model.updateImageAnnotations(imageAnnotations);

            return new IOResult(
                    IOResult.OperationType.ANNOTATION_IMPORT,
                    imageAnnotations.size(),
                    unParsedFileErrorMessages
            );
        }
    }

    private void loadObjectCategories(Path root) throws IOException {
        if(!root.resolve(OBJECT_DATA_FILE_NAME).toFile().exists()) {
            throw new InvalidAnnotationFormatException("Does not exist in annotation folder \"" + root.getFileName().toString() + "\".");
        }

        try(BufferedReader fileReader = Files.newBufferedReader(root.resolve(OBJECT_DATA_FILE_NAME))) {
            String line;

            while((line = fileReader.readLine()) != null) {
                line = line.strip();

                if(!line.isBlank()) {
                    categories.add(line);
                }
            }
        }
    }

    private ImageAnnotation loadAnnotationFromFile(File file) throws IOException {
        String annotatedImageFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + YOLO_IMAGE_EXTENSION;

        if(!fileNamesToLoad.contains(annotatedImageFileName)) {
            throw new AnnotationToNonExistentImageException("The image file \"" + annotatedImageFileName + "\" does not belong to the currently loaded images.");
        }

        try(BufferedReader fileReader = Files.newBufferedReader(file.toPath())) {
            String line;

            List<BoundingShapeData> boundingShapeDataList = new ArrayList<>();

            int counter = 1;

            while((line = fileReader.readLine()) != null) {
                line = line.strip();

                if(!line.isBlank()) {
                    try {
                        boundingShapeDataList.add(parseBoundingBoxData(line, counter));
                    } catch(InvalidAnnotationFormatException e) {
                        unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(file.getName(), e.getMessage()));
                    }
                }

                ++counter;
            }

            if(boundingShapeDataList.isEmpty()) {
                return null;
            }

            ImageMetaData imageMetaData = imageMetaDataMap.getOrDefault(annotatedImageFileName,
                    new ImageMetaData(annotatedImageFileName));

            // ImageMetaData will be loaded when the corresponding image is displayed for the first time.
            return new ImageAnnotation(imageMetaData, boundingShapeDataList);
        }
    }

    private BoundingBoxData parseBoundingBoxData(String line, int lineNumber) {
        Scanner scanner = new Scanner(line);
        scanner.useLocale(Locale.ENGLISH);

        int categoryId = parseCategoryIndex(scanner, lineNumber);

        double xMidRelative = parseRatio(scanner, lineNumber);
        double yMidRelative = parseRatio(scanner, lineNumber);
        double widthRelative = parseRatio(scanner, lineNumber);
        double heightRelative = parseRatio(scanner, lineNumber);

        double xMinRelative = xMidRelative - widthRelative / 2;
        assertRatio(xMinRelative, INVALID_BOUNDING_BOX_COORDINATES_MESSAGE + lineNumber + ".");

        double yMinRelative = yMidRelative - heightRelative / 2;
        assertRatio(yMinRelative, INVALID_BOUNDING_BOX_COORDINATES_MESSAGE + lineNumber + ".");

        double xMaxRelative = xMidRelative + widthRelative / 2;
        assertRatio(xMaxRelative, INVALID_BOUNDING_BOX_COORDINATES_MESSAGE + lineNumber + ".");

        double yMaxRelative = yMidRelative + heightRelative / 2;
        assertRatio(yMaxRelative, INVALID_BOUNDING_BOX_COORDINATES_MESSAGE + lineNumber + ".");

        String categoryName = categories.get(categoryId);

        ObjectCategory objectCategory = nameToObjectCategoryMap.computeIfAbsent(categoryName,
                key -> new ObjectCategory(key, ColorUtils.createRandomColor()));

        // Note that there are no tags or parts in YOLO-format.
        BoundingBoxData boundingBoxData = new BoundingBoxData(objectCategory,
                xMinRelative, yMinRelative, xMaxRelative, yMaxRelative, Collections.emptyList());

        boundingShapeCountPerCategory.merge(categoryName, 1, Integer::sum);

        return boundingBoxData;
    }

    private double parseRatio(Scanner scanner, int lineNumber) {
        if(!scanner.hasNextDouble()) {
            throw new InvalidAnnotationFormatException("Missing or invalid bounding-box bounds on line " + lineNumber + ".");
        }

        double ratio = scanner.nextDouble();

        assertRatio(ratio, lineNumber);

        return ratio;
    }

    private int parseCategoryIndex(Scanner scanner, int lineNumber) {
        if(!scanner.hasNextInt()) {
            throw new InvalidAnnotationFormatException("Missing or invalid category index on line " + lineNumber + ".");
        }

        int categoryId = scanner.nextInt();

        if(categoryId < 0 || categoryId >= categories.size()) {
            throw new InvalidAnnotationFormatException("Invalid category index " + categoryId
                    + " (of " + categories.size() + " categories) on line " + lineNumber + ".");
        }

        return categoryId;
    }

    private void assertRatio(double ratio, int lineNumber) {
        if(ratio < 0 || ratio > 1) {
            throw new InvalidAnnotationFormatException("Bounds ratio not within [0, 1] on line " + lineNumber + ".");
        }
    }

    private void assertRatio(double ratio, String message) {
        if(ratio < 0 || ratio > 1) {
            throw new InvalidAnnotationFormatException(message);
        }
    }
}
