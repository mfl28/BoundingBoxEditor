/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.data;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.google.gson.annotations.SerializedName;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Holds metadata information about an image.
 */
public final class ImageMetaData {
    private static final List<String> supportedImageFormats = List.of("jpeg", "bmp", "png");
    private static final String NOT_AN_IMAGE_FILE_ERROR_MESSAGE = "Not an image file.";
    private static final String UNSUPPORTED_IMAGE_FORMAT_ERROR_MESSAGE = "Unsupported image file format.";
    private static final String INVALID_IMAGE_FILE_ERROR_MESSAGE = "Invalid image file.";
    private static final String JPEG_READ_ERROR_MESSAGE = "Cannot read JPEG metadata.";
    private final String fileName;
    private ImageMetaDataDetails details;

    /**
     * Creates a new ImageMetaData object.
     *
     * @param fileName    the name of the image-file
     * @param folderName  the name of the folder containing the image-file
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     * @param imageDepth  the depth (= number of channels) of the image
     */
    public ImageMetaData(String fileName, String folderName, String url, double imageWidth, double imageHeight, int imageDepth) {
        this.fileName = fileName;
        this.details = new ImageMetaDataDetails(folderName, url, imageWidth, imageHeight, imageDepth, 1);
    }

    public ImageMetaData(String fileName, String folderName, String url, double imageWidth, double imageHeight, int imageDepth, int orientation) {
        this.fileName = fileName;
        this.details = new ImageMetaDataDetails(folderName, url, imageWidth, imageHeight, imageDepth, orientation);
    }

    public ImageMetaData(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Constructs an ImageMetaData object from an image-file without loading the whole image.
     *
     * @param imageFile the image-file
     * @return an ImageMetaData object containing metadata about the provided image-file
     */
    public static ImageMetaData fromFile(File imageFile) throws IOException {
        ImageDimensions imageDimensions = readImageDimensionsFromFile(imageFile);
        return new ImageMetaData(imageFile.getName(), imageFile.toPath().getParent().toFile().getName(),
                imageFile.toURI().toString(),
                imageDimensions.width(), imageDimensions.height(), imageDimensions.depth(), imageDimensions.orientation());
    }

    /**
     * Returns the width of the image.
     *
     * @return the width
     */
    public double getImageWidth() {
        return details.imageWidth;
    }

    public double getOrientedWidth() {
        if(details.orientation >= 5) {
            return getImageHeight();
        }

        return getImageWidth();
    }

    /**
     * Returns the height of the image.
     *
     * @return the height
     */
    public double getImageHeight() {
        return details.imageHeight;
    }

    public double getOrientedHeight() {
        if(details.orientation >= 5) {
            return getImageWidth();
        }

        return getImageHeight();
    }

    /**
     * Returns the depth (= number of channels) of the image.
     *
     * @return the depth
     */
    public int getImageDepth() {
        return details.imageDepth;
    }

    public int getOrientation() {
        return details.orientation;
    }

    /**
     * Returns the filename of the image.
     *
     * @return the filename
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the name of the folder that contains the image.
     *
     * @return the folder-name
     */
    public String getFolderName() {
        return details.folderName;
    }

    public String getFileUrl() {
        return details.url;
    }

    public boolean hasDetails() {
        return details != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, details);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ImageMetaData other) {
            return Objects.equals(fileName, other.fileName) && Objects.equals(details, other.details);
        }
        return false;
    }

    public String getDimensionsString() {
        if(details == null) {
            return "[]";
        }

        return "[" + (int) getOrientedWidth() + " x " + (int) getOrientedHeight() + "]";
    }

    private static ImageDimensions readImageDimensionsFromFile(File imageFile) throws IOException {
        double width;
        double height;
        int numComponents;
        int orientation = 1;

        try(ImageInputStream imageStream = ImageIO.createImageInputStream(imageFile)) {
            if(imageStream == null) {
                throw new NotAnImageFileException(NOT_AN_IMAGE_FILE_ERROR_MESSAGE);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);

            if(readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageStream);

                    final String imageFormatName = reader.getFormatName().toLowerCase();

                    if(!supportedImageFormats.contains(imageFormatName)) {
                        throw new UnsupportedImageFileException(UNSUPPORTED_IMAGE_FORMAT_ERROR_MESSAGE);
                    }

                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                    numComponents = reader.getRawImageType(0).getNumComponents();

                    if(imageFormatName.equals("jpeg")) {
                        try {
                            final ExifIFD0Directory exifDirectory = JpegMetadataReader.readMetadata(imageFile).getFirstDirectoryOfType(ExifIFD0Directory.class);

                            if(exifDirectory != null && exifDirectory.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
                                orientation = exifDirectory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
                            }
                        } catch(JpegProcessingException | MetadataException | IOException exception) {
                            throw new UnsupportedImageFileException(JPEG_READ_ERROR_MESSAGE);
                        }
                    }
                } finally {
                    reader.dispose();
                }
            } else {
                throw new UnsupportedImageFileException(INVALID_IMAGE_FILE_ERROR_MESSAGE);
            }
        }

        return new ImageDimensions(width, height, numComponents, orientation);
    }

    private record ImageMetaDataDetails(String folderName, String url, @SerializedName("width") double imageWidth,
                                        @SerializedName("height") double imageHeight,
                                        @SerializedName("depth") int imageDepth,
                                        int orientation) {
    }

    public static class NotAnImageFileException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 5256590447321177896L;

        public NotAnImageFileException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class UnsupportedImageFileException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = -4143199502921469708L;

        public UnsupportedImageFileException(String errorMessage) {
            super(errorMessage);
        }
    }

    private record ImageDimensions(double width, double height, int depth, int orientation) {
    }
}
