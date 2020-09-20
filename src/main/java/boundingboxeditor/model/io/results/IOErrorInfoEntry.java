package boundingboxeditor.model.io.results;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * Holds information about an error that occurred during an io-operation.
 */
public class IOErrorInfoEntry {
    private final StringProperty fileName;
    private final StringProperty errorDescription;

    /**
     * Creates a new error-information entry.
     *
     * @param fileName         the filename of the file in the processing of which the error occurred
     * @param errorDescription a description of the error that occurred
     */
    public IOErrorInfoEntry(String fileName, String errorDescription) {
        this.fileName = new SimpleStringProperty(fileName);
        this.errorDescription = new SimpleStringProperty(errorDescription);
    }

    /**
     * Returns the filename of the file in the processing of which the error occurred.
     *
     * @return the filename
     */
    public String getFileName() {
        return fileName.get();
    }

    /**
     * Set the filename of the file in the processing of which the error occurred.
     *
     * @param fileName the filename to set
     */
    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    /**
     * Returns the filename-property of the file in the processing of which the error occurred.
     *
     * @return filename-property
     */
    public StringProperty fileNameProperty() {
        return fileName;
    }

    /**
     * Returns the description of the error that occurred.
     *
     * @return the error-description
     */
    public String getErrorDescription() {
        return errorDescription.get();
    }

    /**
     * Sets a description of the error that occurred.
     *
     * @param errorDescription the error-description
     */
    public void setErrorDescription(String errorDescription) {
        this.errorDescription.set(errorDescription);
    }

    /**
     * Returns the description-property of the error that occurred.
     *
     * @return the error-description-property
     */
    public StringProperty errorDescriptionProperty() {
        return errorDescription;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName.get(), errorDescription.get());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(!(o instanceof IOErrorInfoEntry)) {
            return false;
        }

        IOErrorInfoEntry that = (IOErrorInfoEntry) o;

        return Objects.equals(fileName.get(), that.fileName.get()) &&
                Objects.equals(errorDescription.get(), that.errorDescription.get());
    }
}
