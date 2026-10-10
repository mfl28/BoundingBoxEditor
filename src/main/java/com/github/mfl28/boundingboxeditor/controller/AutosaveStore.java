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

import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.SimpleDoubleProperty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Stores the not yet exported annotations of image folders, one file per folder (in the app's JSON format), in the
 * user's application data folder, so that they can be restored after a crash or a forced quit.
 */
final class AutosaveStore {
    /**
     * Overrides the folder for the autosave files (the tests use it).
     */
    static final String DIRECTORY_PROPERTY = "boundingboxeditor.autosaveDirectory";
    private static final String APP_FOLDER_NAME = "BoundingBoxEditor";
    private static final String LINUX_APP_FOLDER_NAME = "boundingboxeditor";
    private static final String AUTOSAVE_FOLDER_NAME = "autosave";
    private static final String FILE_EXTENSION = ".json";

    private final Path directory;

    AutosaveStore(Path directory) {
        this.directory = directory;
    }

    /**
     * Creates the store for the user's application data folder (or the folder in {@value #DIRECTORY_PROPERTY}).
     *
     * @return the store
     */
    static AutosaveStore createDefault() {
        final String overriddenDirectory = System.getProperty(DIRECTORY_PROPERTY);

        if(overriddenDirectory != null && !overriddenDirectory.isBlank()) {
            return new AutosaveStore(Path.of(overriddenDirectory));
        }

        return new AutosaveStore(defaultDirectory(System.getProperty("os.name", ""), System.getProperty("user.home"),
                                                  System.getenv("APPDATA"), System.getenv("XDG_DATA_HOME")));
    }

    /**
     * Returns the platform's folder for application data: Application Support on macOS, %APPDATA% on Windows and
     * $XDG_DATA_HOME (or ~/.local/share) on Linux.
     */
    static Path defaultDirectory(String osName, String userHome, String appData, String xdgDataHome) {
        final String os = osName.toLowerCase(Locale.ROOT);

        if(os.startsWith("mac")) {
            return Path.of(userHome, "Library", "Application Support", APP_FOLDER_NAME, AUTOSAVE_FOLDER_NAME);
        }

        if(os.startsWith("windows")) {
            final Path base = appData != null && !appData.isBlank() ? Path.of(appData)
                    : Path.of(userHome, "AppData", "Roaming");
            return base.resolve(APP_FOLDER_NAME).resolve(AUTOSAVE_FOLDER_NAME);
        }

        final Path base = xdgDataHome != null && !xdgDataHome.isBlank() ? Path.of(xdgDataHome)
                : Path.of(userHome, ".local", "share");
        return base.resolve(LINUX_APP_FOLDER_NAME).resolve(AUTOSAVE_FOLDER_NAME);
    }

    Path getDirectory() {
        return directory;
    }

    /**
     * Returns the autosave file of an image folder (whether it exists or not). It is named after a hash of the
     * folder's path.
     *
     * @param imageFolder the image folder
     * @return the file
     */
    Path fileFor(File imageFolder) {
        final String path = imageFolder.getAbsoluteFile().toPath().normalize().toString();

        try {
            final byte[] hash = MessageDigest.getInstance("SHA-256").digest(path.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(hash) + FILE_EXTENSION);
        } catch(NoSuchAlgorithmException e) {
            // Every Java platform has SHA-256.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns when the annotations of an image folder were last autosaved.
     *
     * @param imageFolder the image folder
     * @return the time, or an empty optional if there is no autosave
     */
    Optional<Instant> lastSaved(File imageFolder) {
        final Path file = fileFor(imageFolder);

        try {
            return Files.isRegularFile(file) ? Optional.of(Files.getLastModifiedTime(file).toInstant())
                    : Optional.empty();
        } catch(IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Saves the annotations of an image folder. The file is replaced only once the new one is written completely.
     *
     * @param imageFolder the image folder
     * @param annotations the annotations
     * @throws IOException if the file can't be written
     */
    void save(File imageFolder, ImageAnnotationData annotations) throws IOException {
        Files.createDirectories(directory);
        final Path target = fileFor(imageFolder);
        final Path temporary = Files.createTempFile(directory, "autosave", ".tmp");

        try {
            final ImageAnnotationExportResult result = ImageAnnotationSaveStrategy
                    .createStrategy(ImageAnnotationSaveStrategy.Type.JSON)
                    .save(annotations, temporary, new SimpleDoubleProperty());

            if(!result.getErrorTableEntries().isEmpty()) {
                throw new IOException(result.getErrorTableEntries().getFirst().getErrorDescription());
            }

            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch(AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * Deletes the autosave of an image folder, if there is one.
     *
     * @param imageFolder the image folder
     */
    void delete(File imageFolder) {
        try {
            Files.deleteIfExists(fileFor(imageFolder));
        } catch(IOException e) {
            // Nothing to restore from a file that can't be deleted either; it is replaced by the next autosave.
        }
    }
}
