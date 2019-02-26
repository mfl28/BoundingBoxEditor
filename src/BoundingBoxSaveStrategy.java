import java.nio.file.Path;

public interface BoundingBoxSaveStrategy {
    void save(Path savePath) throws Exception;
}
