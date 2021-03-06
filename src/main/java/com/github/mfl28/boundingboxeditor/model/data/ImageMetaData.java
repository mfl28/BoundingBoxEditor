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
package com.github.mfl28.boundingboxeditor.model.data;

import com.google.gson.annotations.SerializedName;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Holds metadata information about an image.
 */
public class ImageMetaData {
    private static final List<String> supportedImageFormats = List.of("jpeg", "bmp", "png");
    private static final String NOT_AN_IMAGE_FILE_ERROR_MESSAGE = "Not an image file.";
    private static final String UNSUPPORTED_IMAGE_FORMAT_ERROR_MESSAGE = "Unsupported image file format.";
    private static final String INVALID_OR_UNSUPPORTED_IMAGE_ERROR_MESSAGE = "Invalid or unsupported image file.";
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
    public ImageMetaData(String fileName, String folderName, double imageWidth, double imageHeight, int imageDepth) {
        this.fileName = fileName;
        this.details = new ImageMetaDataDetails(folderName, imageWidth, imageHeight, imageDepth);
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
                                 imageDimensions.getWidth(), imageDimensions.getHeight(), imageDimensions.getDepth());
    }

    /**
     * Returns the width of the image.
     *
     * @return the width
     */
    public double getImageWidth() {
        return details.imageWidth;
    }

    /**
     * Returns the height of the image.
     *
     * @return the height
     */
    public double getImageHeight() {
        return details.imageHeight;
    }

    /**
     * Returns the depth (= number of channels) of the image.
     *
     * @return the depth
     */
    public int getImageDepth() {
        return details.imageDepth;
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

    public boolean hasDetails() {
        return details != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, details);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ImageMetaData) {
            ImageMetaData other = (ImageMetaData) obj;
            return Objects.equals(fileName, other.fileName) && Objects.equals(details, other.details);
        }
        return false;
    }

    public String getDimensionsString() {
        if(details == null) {
            return "[]";
        }

        return "[" + (int) getImageWidth() + " x " + (int) getImageHeight() + "]";
    }

    private static ImageDimensions readImageDimensionsFromFile(File imageFile) throws IOException {
        double width;
        double height;
        int numComponents;
        // Source: https://stackoverflow.com/a/1560052
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
                } finally {
                    reader.dispose();
                }
            } else {
                throw new UnsupportedImageFileException(INVALID_OR_UNSUPPORTED_IMAGE_ERROR_MESSAGE);
            }
        }

        return new ImageDimensions(width, height, numComponents);
    }

    private static class ImageMetaDataDetails {
        private final String folderName;
        @SerializedName("width")
        private final double imageWidth;
        @SerializedName("height")
        private final double imageHeight;
        @SerializedName("depth")
        private final int imageDepth;

        ImageMetaDataDetails(String folderName, double imageWidth, double imageHeight, int imageDepth) {
            this.folderName = folderName;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.imageDepth = imageDepth;
        }

        @Override
        public int hashCode() {
            return Objects.hash(imageWidth, imageHeight, imageDepth);
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }

            if(!(o instanceof ImageMetaDataDetails)) {
                return false;
            }

            ImageMetaDataDetails that = (ImageMetaDataDetails) o;

            return Double.compare(that.imageWidth, imageWidth) == 0 &&
                    Double.compare(that.imageHeight, imageHeight) == 0 &&
                    imageDepth == that.imageDepth;
        }
    }

    public static class NotAnImageFileException extends RuntimeException {
        private static final long serialVersionUID = 5256590447321177896L;

        public NotAnImageFileException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class UnsupportedImageFileException extends RuntimeException {
        private static final long serialVersionUID = -4143199502921469708L;

        public UnsupportedImageFileException(String errorMessage) {
            super(errorMessage);
        }
    }

    private static class ImageDimensions {
        private final double width;
        private final double height;
        private final int depth;

        ImageDimensions(double width, double height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        double getWidth() {
            return width;
        }

        double getHeight() {
            return height;
        }

        int getDepth() {
            return depth;
        }
    }
}
