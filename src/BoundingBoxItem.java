import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class BoundingBoxItem {
    private StringProperty name;
    private ObjectProperty<Color> color;

    public BoundingBoxItem(){
        this.name = new SimpleStringProperty( "Default");
        this.color = new SimpleObjectProperty<>(Color.ORANGE);
    }

    public BoundingBoxItem(String name, Color color){
        this.name = new SimpleStringProperty(name);
        this.color = new SimpleObjectProperty<>(color);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    StringProperty nameProperty(){
        return name;
    }

    ObjectProperty<Color> colorProperty(){
        return color;
    }
}
