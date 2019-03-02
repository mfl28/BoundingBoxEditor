public class BoundingBox {
    private final String categoryName;
    private final double xMin;
    private final double yMin;
    private final double xMax;
    private final double yMax;

    // TODO: Optionally add attributes: pose (Front, back, right etc.), truncated(0 or 1), difficult(0 or 1)
    //       occluded (0 or 1)

    public BoundingBox(final String categoryName, double xMin, double yMin, double xMax, double yMax){
        this.categoryName = categoryName;
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public double getxMin() {
        return xMin;
    }

    public double getyMin() {
        return yMin;
    }

    public double getxMax() {
        return xMax;
    }

    public double getyMax() {
        return yMax;
    }
}
