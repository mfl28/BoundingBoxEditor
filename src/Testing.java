import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Testing {
    public static void main(String[] args) {
        List<Double> list = new ArrayList<>();
        list.add(1.0);
        list.add(2.0);
        list.add(3.0);
        System.out.println(list.stream().map(t -> t.toString()).collect(Collectors.joining(", ")));
//        BidirectionalIterator<String> it = new BidirectionalIterator<>(list.listIterator());
//intList.stream().map(i -> i.toString()).collect(Collectors.joining(","))
//        System.out.println(it.hasNext());
//        System.out.println(it.hasPrevious());

    }
}
