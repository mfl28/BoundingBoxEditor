package BoundingboxEditor.model.io;

import BoundingboxEditor.model.Model;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Base interface for all image loader implementations. An ImageAnnotationLoader
 * is responsible for loading existing image annotation data from disk.
 */
public class ImageAnnotationLoader {
    private final ImageAnnotationLoadStrategy loadStrategy;

    public ImageAnnotationLoader(final ImageAnnotationLoadStrategy.Type strategy) {
        loadStrategy = ImageAnnotationLoadStrategy.createStrategy(strategy);
    }


    public LoadResult load(final Model model, final Path saveFolderPath) throws IOException {
        return loadStrategy.load(model, saveFolderPath);
    }

    public static class LoadResult {
        int loadedImageAnnotations;
        long timeTakenInMilliseconds;
        List<ErrorTableEntry> errors;

        public LoadResult(int loadedImageAnnotations, long timeTakenInMilliseconds, List<ErrorTableEntry> errors) {
            this.loadedImageAnnotations = loadedImageAnnotations;
            this.timeTakenInMilliseconds = timeTakenInMilliseconds;
            this.errors = errors;
        }

        public int getNrLoadedImageAnnotations() {
            return loadedImageAnnotations;
        }

        public long getTimeTakenInMilliseconds() {
            return timeTakenInMilliseconds;
        }

        public List<ErrorTableEntry> getErrorEntries() {
            return errors;
        }

        public static class ErrorTableEntry {
            private StringProperty fileName;
            private StringProperty problemDescription;

            public ErrorTableEntry(String fileName, String problemDescription) {
                this.fileName = new SimpleStringProperty(fileName);
                this.problemDescription = new SimpleStringProperty(problemDescription);
            }

            public String getFileName() {
                return fileName.get();
            }

            public void setFileName(String fileName) {
                this.fileName.set(fileName);
            }

            public StringProperty fileNameProperty() {
                return fileName;
            }

            public String getProblemDescription() {
                return problemDescription.get();
            }

            public void setProblemDescription(String problemDescription) {
                this.problemDescription.set(problemDescription);
            }

            public StringProperty problemDescriptionProperty() {
                return problemDescription;
            }
        }
    }
}
