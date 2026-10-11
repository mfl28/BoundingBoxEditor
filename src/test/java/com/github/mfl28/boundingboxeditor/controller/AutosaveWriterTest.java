/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.controller;

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class AutosaveWriterTest {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ObjectCategory category = new ObjectCategory("cat", Color.RED);

    @AfterEach
    void shutDown() {
        executor.shutdownNow();
    }

    @Test
    void onSavingWhileEarlierSavesWait_ShouldOnlySaveTheNewestAnnotations(@TempDir Path tempDir) throws Exception {
        final AutosaveStore store = new AutosaveStore(tempDir);
        final AtomicInteger saveCount = new AtomicInteger();
        final AutosaveWriter writer = new AutosaveWriter(store, executor, saveCount::incrementAndGet,
                                                         e -> fail(e));
        final File folder = tempDir.resolve("images").toFile();

        // Keeps the writer thread busy, like a slow save.
        final CountDownLatch busy = new CountDownLatch(1);
        executor.execute(() -> awaitQuietly(busy));

        writer.save(folder, annotationsWithBoxes(1));
        writer.save(folder, annotationsWithBoxes(2));
        writer.save(folder, annotationsWithBoxes(3));
        busy.countDown();
        writer.awaitCompletion();

        assertEquals(1, saveCount.get());
        assertEquals(3, loadBoxCount(store, folder));

        // Once that save is done, the next one is written again.
        writer.save(folder, annotationsWithBoxes(4));
        writer.awaitCompletion();

        assertEquals(2, saveCount.get());
        assertEquals(4, loadBoxCount(store, folder));

        // A deletion comes after the saves that were requested before it.
        writer.save(folder, annotationsWithBoxes(5));
        writer.delete(folder);

        assertTrue(store.lastSaved(folder).isEmpty());
    }

    @Test
    void onShuttingDown_ShouldEndTheThreadAndStillDeleteButNotSave(@TempDir Path tempDir) throws Exception {
        final AutosaveStore store = new AutosaveStore(tempDir);
        final AtomicInteger saveCount = new AtomicInteger();
        final AutosaveWriter writer = new AutosaveWriter(store, executor, saveCount::incrementAndGet,
                                                         e -> fail(e));
        final File folder = tempDir.resolve("images").toFile();
        writer.save(folder, annotationsWithBoxes(1));

        writer.shutdown();

        // The save that was requested before is written.
        assertEquals(1, saveCount.get());
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        writer.save(folder, annotationsWithBoxes(2));
        writer.awaitCompletion();
        assertEquals(1, saveCount.get());

        writer.delete(folder);
        assertTrue(store.lastSaved(folder).isEmpty());
    }

    @Test
    void onFailingToSave_ShouldReportTheError(@TempDir Path tempDir) throws Exception {
        // The store's folder can't be created, because a file is in its place.
        final Path blocked = tempDir.resolve("blocked");
        java.nio.file.Files.createFile(blocked);
        final List<IOException> errors = Collections.synchronizedList(new ArrayList<>());
        final AutosaveWriter writer = new AutosaveWriter(new AutosaveStore(blocked), executor,
                                                         () -> fail("Should not save"), errors::add);

        writer.save(tempDir.resolve("images").toFile(), annotationsWithBoxes(1));
        writer.awaitCompletion();

        assertEquals(1, errors.size());
    }

    private ImageAnnotationData annotationsWithBoxes(int count) {
        final List<BoundingShapeData> boxes = new ArrayList<>();

        for(int i = 0; i < count; i++) {
            boxes.add(new BoundingBoxData(category, 0.1, 0.1, 0.5, 0.5, List.of()));
        }

        final ImageMetaData image = new ImageMetaData("a.jpg", "images", "file:/images/a.jpg", 100, 50, 3);
        return new ImageAnnotationData(List.of(new ImageAnnotation(image, boxes)), Map.of(category.getName(), count),
                                       Map.of(category.getName(), category));
    }

    private static int loadBoxCount(AutosaveStore store, File folder) throws IOException {
        return ImageAnnotationLoadStrategy.createStrategy(ImageAnnotationLoadStrategy.Type.JSON)
                                          .load(store.fileFor(folder), Set.of("a.jpg"), new HashMap<>(),
                                                new SimpleDoubleProperty())
                                          .getImageAnnotationData().imageAnnotations().iterator().next()
                                          .getBoundingShapeData().size();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch(InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
