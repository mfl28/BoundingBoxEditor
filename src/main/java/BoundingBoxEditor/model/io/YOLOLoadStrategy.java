package boundingboxeditor.model.io;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.Model;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.utils.ColorUtils;
import javafx.beans.property.DoubleProperty;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YOLOLoadStrategy implements ImageAnnotationLoadStrategy {
    private static final boolean INCLUDE_SUBDIRECTORIES = false;
    private static final String OBJECT_DATA_FILE_NAME = "object.data";
    private Set<String> fileNamesToLoad;
    private Map<String, ObjectCategory> nameToObjectCategoryMap;
    private Map<String, Integer> boundingShapeCountPerCategory;
    private List<IOResult.ErrorInfoEntry> unParsedFileErrorMessages;
    private final List<String> categories = new ArrayList<>();
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
        } catch(IOException e) {
            unParsedFileErrorMessages.add(new IOResult.ErrorInfoEntry(path.resolve(OBJECT_DATA_FILE_NAME).toString(), e.getMessage()));
        }

        try(Stream<Path> fileStream = Files.walk(path, INCLUDE_SUBDIRECTORIES ? Integer.MAX_VALUE : 1)) {
            List<File> annotationFiles = fileStream
                    .filter(pathItem -> pathItem.getFileName().toString().endsWith(".txt"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());

            int totalNrOfFiles = annotationFiles.size();
            AtomicInteger nrProcessedFiles = new AtomicInteger(0);

            List<ImageAnnotation> imageAnnotations = annotationFiles.stream()
                    .map(file -> {
                        progress.set(1.0 * nrProcessedFiles.incrementAndGet() / totalNrOfFiles);

                        try {
                            return loadAnnotationFromFile(file);
                        } catch(InvalidAnnotationFileFormatException | AnnotationToNonExistentImageException | IOException e) {
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
        String annotatedImageFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".jpg";

        if(!fileNamesToLoad.contains(annotatedImageFileName)) {
            throw new AnnotationToNonExistentImageException("The image file does not belong to the currently loaded images.");
        }

        try(BufferedReader fileReader = Files.newBufferedReader(file.toPath())) {
            String line;

            List<BoundingShapeData> boundingShapeDataList = new ArrayList<>();

            while((line = fileReader.readLine()) != null) {
                line = line.strip();

                if(!line.isBlank()) {
                    Scanner scanner = new Scanner(line);
                    scanner.useLocale(Locale.ENGLISH);

                    int categoryId = scanner.nextInt();
                    double xMidRelative = scanner.nextDouble();
                    double yMidRelative = scanner.nextDouble();
                    double widthRelative = scanner.nextDouble();
                    double heightRelative = scanner.nextDouble();

                    double xMinRelative = xMidRelative - widthRelative / 2;
                    double yMinRelative = yMidRelative - heightRelative / 2;
                    double xMaxRelative = xMidRelative + widthRelative / 2;
                    double yMaxRelative = yMidRelative + heightRelative / 2;

                    String categoryName = categories.get(categoryId);

                    ObjectCategory objectCategory = nameToObjectCategoryMap.computeIfAbsent(categoryName,
                            key -> new ObjectCategory(key, ColorUtils.createRandomColor()));

                    // Note that there are no tags or parts in YOLO-format.
                    BoundingBoxData boundingBoxData = new BoundingBoxData(objectCategory,
                            xMinRelative, yMinRelative, xMaxRelative, yMaxRelative, Collections.emptyList());

                    boundingShapeCountPerCategory.merge(categoryName, 1, Integer::sum);

                    boundingShapeDataList.add(boundingBoxData);
                }
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
}
