package engine.core;

public class Item {
    private String id;
    private String name;
    private String description;
    private String pickupMessage;
    private String englishName;
    private String englishDescription;

    public Item(String id, String name, String description, String pickupMessage) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pickupMessage = pickupMessage;
        this.englishName = name;
        this.englishDescription = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPickupMessage() { return pickupMessage; }
    public String getEnglishName() { return englishName; }
    public String getEnglishDescription() { return englishDescription; }

    public void setEnglishName(String englishName) { this.englishName = englishName; }
    public void setEnglishDescription(String englishDescription) { this.englishDescription = englishDescription; }
}
