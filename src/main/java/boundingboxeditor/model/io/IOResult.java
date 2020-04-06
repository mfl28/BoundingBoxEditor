package boundingboxeditor.model.io;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.List;
import java.util.Objects;

/**
 * Responsible for holding information about a finished IO-operation (e.g. loading or
 * saving of image-annotations).
 */
public class IOResult {
    private final int nrSuccessfullyProcessedItems;
    private final List<ErrorInfoEntry> errorTableEntries;
    private final OperationType operationType;
    private long timeTakenInMilliseconds = 0;

    /**
     * Creates a new io-operation result.
     *
     * @param operationType                specifies the result's operation-type
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link ErrorInfoEntry} that contain information
     *                                     about where and which errors occurred during the operation.
     */
    public IOResult(OperationType operationType, int nrSuccessfullyProcessedItems,
                    List<ErrorInfoEntry> errorTableEntries) {
        this.operationType = operationType;
        this.nrSuccessfullyProcessedItems = nrSuccessfullyProcessedItems;
        this.errorTableEntries = errorTableEntries;
    }

    /**
     * Returns the type of operation the result belongs to.
     *
     * @return the operation type
     */
    public OperationType getOperationType() {
        return operationType;
    }

    /**
     * Returns the number of successfully processed items during the io-operation.
     *
     * @return the number of successfully processed items
     */
    public int getNrSuccessfullyProcessedItems() {
        return nrSuccessfullyProcessedItems;
    }

    /**
     * Returns the time (in milliseconds) that the operation took to complete.
     *
     * @return the duration in milliseconds
     */
    public long getTimeTakenInMilliseconds() {
        return timeTakenInMilliseconds;
    }

    /**
     * Sets the time (in milliseconds) that the operation took to complete.
     *
     * @param timeTakenInMilliseconds the duration in milliseconds
     */
    public void setTimeTakenInMilliseconds(long timeTakenInMilliseconds) {
        this.timeTakenInMilliseconds = timeTakenInMilliseconds;
    }

    /**
     * Returns a list of objects containing information of where and which errors occurred during
     * the operation.
     *
     * @return the list of error-entries
     */
    public List<ErrorInfoEntry> getErrorTableEntries() {
        return errorTableEntries;
    }

    public enum OperationType {ANNOTATION_IMPORT, ANNOTATION_SAVING}

    /**
     * Holds information about an error that occurred during an io-operation.
     */
    public static class ErrorInfoEntry {
        private final StringProperty fileName;
        private final StringProperty errorDescription;

        /**
         * Creates a new error-information entry.
         *
         * @param fileName         the filename of the file in the processing of which the error occurred
         * @param errorDescription a description of the error that occurred
         */
        public ErrorInfoEntry(String fileName, String errorDescription) {
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

            if(!(o instanceof ErrorInfoEntry)) {
                return false;
            }

            ErrorInfoEntry that = (ErrorInfoEntry) o;

            return Objects.equals(fileName.get(), that.fileName.get()) &&
                    Objects.equals(errorDescription.get(), that.errorDescription.get());
        }
    }
}
