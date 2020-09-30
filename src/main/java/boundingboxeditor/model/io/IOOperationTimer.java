/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package boundingboxeditor.model.io;

import boundingboxeditor.model.io.results.IOResult;

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
