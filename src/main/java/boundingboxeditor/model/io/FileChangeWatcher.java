package boundingboxeditor.model.io;

import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

/**
 * Used to check if (image) files of the currently loaded folder were modified/removed and initiate folder reload when necessary.
 */
public class FileChangeWatcher implements Runnable {
    private final Path directoryToWatch;
    private final Set<String> fileNamesToWatch;
    private final Runnable onFilesChangedHandler;

    /**
     * Creates a new file change watcher
     *
     * @param directoryToWatch      the path to the directory that should be watched
     * @param fileNamesToWatch      the set of names of files to watch
     * @param onFilesChangedHandler what should be done when files were modified/removed
     */
    public FileChangeWatcher(Path directoryToWatch, Set<String> fileNamesToWatch, Runnable onFilesChangedHandler) {
        this.directoryToWatch = directoryToWatch;
        this.fileNamesToWatch = fileNamesToWatch;
        this.onFilesChangedHandler = onFilesChangedHandler;
    }

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            directoryToWatch.register(watchService, StandardWatchEventKinds.ENTRY_DELETE,
                                      StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey key;
            while((key = watchService.take()) != null) {
                if(key.pollEvents().stream()
                      .anyMatch(watchEvent -> fileNamesToWatch.contains(watchEvent.context().toString()))) {
                    Platform.runLater(onFilesChangedHandler);
                } else {
                    key.reset();
                }
            }
        } catch(InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch(IOException e) {
            Platform.runLater(onFilesChangedHandler);
        }
    }
}
