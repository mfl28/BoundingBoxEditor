package BoundingboxEditor.ui;

import BoundingboxEditor.model.BoundingBoxCategory;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class BoundingBoxViewDatabase implements Iterable<ObservableList<BoundingBoxView>> {
    private final IntegerProperty index = new SimpleIntegerProperty(0);
    private final List<ObservableList<BoundingBoxView>> imageBoundingBoxes = new ArrayList<>();
    //private final Map<File, ObservableList<BoundingBoxView>> imageBoundingBoxes = new HashMap<>();

    BoundingBoxViewDatabase(int size) {
        for(int i = 0; i != size; ++i) {
            imageBoundingBoxes.add(FXCollections.observableArrayList());
        }
    }

    public void add(int index, BoundingBoxView boundingBoxView) {
        imageBoundingBoxes.get(index).add(boundingBoxView);
    }

    public void addToCurrentBoundingBoxes(BoundingBoxView boundingBox) {
        imageBoundingBoxes.get(index.get()).add(boundingBox);
    }

    public IntegerProperty indexProperty() {
        return index;
    }

    public ObservableList<BoundingBoxView> get(int index) {
        return imageBoundingBoxes.get(index);
    }

    public boolean isEmpty() {
        return imageBoundingBoxes.isEmpty() || imageBoundingBoxes.stream().allMatch(List::isEmpty);
    }

    @Override
    public Iterator<ObservableList<BoundingBoxView>> iterator() {
        return imageBoundingBoxes.iterator();
    }

    @Override
    public void forEach(Consumer<? super ObservableList<BoundingBoxView>> action) {
        imageBoundingBoxes.forEach(action);
    }

    @Override
    public Spliterator<ObservableList<BoundingBoxView>> spliterator() {
        return imageBoundingBoxes.spliterator();
    }

    public Stream<ObservableList<BoundingBoxView>> stream() {
        return imageBoundingBoxes.stream();
    }

    ObservableList<BoundingBoxView> getCurrentBoundingBoxes() {
        return imageBoundingBoxes.get(index.get());
    }

    void removeFromCurrentBoundingBoxes(BoundingBoxView boundingBox) {
        imageBoundingBoxes.get(index.get()).remove(boundingBox);
    }

    void removeAllFromCurrentBoundingBoxes(Collection<BoundingBoxView> boundingBoxes) {
        imageBoundingBoxes.get(index.get()).removeAll(boundingBoxes);
    }

    boolean containsBoundingBoxWithCategory(BoundingBoxCategory category) {
        for(ObservableList<BoundingBoxView> boundingBoxViews : imageBoundingBoxes) {
            if(boundingBoxViews.stream().anyMatch(item -> item.getBoundingBoxCategory().equals(category))) {
                return true;
            }
        }
        return false;
    }
}
