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
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class AutosaveStoreTest {
    @Test
    void onChoosingTheDefaultFolder_ShouldUseThePlatformsApplicationDataFolder() {
        assertEquals(Path.of("/Users/ann", "Library", "Application Support", "BoundingBoxEditor", "autosave"),
                     AutosaveStore.defaultDirectory("Mac OS X", "/Users/ann", null, null));
        assertEquals(Path.of("C:\\Users\\ann\\AppData\\Roaming", "BoundingBoxEditor", "autosave"),
                     AutosaveStore.defaultDirectory("Windows 11", "C:\\Users\\ann", "C:\\Users\\ann\\AppData\\Roaming",
                                                    null));
        assertEquals(Path.of("/home/ann", ".local", "share", "boundingboxeditor", "autosave"),
                     AutosaveStore.defaultDirectory("Linux", "/home/ann", null, null));
        assertEquals(Path.of("/data", "boundingboxeditor", "autosave"),
                     AutosaveStore.defaultDirectory("Linux", "/home/ann", null, "/data"));
        // Unset or empty variables fall back to the usual folders.
        assertEquals(Path.of("C:\\Users\\ann", "AppData", "Roaming", "BoundingBoxEditor", "autosave"),
                     AutosaveStore.defaultDirectory("Windows 11", "C:\\Users\\ann", " ", null));
        assertEquals(Path.of("/home/ann", ".local", "share", "boundingboxeditor", "autosave"),
                     AutosaveStore.defaultDirectory("Linux", "/home/ann", null, ""));
    }

    @Test
    void onCreatingTheDefaultStore_ShouldUseTheOverriddenOrTheApplicationDataFolder() {
        final String overridden = System.getProperty(AutosaveStore.DIRECTORY_PROPERTY);

        try {
            System.setProperty(AutosaveStore.DIRECTORY_PROPERTY, "/tmp/autosave-test");
            assertEquals(Path.of("/tmp/autosave-test"), AutosaveStore.createDefault().directory());

            System.clearProperty(AutosaveStore.DIRECTORY_PROPERTY);
            assertEquals(AutosaveStore.defaultDirectory(System.getProperty("os.name"),
                                                        System.getProperty("user.home"), System.getenv("APPDATA"),
                                                        System.getenv("XDG_DATA_HOME")),
                         AutosaveStore.createDefault().directory());
        } finally {
            if(overridden == null) {
                System.clearProperty(AutosaveStore.DIRECTORY_PROPERTY);
            } else {
                System.setProperty(AutosaveStore.DIRECTORY_PROPERTY, overridden);
            }
        }
    }

    @Test
    void onNamingFiles_ShouldUseOneStableFilePerFolder(@TempDir Path tempDir) {
        final AutosaveStore store = new AutosaveStore(tempDir);
        final File folder = new File("/images/cats");

        assertEquals(store.fileFor(folder), store.fileFor(new File("/images/dogs/../cats")));
        assertNotEquals(store.fileFor(folder), store.fileFor(new File("/images/dogs")));
        assertEquals(tempDir, store.fileFor(folder).getParent());
        assertTrue(store.fileFor(folder).getFileName().toString().endsWith(".json"));
    }

    @Test
    void onSavingAndDeleting_ShouldWriteLoadableAnnotationsAndRemoveThem(@TempDir Path tempDir) throws Exception {
        final AutosaveStore store = new AutosaveStore(tempDir.resolve("autosave"));
        final File folder = tempDir.resolve("images").toFile();
        final ObjectCategory category = new ObjectCategory("cat", Color.RED);
        final MutableMask mask = new MutableMask(100, 50);
        mask.fillCircle(30, 20, 10, true);
        final BoundingBoxData box = new BoundingBoxData(category, 0.1, 0.1, 0.5, 0.5, List.of("difficult"));
        box.setParts(List.of(new BoundingMaskData(category, mask.toBitmap(), List.of())));

        assertTrue(store.lastSaved(folder).isEmpty());

        store.save(folder, annotations(category, box));
        assertTrue(store.lastSaved(folder).isPresent());

        // Saving again replaces the file, and no temporary files are left.
        store.save(folder, annotations(category, box));
        assertEquals(List.of(store.fileFor(folder)), listFiles(tempDir.resolve("autosave")));

        final ImageAnnotationImportResult result = ImageAnnotationLoadStrategy
                .createStrategy(ImageAnnotationLoadStrategy.Type.JSON)
                .load(store.fileFor(folder), Set.of("a.jpg"), new HashMap<>(), new SimpleDoubleProperty());
        assertTrue(result.getErrorTableEntries().isEmpty(), () -> result.getErrorTableEntries().toString());
        final BoundingShapeData loaded = result.getImageAnnotationData().imageAnnotations().iterator().next()
                                               .getBoundingShapeData().getFirst();
        assertEquals(box, loaded);
        assertEquals(mask.toBitmap(), ((BoundingMaskData) loaded.getParts().getFirst()).getMask());

        store.delete(folder);
        assertTrue(store.lastSaved(folder).isEmpty());
        // Deleting again is harmless.
        store.delete(folder);
    }

    private static ImageAnnotationData annotations(ObjectCategory category, BoundingShapeData shape) {
        final ImageMetaData image = new ImageMetaData("a.jpg", "images", "file:/images/a.jpg", 100, 50, 3);
        return new ImageAnnotationData(List.of(new ImageAnnotation(image, List.of(shape))),
                                       Map.of(category.getName(), 2), Map.of(category.getName(), category));
    }

    private static List<Path> listFiles(Path directory) throws Exception {
        try(var files = Files.list(directory)) {
            return files.toList();
        }
    }
}
