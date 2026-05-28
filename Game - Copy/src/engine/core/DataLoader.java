package engine.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataLoader {

    // 1. Εσωτερικές κλάσεις που αντιστοιχούν στη δομή του JSON
    private static class WorldModel {
        StoryData story; // Προσθήκη για το αντικείμενο story του JSON
        List<RoomData> rooms;

        // Νέα εσωτερική κλάση για τα κείμενα της ιστορίας
        static class StoryData {
            String title;
            String intro;
            String exitMessage;
        }

        static class RoomData {
            String id;
            String name;
            String description;
            Map<String, String> exits;
            List<ItemData> items;
            List<Npc> npcs;
            Map<String, Inspectable> inspectables;
        }

        static class ItemData {
            String id;
            String name;
            String description;
            String pickupMessage;
            String englishName;
            String englishDescription;
        }
    }

    /**
     * Φορτώνει τις ρυθμίσεις των εντολών από το commands.json
     */
    public List<CommandConfig> loadCommandConfigs(String path) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(path)) {
            Type listType = new TypeToken<ArrayList<CommandConfig>>() {}.getType();
            List<CommandConfig> configs = gson.fromJson(reader, listType);
            return (configs != null) ? configs : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Σφάλμα φόρτωσης εντολών: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Φορτώνει τον κόσμο του παιχνιδιού και την ιστορία από το map.json
     */
    public static void loadGame(GameState state, String filePath) {
        Gson gson = new Gson();

        try (Reader reader = new FileReader(filePath)) {
            WorldModel model = gson.fromJson(reader, WorldModel.class);

            if (model == null) {
                System.err.println("Το αρχείο χάρτη είναι άδειο ή κατεστραμμένο.");
                return;
            }

            // --- ΦΟΡΤΩΣΗ ΙΣΤΟΡΙΑΣ (STORY) ---
            if (model.story != null) {
                state.setStoryTitle(model.story.title);
                state.setStoryIntro(model.story.intro);
                state.setStoryExitMessage(model.story.exitMessage);
            }

            // --- ΦΟΡΤΩΣΗ ΔΩΜΑΤΙΩΝ (ROOMS) ---
            if (model.rooms != null) {
                for (WorldModel.RoomData rData : model.rooms) {
                    Room room = new Room(rData.id, rData.name, rData.description);

                    if (rData.inspectables != null) {
                        room.setInspectables(rData.inspectables);
                    }

                    if (rData.exits != null) {
                        rData.exits.forEach(room::addExit);
                    }

                    if (rData.items != null) {
                        for (WorldModel.ItemData iData : rData.items) {
                            String pMessage = (iData.pickupMessage != null) ? iData.pickupMessage : "Πήρες το " + iData.name + ".";
                            Item item = new Item(iData.id, iData.name, iData.description, pMessage);
                            if (iData.englishName != null) item.setEnglishName(iData.englishName);
                            if (iData.englishDescription != null) item.setEnglishDescription(iData.englishDescription);
                            room.addItem(item);
                        }
                    }

                    if (rData.npcs != null) {
                        for (Npc npc : rData.npcs) {
                            room.addNpc(npc);
                        }
                    }

                    state.addRoom(room);
                }

                // Ορισμός αρχικού δωματίου
                if (!model.rooms.isEmpty()) {
                    state.setCurrentRoom(model.rooms.get(0).id);
                }
            }

        } catch (Exception e) {
            System.err.println("Σφάλμα φόρτωσης χάρτη: " + e.getMessage());
            e.printStackTrace();
        }
    }
}