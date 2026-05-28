package engine.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SaveManager {

    public static final int MAX_SLOTS = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static String slotFile(int slot) {
        return "savegame_" + slot + ".json";
    }

    // ── Slot checks ───────────────────────────────────────────────────────

    public static boolean hasSave(int slot) {
        return new File(slotFile(slot)).exists();
    }

    public static boolean hasAnySave() {
        for (int i = 1; i <= MAX_SLOTS; i++) if (hasSave(i)) return true;
        return false;
    }

    public static void deleteSave(int slot) {
        new File(slotFile(slot)).delete();
    }

    // ── SaveInfo — για εμφάνιση στο μενού ────────────────────────────────

    public static class SaveInfo {
        public final int    slot;
        public final boolean exists;
        public final String  roomName;
        public final String  difficulty;
        public final int     coins;
        public final String  savedAt;

        private SaveInfo(int slot, boolean exists,
                         String roomName, String difficulty, int coins, String savedAt) {
            this.slot       = slot;
            this.exists     = exists;
            this.roomName   = roomName;
            this.difficulty = difficulty;
            this.coins      = coins;
            this.savedAt    = savedAt;
        }

        static SaveInfo empty(int slot) {
            return new SaveInfo(slot, false, "", "", 0, "");
        }

        static SaveInfo from(int slot, SaveData data) {
            return new SaveInfo(slot, true,
                    data.currentRoomName != null ? data.currentRoomName : "Άγνωστο",
                    data.difficulty      != null ? data.difficulty      : "—",
                    data.coins,
                    data.savedAt         != null ? data.savedAt         : "—");
        }

        /** Μία γραμμή για εμφάνιση στο μενού */
        public String toMenuLine() {
            if (!exists)
                return "  " + slot + ". [ Άδειο ]";
            return String.format("  %d. %-26s | %-10s | %2d νομ. | %s",
                    slot, roomName, difficulty, coins, savedAt);
        }
    }

    public static List<SaveInfo> listSaves() {
        List<SaveInfo> list = new ArrayList<>();
        for (int i = 1; i <= MAX_SLOTS; i++) {
            if (!hasSave(i)) { list.add(SaveInfo.empty(i)); continue; }
            try (Reader r = new FileReader(slotFile(i))) {
                list.add(SaveInfo.from(i, GSON.fromJson(r, SaveData.class)));
            } catch (IOException e) {
                list.add(SaveInfo.empty(i));
            }
        }
        return list;
    }

    // ── Save ─────────────────────────────────────────────────────────────

    /** Convenience: αποθηκεύει στο slot που είναι αποθηκευμένο στο state. */
    public static void save(GameState state) {
        save(state, state.getCurrentSlot());
    }

    public static void save(GameState state, int slot) {
        SaveData data = new SaveData();
        data.currentRoomId   = state.getCurrentRoomId();
        data.currentRoomName = (state.getCurrentRoom() != null)
                ? state.getCurrentRoom().getName() : "";
        data.savedAt   = LocalDateTime.now().format(DATE_FMT);
        data.coins     = state.getCoins();
        data.flags     = new HashMap<>(state.getFlags());
        data.difficulty = state.getDifficulty();

        data.inventory = new HashMap<>();
        for (Item item : state.getInventory()) {
            data.inventory.put(item.getId(), new ItemSnapshot(item));
        }

        data.roomItems = new HashMap<>();
        for (Map.Entry<String, Room> entry : state.getRooms().entrySet()) {
            List<ItemSnapshot> snapshots = new ArrayList<>();
            for (Item item : entry.getValue().getItems().values()) {
                snapshots.add(new ItemSnapshot(item));
            }
            data.roomItems.put(entry.getKey(), snapshots);
        }

        try (Writer writer = new FileWriter(slotFile(slot))) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Σφάλμα αποθήκευσης: " + e.getMessage());
        }
    }

    // ── Load ─────────────────────────────────────────────────────────────

    public static void load(GameState state, int slot) {
        try (Reader reader = new FileReader(slotFile(slot))) {
            SaveData data = GSON.fromJson(reader, SaveData.class);

            // Επαναφορά αντικειμένων δωματίων (συμπεριλαμβάνει dropped items)
            if (data.roomItems != null) {
                for (Map.Entry<String, Room> entry : state.getRooms().entrySet()) {
                    Room room = entry.getValue();
                    room.getItems().clear();
                    List<ItemSnapshot> savedItems = data.roomItems.get(entry.getKey());
                    if (savedItems == null) continue;
                    for (ItemSnapshot snap : savedItems) {
                        Item item = new Item(snap.id, snap.name, snap.description, snap.pickupMessage);
                        if (snap.englishName != null) item.setEnglishName(snap.englishName);
                        if (snap.englishDescription != null) item.setEnglishDescription(snap.englishDescription);
                        room.addItem(item);
                    }
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
            if (data.flags != null) data.flags.forEach(state::setFlag);

        } catch (IOException e) {
            System.err.println("Σφάλμα φόρτωσης: " + e.getMessage());
        }
    }

    // ── Internal data classes ─────────────────────────────────────────────

    static class SaveData {
        String currentRoomId;
        String currentRoomName;
        String difficulty;
        String savedAt;
        int    coins;
        Map<String, Boolean>           flags;
        Map<String, ItemSnapshot>      inventory;
        Map<String, List<ItemSnapshot>> roomItems;
    }

    private static class ItemSnapshot {
        String id, name, description, pickupMessage, englishName, englishDescription;

        ItemSnapshot(Item item) {
            this.id                 = item.getId();
            this.name               = item.getName();
            this.description        = item.getDescription();
            this.pickupMessage      = item.getPickupMessage();
            this.englishName        = item.getEnglishName();
            this.englishDescription = item.getEnglishDescription();
        }
    }
}
