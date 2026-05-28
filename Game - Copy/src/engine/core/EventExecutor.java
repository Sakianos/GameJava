package engine.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class EventExecutor {
    private JsonArray events;
    private Random random = new Random();

    public static class EventResult {
        public String response;
        public List<Consequence> consequences;
    }

    public static class Consequence {
        public String type;
        public JsonElement data;
    }

    public EventExecutor(String eventsJsonPath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(eventsJsonPath)));
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        this.events = root.getAsJsonArray("events");
    }

    public EventResult executeEvent(String action, String argument, String targetNpc, GameState state) {
        EventResult result = new EventResult();
        result.consequences = new ArrayList<>();

        for (JsonElement eventElem : events) {
            JsonObject event = eventElem.getAsJsonObject();
            if (matchesEvent(event, action, argument, targetNpc, state)) {
                if (meetsConditions(event.getAsJsonArray("conditions"), state)) {
                    JsonArray responses = event.getAsJsonArray("responses");
                    JsonObject selectedResponse = responses.get(random.nextInt(responses.size())).getAsJsonObject();
                    result.response = selectedResponse.get("text").getAsString();

                    JsonArray consequences = event.getAsJsonArray("consequences");
                    if (consequences != null) {
                        for (JsonElement consElem : consequences) {
                            JsonObject cons = consElem.getAsJsonObject();
                            Consequence c = new Consequence();
                            c.type = cons.get("type").getAsString();
                            c.data = cons;
                            result.consequences.add(c);
                        }
                    }
                    return result;
                }
            }
        }
        return null;
    }

    private boolean matchesEvent(JsonObject event, String action, String argument, String targetNpc, GameState state) {
        JsonObject trigger = event.getAsJsonObject("trigger");
        String triggerAction = trigger.get("action").getAsString();

        if (!triggerAction.equals(action) && !triggerAction.equals("*")) return false;

        if (triggerAction.equals("custom")) {
            String input = trigger.get("input").getAsString();
            return input.equalsIgnoreCase(argument);
        }

        if (trigger.has("target")) {
            String target = trigger.get("target").getAsString();
            if (!target.equals("*") && !target.equalsIgnoreCase(targetNpc)) return false;
        }

        if (trigger.has("item")) {
            String item = trigger.get("item").getAsString();
            if (!item.equals("*") && !item.equalsIgnoreCase(argument)) return false;
        }

        if (trigger.has("direction")) {
            String direction = trigger.get("direction").getAsString();
            if (!direction.equals("*") && !direction.equalsIgnoreCase(argument)) return false;
        }

        return true;
    }

    private boolean meetsConditions(JsonArray conditions, GameState state) {
        if (conditions == null || conditions.size() == 0) return true;

        for (JsonElement condElem : conditions) {
            JsonObject condition = condElem.getAsJsonObject();
            String type = condition.get("type").getAsString();

            switch (type) {
                case "room":
                    if (!state.getCurrentRoom().getId().equals(condition.get("value").getAsString())) return false;
                    break;
                case "flag":
                    if (state.getFlag(condition.get("key").getAsString()) != condition.get("value").getAsBoolean()) return false;
                    break;
                case "hasItem":
                    if (!state.hasItem(condition.get("value").getAsString())) return false;
                    break;
                case "hasCoins":
                    if (state.getCoins() < condition.get("amount").getAsInt()) return false;
                    break;
                case "npcInRoom":
                    if (state.getCurrentRoom().getNpcByName(condition.get("value").getAsString()) == null) return false;
                    break;
                case "itemInRoom":
                    if (!state.getCurrentRoom().getItems().containsKey(condition.get("value").getAsString().toLowerCase())) return false;
                    break;
                case "notFlag":
                    if (state.getFlag(condition.get("key").getAsString())) return false;
                    break;
            }
        }
        return true;
    }

    public void executeConsequences(List<Consequence> consequences, GameState state) {
        for (Consequence c : consequences) {
            JsonObject data = (JsonObject) c.data;
            switch (c.type) {
                case "setFlag":
                    state.setFlag(data.get("key").getAsString(), data.get("value").getAsBoolean());
                    break;
                case "removeItem":
                    state.removeFromInventory(data.get("item").getAsString());
                    break;
                case "addToInventory":
                    String itemToAdd = data.get("item").getAsString();
                    // ΔΙΟΡΘΩΣΗ: Προσθήκη 4ου ορίσματος (type)
                    Item newItem = new Item(itemToAdd, itemToAdd, "Αντικείμενο: " + itemToAdd, "general");
                    state.addToInventory(newItem);
                    break;
                case "removeCoins":
                    state.removeCoins(data.get("amount").getAsInt());
                    break;
                case "addCoins":
                    state.addCoins(data.get("amount").getAsInt());
                    break;
                case "moveToRoom":
                    state.setCurrentRoom(data.get("room").getAsString());
                    break;
                case "addItem":
                    String itemId = data.get("item").getAsString();
                    // ΔΙΟΡΘΩΣΗ: Προσθήκη 4ου ορίσματος (type)
                    Item item = new Item(itemId, itemId, "Ένα αντικείμενο στο χώρο", "general");
                    state.getCurrentRoom().addItem(item);
                    break;
            }
        }
    }
}