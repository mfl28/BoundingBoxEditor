package com.github.mfl28.boundingboxeditor.model.data;

import com.github.mfl28.boundingboxeditor.ui.BoundingFreehandShapeView;
import com.github.mfl28.boundingboxeditor.ui.BoundingShapeViewable;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class BoundingFreehandShapeData extends BoundingShapeData {
    @SerializedName("path")
    private final List<Double> relativePathPoints;

    public BoundingFreehandShapeData(ObjectCategory category, List<String> tags,
                                     List<Double> relativePathPoints) {
        super(category, tags);
        this.relativePathPoints = relativePathPoints;
    }


    public static List<Double> absoluteToRelativePoints(List<Double> absolutePoints, double width, double height) {
        List<Double> relativePoints = new ArrayList<>(absolutePoints.size());

        for(int i = 0; i < absolutePoints.size(); i += 2) {
            relativePoints.add(absolutePoints.get(i) / width);
            relativePoints.add(absolutePoints.get(i + 1) / height);
        }

        return relativePoints;
    }

    public static List<Double> relativeToAbsolutePoints(List<Double> relativePoints, double width, double height) {
        List<Double> absolutePoints = new ArrayList<>(relativePoints.size());

        for(int i = 0; i < relativePoints.size(); i += 2) {
            absolutePoints.add(relativePoints.get(i) * width);
            absolutePoints.add(relativePoints.get(i + 1) * height);
        }

        return absolutePoints;
    }

    public List<Double> getRelativePathPoints() {
        return relativePathPoints;
    }

    public List<Double> getAbsolutePathPoints(double imageWidth, double imageHeight) {
        return relativeToAbsolutePoints(relativePathPoints, imageWidth, imageHeight);
    }

    @Override
    public BoundingShapeViewable toBoundingShapeView(double imageWidth, double imageHeight) {
        return BoundingFreehandShapeView.fromData(this, imageWidth, imageHeight);
    }

    @Override
    public <T> T accept(BoundingShapeDataVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
