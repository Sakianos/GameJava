package engine.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

public class SaveManager {

    private static final String SAVE_FILE = "savegame.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save(GameState state) {
        SaveData data = new SaveData();
        data.currentRoomId = state.getCurrentRoomId();
        data.coins = state.getCoins();
        data.flags = new HashMap<>(state.getFlags());

        data.inventory = new HashMap<>();
        for (Item item : state.getInventory()) {
            data.inventory.put(item.getId(), new ItemSnapshot(item));
        }

        data.difficulty = state.getDifficulty();

        data.roomItems = new HashMap<>();
        for (Map.Entry<String, Room> entry : state.getRooms().entrySet()) {
            data.roomItems.put(entry.getKey(), new ArrayList<>(entry.getValue().getItems().keySet()));
        }

        try (Writer writer = new FileWriter(SAVE_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Σφάλμα αποθήκευσης: " + e.getMessage());
        }
    }

    public static boolean hasSave() {
        return new File(SAVE_FILE).exists();
    }

    public static void deleteSave() {
        new File(SAVE_FILE).delete();
    }

    public static void load(GameState state) {
        try (Reader reader = new FileReader(SAVE_FILE)) {
            SaveData data = GSON.fromJson(reader, SaveData.class);

            // Αφαίρεση αντικειμένων από δωμάτια που έχουν ήδη μαζευτεί
            if (data.roomItems != null) {
                for (Map.Entry<String, Room> entry : state.getRooms().entrySet()) {
                    Room room = entry.getValue();
                    List<String> savedIds = data.roomItems.get(entry.getKey());
                    if (savedIds == null) continue;
                    List<String> toRemove = new ArrayList<>();
                    for (String id : room.getItems().keySet()) {
                        if (!savedIds.contains(id)) toRemove.add(id);
                    }
                    toRemove.forEach(room::removeItem);
                }
            }

            // Επαναφορά inventory
            if (data.inventory != null) {
                for (ItemSnapshot snap : data.inventory.values()) {
                    Item item = new Item(snap.id, snap.name, snap.description, snap.pickupMessage);
                    if (snap.englishName != null) item.setEnglishName(snap.englishName);
                    if (snap.englishDescription != null) item.setEnglishDescription(snap.englishDescription);
                    state.addToInventory(item);
                }
            }

            state.setCurrentRoom(data.currentRoomId);
            state.setCoins(data.coins);
            if (data.difficulty != null) state.setDifficulty(data.difficulty);

            if (data.flags != null) {
                data.flags.forEach(state::setFlag);
            }

        } catch (IOException e) {
            System.err.println("Σφάλμα φόρτωσης: " + e.getMessage());
        }
    }

    private static class SaveData {
        String currentRoomId;
        String difficulty;
        int coins;
        Map<String, Boolean> flags;
        Map<String, ItemSnapshot> inventory;
        Map<String, List<String>> roomItems;
    }

    private static class ItemSnapshot {
        String id, name, description, pickupMessage, englishName, englishDescription;

        ItemSnapshot(Item item) {
            this.id = item.getId();
            this.name = item.getName();
            this.description = item.getDescription();
            this.pickupMessage = item.getPickupMessage();
            this.englishName = item.getEnglishName();
            this.englishDescription = item.getEnglishDescription();
        }
    }
}
