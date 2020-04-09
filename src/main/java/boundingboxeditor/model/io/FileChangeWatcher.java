package boundingboxeditor.model.io;

import boundingboxeditor.ui.MainView;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

public class FileChangeWatcher implements Runnable {
    private static final String IMAGE_FILES_CHANGED_ERROR_TITLE = "Image files changed";
    private static final String IMAGE_FILES_CHANGED_ERROR_CONTENT = "Image files were changed externally, will reload folder.";
    private static final String IMAGE_FILE_CHANGE_WATCHER_ERROR_TITLE = "Image file-change watcher error";
    private static final String IMAGE_FILE_CHANGE_WATCHER_ERROR_CONTENT = "Image file-change watcher has encountered an IO error, will reload folder.";
    private final Path directoryToWatch;
    private final Set<String> fileNamesToWatch;
    private final Runnable onFilesChangedHandler;

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

            while(true) {
                WatchKey watchKey = watchService.take();

                if(watchKey != null) {
                    if(watchKey.pollEvents().stream().anyMatch(watchEvent -> fileNamesToWatch.contains(watchEvent.context().toString()))) {
                        Platform.runLater(() -> {
                            MainView.displayErrorAlert(IMAGE_FILES_CHANGED_ERROR_TITLE, IMAGE_FILES_CHANGED_ERROR_CONTENT);
                            onFilesChangedHandler.run();
                        });
                    } else {
                        watchKey.reset();
                    }
                }
            }
        } catch(InterruptedException ignored) {
        } catch(IOException e) {
            Platform.runLater(() -> {
                MainView.displayErrorAlert(IMAGE_FILE_CHANGE_WATCHER_ERROR_TITLE, IMAGE_FILE_CHANGE_WATCHER_ERROR_CONTENT);
                onFilesChangedHandler.run();
            });
        }
    }
}
