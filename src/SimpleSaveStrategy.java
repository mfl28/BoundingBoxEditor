import jdk.jshell.spi.ExecutionControl;

import java.nio.file.Path;

public class SimpleSaveStrategy implements BoundingBoxSaveStrategy {
    @Override
    public void save(Path savePath) throws Exception {
        throw new ExecutionControl.NotImplementedException("Not implemented");
    }
}
