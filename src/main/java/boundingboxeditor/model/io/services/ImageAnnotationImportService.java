package boundingboxeditor.model.io.services;

import boundingboxeditor.model.data.ObjectCategory;
import boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import boundingboxeditor.model.io.ImageAnnotationLoader;
import boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

public class ImageAnnotationImportService extends Service<ImageAnnotationImportResult> {
    private final ObjectProperty<File> source = new SimpleObjectProperty<>(this, "source");
    private final ObjectProperty<ImageAnnotationLoadStrategy.Type> importFormat =
            new SimpleObjectProperty<>(this, "importFormat");
    private final ObjectProperty<Set<String>> importableFileNames = new SimpleObjectProperty<>(this,
                                                                                               "importableFileNames");
    private final ObjectProperty<Map<String, ObjectCategory>> categoryNameToCategoryMap =
            new SimpleObjectProperty<>(this, "categoryNameToCategoryMap");

    public Set<String> getImportableFileNames() {
        return importableFileNames.get();
    }

    public void setImportableFileNames(Set<String> importableFileNames) {
        this.importableFileNames.set(importableFileNames);
    }

    public ObjectProperty<Set<String>> importableFileNamesProperty() {
        return importableFileNames;
    }

    public Map<String, ObjectCategory> getCategoryNameToCategoryMap() {
        return categoryNameToCategoryMap.get();
    }

    public void setCategoryNameToCategoryMap(Map<String, ObjectCategory> categoryNameToCategoryMap) {
        this.categoryNameToCategoryMap.set(categoryNameToCategoryMap);
    }

    public File getSource() {
        return source.get();
    }

    public void setSource(File source) {
        this.source.set(source);
    }

    public ImageAnnotationLoadStrategy.Type getImportFormat() {
        return importFormat.get();
    }

    public void setImportFormat(ImageAnnotationLoadStrategy.Type importFormat) {
        this.importFormat.set(importFormat);
    }

    @Override
    protected Task<ImageAnnotationImportResult> createTask() {
        return new Task<>() {
            @Override
            protected ImageAnnotationImportResult call() throws Exception {
                ImageAnnotationLoader loader = new ImageAnnotationLoader(importFormat.get());
                loader.progressProperty()
                      .addListener((observable, oldValue, newValue) -> updateProgress(newValue.doubleValue(), 1.0));
                return loader.load(Paths.get(source.get().getPath()), importableFileNames.get(),
                                   categoryNameToCategoryMap.get());
            }
        };
    }
}
