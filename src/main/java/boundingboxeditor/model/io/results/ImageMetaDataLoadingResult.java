package boundingboxeditor.model.io.results;

import boundingboxeditor.model.data.ImageMetaData;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ImageMetaDataLoadingResult extends IOResult {
    private final List<File> validFiles;
    private final Map<String, ImageMetaData> fileNameToMetaDataMap;

    /**
     * Creates a new io-operation result.
     *
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     * @param validFiles                   the files that could be parsed
     * @param fileNameToMetaDataMap        maps filenames to parsed meta data
     */
    public ImageMetaDataLoadingResult(int nrSuccessfullyProcessedItems,
                                      List<IOErrorInfoEntry> errorTableEntries,
                                      List<File> validFiles,
                                      Map<String, ImageMetaData> fileNameToMetaDataMap) {
        super(OperationType.IMAGE_METADATA_LOADING, nrSuccessfullyProcessedItems, errorTableEntries);
        this.validFiles = validFiles;
        this.fileNameToMetaDataMap = fileNameToMetaDataMap;
    }

    public List<File> getValidFiles() {
        return validFiles;
    }

    public Map<String, ImageMetaData> getFileNameToMetaDataMap() {
        return fileNameToMetaDataMap;
    }
}
