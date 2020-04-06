package boundingboxeditor.model.io;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

class IOOperationTimer {
    public static IOResult time(Callable<IOResult> callable) throws Exception {
        long startTime = System.nanoTime();
        IOResult result = callable.call();
        long duration = System.nanoTime() - startTime;

        result.setTimeTakenInMilliseconds(TimeUnit.MILLISECONDS.convert(Duration.ofNanos(duration)));

        return result;
    }
}
