package engine.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class GameState {
    private String currentRoomId;
    private Map<String, Room> rooms = new HashMap<>();
    private Map<String, Item> inventory = new HashMap<>();

    // --- ΠΕΔΙΑ ΓΙΑ ΤΗΝ ΙΣΤΟΡΙΑ (JSON Driven) ---
    private String storyTitle;
    private String storyIntro;
    private String storyExitMessage;

    // --- ΠΕΔΙΑ ΓΙΑ ΤΙΣ ΕΝΤΟΛΕΣ & ΠΡΟΟΔΟ ---
    private int coins = 0;
    private Map<String, Boolean> flags = new HashMap<>();
    private String difficulty = "Κανονικό";
    private int maxInventorySize = 4;

    public void addRoom(Room room) {
        rooms.put(room.getId().toLowerCase(), room);
    }

    public Room getCurrentRoom() {
        return rooms.get(currentRoomId);
    }

    public void setCurrentRoom(String roomId) {
        this.currentRoomId = roomId;
    }

    public Map<String, Room> getRooms() { return rooms; }

    // --- GETTERS & SETTERS ΓΙΑ ΤΗΝ ΙΣΤΟΡΙΑ ---
    public String getStoryTitle() { return storyTitle; }
    public void setStoryTitle(String storyTitle) { this.storyTitle = storyTitle; }

    public String getStoryIntro() { return storyIntro; }
    public void setStoryIntro(String storyIntro) { this.storyIntro = storyIntro; }

    public String getStoryExitMessage() { return storyExitMessage; }
    public void setStoryExitMessage(String storyExitMessage) { this.storyExitMessage = storyExitMessage; }

    // --- ΔΙΑΧΕΙΡΙΣΗ INVENTORY ---
    public void addToInventory(Item item) {
        inventory.put(item.getId().toLowerCase(), item);
    }

    public Item removeFromInventory(String itemId) {
        return inventory.remove(itemId.toLowerCase());
    }

    public boolean hasItem(String itemId) {
        return inventory.containsKey(itemId.toLowerCase());
    }

    public Collection<Item> getInventory() {
        return inventory.values();
    }

    // --- ΔΙΑΧΕΙΡΙΣΗ ΝΟΜΙΣΜΑΤΩΝ ---
    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public void removeCoins(int amount) {
        this.coins = Math.max(0, this.coins - amount);
    }

    // --- ΔΙΑΧΕΙΡΙΣΗ FLAGS ---
    public void setFlag(String key, boolean value) {
        flags.put(key, value);
    }

    public boolean getFlag(String key) {
        return flags.getOrDefault(key, false);
    }

    public Map<String, Boolean> getFlags() { return flags; }

    public String getCurrentRoomId() { return currentRoomId; }

    public void setCoins(int amount) { this.coins = amount; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        switch (difficulty) {
            case "Εύκολο":   this.maxInventorySize = 5; break;
            case "Δύσκολο":  this.maxInventorySize = 3; break;
            default:          this.maxInventorySize = 4; break;
        }
    }

    public int getMaxInventorySize() { return maxInventorySize; }
    public boolean isInventoryFull() { return inventory.size() >= maxInventorySize; }

    public Item removeFromInventoryByName(String name) {
        for (Map.Entry<String, Item> entry : inventory.entrySet()) {
            Item item = entry.getValue();
            if (item.getName().equalsIgnoreCase(name) || item.getEnglishName().equalsIgnoreCase(name)) {
                return inventory.remove(entry.getKey());
            }
        }
        return null;
    }
}