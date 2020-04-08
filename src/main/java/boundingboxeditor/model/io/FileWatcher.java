package boundingboxeditor.model.io;

import boundingboxeditor.controller.Controller;
import boundingboxeditor.ui.MainView;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

public class FileWatcher implements Runnable {
    private final Path directoryToWatch;
    private final Controller controller;
    private final Set<String> fileNamesToWatch;

    public FileWatcher(Path directoryToWatch, Controller controller, Set<String> fileNamesToWatch) {
        this.directoryToWatch = directoryToWatch;
        this.controller = controller;
        this.fileNamesToWatch = fileNamesToWatch;
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
                            MainView.displayErrorAlert("Image files changed", "Image files were changed externally, will reload folder.");
                            this.controller.reloadCurrentImageFolder();
                        });
                    }
                }
            }
        } catch(IOException | InterruptedException ignored) {

        }
    }
}
