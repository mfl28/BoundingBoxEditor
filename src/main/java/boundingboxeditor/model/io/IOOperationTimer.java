package boundingboxeditor.model.io;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Times a {@link Callable<IOResult>}'s call method and sets the time in the
 * returned {@link IOResult} object.
 */
public class IOOperationTimer {
    private IOOperationTimer() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Times the callable and sets the time in the
     * returned {@link IOResult} object.
     *
     * @param callable the callable whose call() method should be timed
     * @return an {@link IOResult} object with the set time
     * @throws Exception any that might be thrown by the callable's call() method
     */
    public static <T extends IOResult> T time(Callable<T> callable) throws Exception {
        long startTime = System.nanoTime();
        T result = callable.call();
        long duration = System.nanoTime() - startTime;

        result.setTimeTakenInMilliseconds(TimeUnit.MILLISECONDS.convert(Duration.ofNanos(duration)));

        return result;
    }
}
