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

import com.github.mfl28.boundingboxeditor.model.ImageFileFilter;
import com.github.mfl28.boundingboxeditor.model.Model;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.ui.ImageFileListView.FileInfo;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which image files the image file list shows, and navigates between them.
 * <p>
 * The list only shows the images matching the {@link ImageFileFilter}, plus the current image, which stays
 * visible while it is shown even if edits make it stop matching. The filter is re-evaluated when the user
 * moves to another image, when the filter changes and when annotations are imported or cleared.
 * Positions in the shown list therefore differ from the model's file indices ({@link FileInfo#getFileIndex()}).
 */
class ImageFileFilterController {
    private final Model model;
    private final ReadOnlyIntegerWrapper currentPosition = new ReadOnlyIntegerWrapper(-1);
    private final ReadOnlyIntegerWrapper shownCount = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper matchCount = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyBooleanWrapper hasNext = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper hasPrevious = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper filterActive = new ReadOnlyBooleanWrapper(false);
    private List<FileInfo> allItems = List.of();
    private FilteredList<FileInfo> shownItems = new FilteredList<>(FXCollections.emptyObservableList());
    private ImageFileFilter filter = ImageFileFilter.NONE;
    private FileInfo current;

    ImageFileFilterController(Model model) {
        this.model = model;
    }

    /**
     * Sets the items of all loaded image files and resets the filter.
     *
     * @param items            the items, ordered by file index
     * @param currentFileIndex the model's current file index
     * @return the list of shown items, for the image file list
     */
    ObservableList<FileInfo> setItems(List<FileInfo> items, int currentFileIndex) {
        allItems = List.copyOf(items);
        // A new list for each folder, so that the list view notices the change and reloads its thumbnails.
        shownItems = new FilteredList<>(FXCollections.unmodifiableObservableList(
                FXCollections.observableArrayList(allItems)));
        filter = ImageFileFilter.NONE;
        current = getItem(currentFileIndex).orElse(null);
        update();
        return shownItems;
    }

    /**
     * Removes all items.
     */
    void clear() {
        allItems = List.of();
        shownItems = new FilteredList<>(FXCollections.emptyObservableList());
        filter = ImageFileFilter.NONE;
        current = null;
        update();
    }

    /**
     * Applies a new filter. The caller must store the shown image's shapes in the model first, so that the
     * current image is judged by its latest shapes.
     *
     * @param newFilter the filter
     * @return the item to show instead of the current image, if the current image does not match the filter but
     * others do
     */
    Optional<FileInfo> setFilter(ImageFileFilter newFilter) {
        filter = newFilter;
        final Set<FileInfo> matching = update();

        if(current == null || matching.contains(current)) {
            return Optional.empty();
        }

        return shownItems.stream().filter(matching::contains).findFirst();
    }

    /**
     * Moves the pin to the newly shown image and re-evaluates the filter, so that a previously shown image
     * that no longer matches is hidden.
     *
     * @param fileIndex the model's new current file index
     */
    void onCurrentImageChanged(int fileIndex) {
        current = getItem(fileIndex).orElse(null);
        update();
    }

    /**
     * Re-evaluates the filter after annotations changed.
     */
    void refresh() {
        update();
    }

    Optional<FileInfo> nextItem() {
        return hasNext.get() ? Optional.of(shownItems.get(currentPosition.get() + 1)) : Optional.empty();
    }

    Optional<FileInfo> previousItem() {
        return hasPrevious.get() ? Optional.of(shownItems.get(currentPosition.get() - 1)) : Optional.empty();
    }

    Optional<FileInfo> getItem(int fileIndex) {
        return fileIndex >= 0 && fileIndex < allItems.size() ? Optional.of(allItems.get(fileIndex)) : Optional.empty();
    }

    Optional<FileInfo> getCurrentItem() {
        return Optional.ofNullable(current);
    }

    List<FileInfo> getAllItems() {
        return allItems;
    }

    ObservableList<FileInfo> getShownItems() {
        return shownItems;
    }

    ImageFileFilter getFilter() {
        return filter;
    }

    ReadOnlyIntegerProperty currentPositionProperty() {
        return currentPosition.getReadOnlyProperty();
    }

    ReadOnlyIntegerProperty shownCountProperty() {
        return shownCount.getReadOnlyProperty();
    }

    ReadOnlyIntegerProperty matchCountProperty() {
        return matchCount.getReadOnlyProperty();
    }

    ReadOnlyBooleanProperty hasNextProperty() {
        return hasNext.getReadOnlyProperty();
    }

    ReadOnlyBooleanProperty hasPreviousProperty() {
        return hasPrevious.getReadOnlyProperty();
    }

    ReadOnlyBooleanProperty filterActiveProperty() {
        return filterActive.getReadOnlyProperty();
    }

    private Set<FileInfo> update() {
        final Set<FileInfo> matching = Collections.newSetFromMap(new IdentityHashMap<>());

        if(filter.isActive()) {
            final Map<String, ImageAnnotation> annotations = model.getImageFileNameToAnnotationMap();

            for(FileInfo item : allItems) {
                if(filter.matches(item.getFileName(), annotations.get(item.getFileName()))) {
                    matching.add(item);
                }
            }

            final FileInfo pinned = current;
            shownItems.setPredicate(item -> item.equals(pinned) || matching.contains(item));
        } else {
            matching.addAll(allItems);
            shownItems.setPredicate(null);
        }

        final int position = current == null ? -1 : shownItems.indexOf(current);
        currentPosition.set(position);
        shownCount.set(shownItems.size());
        matchCount.set(matching.size());
        hasPrevious.set(position > 0);
        hasNext.set(position >= 0 && position < shownItems.size() - 1);
        filterActive.set(filter.isActive());
        return matching;
    }
}
