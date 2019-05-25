package BoundingboxEditor.model.io;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.List;

public class IOResult {
    private int nrSuccessfullyProcessedAnnotations;
    private long timeTakenInMilliseconds;
    private List<ErrorTableEntry> errors;

    public IOResult(int nrSuccessfullyProcessedAnnotations, long timeTakenInMilliseconds, List<ErrorTableEntry> errors) {
        this.nrSuccessfullyProcessedAnnotations = nrSuccessfullyProcessedAnnotations;
        this.timeTakenInMilliseconds = timeTakenInMilliseconds;
        this.errors = errors;
    }

    public int getNrSuccessfullyProcessedAnnotations() {
        return nrSuccessfullyProcessedAnnotations;
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
