import com.auction.project.entitiesclasses.Entity;

public abstract class Item extends Entity {
    private String name;
    private double startingPrice;

    public Item(String id, String name, double startingPrice) {
        super(id);
        this.name = name;
        this.startingPrice = startingPrice;
    }

    public String getName() { return name; }
}