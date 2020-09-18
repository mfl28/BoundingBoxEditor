package boundingboxeditor.model.io;

import java.util.List;

/**
 * Responsible for holding information about a finished IO-operation (e.g. loading or
 * saving of image-annotations).
 */
public abstract class IOResult {
    private final int nrSuccessfullyProcessedItems;
    private final List<IOErrorInfoEntry> errorTableEntries;
    private final OperationType operationType;
    private long timeTakenInMilliseconds = 0;

    /**
     * Creates a new io-operation result.
     *
     * @param operationType                specifies the result's operation-type
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     *                                     about where and which errors occurred during the operation.
     */
    protected IOResult(OperationType operationType, int nrSuccessfullyProcessedItems,
                       List<IOErrorInfoEntry> errorTableEntries) {
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
    public List<IOErrorInfoEntry> getErrorTableEntries() {
        return errorTableEntries;
    }

    public enum OperationType {ANNOTATION_IMPORT, ANNOTATION_SAVING, IMAGE_METADATA_LOADING}

}
