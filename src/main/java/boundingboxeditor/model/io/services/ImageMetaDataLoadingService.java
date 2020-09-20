package boundingboxeditor.model.io.services;

import boundingboxeditor.model.data.ImageMetaData;
import boundingboxeditor.model.io.IOOperationTimer;
import boundingboxeditor.model.io.results.IOErrorInfoEntry;
import boundingboxeditor.model.io.results.ImageMetaDataLoadingResult;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ImageMetaDataLoadingService extends Service<ImageMetaDataLoadingResult> {
    private final ObjectProperty<File> source = new SimpleObjectProperty<>(this, "source");
    private final ObjectProperty<List<File>> imageFiles = new SimpleObjectProperty<>(this, "imageFiles");
    private final BooleanProperty reload = new SimpleBooleanProperty(this, "reload");

    public File getSource() {
        return source.get();
    }

    public void setSource(File source) {
        this.source.set(source);
    }

    public List<File> getImageFiles() {
        return imageFiles.get();
    }

    public void setImageFiles(List<File> imageFiles) {
        this.imageFiles.set(imageFiles);
    }

    public boolean isReload() {
        return reload.get();
    }

    public void setReload(boolean reload) {
        this.reload.set(reload);
    }

    @Override
    protected Task<ImageMetaDataLoadingResult> createTask() {
        return new Task<>() {
            @Override
            protected ImageMetaDataLoadingResult call() throws Exception {
                return IOOperationTimer.time(() -> {
                    final Map<String, ImageMetaData> fileNameToMetaDataMap = new HashMap<>();

                    final List<IOErrorInfoEntry> errorInfoEntries =
                            Collections.synchronizedList(new ArrayList<>());

                    int totalNrOfFiles = imageFiles.get().size();
                    final AtomicInteger nrProcessedFiles = new AtomicInteger(0);

                    fileNameToMetaDataMap
                            .putAll(imageFiles.get().parallelStream().collect(HashMap::new, (map, item) -> {
                                updateProgress(1.0 * nrProcessedFiles.incrementAndGet() / totalNrOfFiles, 1.0);
                                try {
                                    map.put(item.getName(), ImageMetaData.fromFile(item));
                                } catch(Exception e) {
                                    errorInfoEntries.add(new IOErrorInfoEntry(item.getName(), e.getMessage()));
                                }
                            }, Map::putAll));

                    final List<File> validImageFiles =
                            imageFiles.get().stream().filter(item -> fileNameToMetaDataMap.containsKey(item.getName()))
                                      .collect(Collectors.toList());

                    return new ImageMetaDataLoadingResult(fileNameToMetaDataMap.size(), errorInfoEntries,
                                                          validImageFiles,
                                                          fileNameToMetaDataMap);
                });
            }
        };
    }
}
