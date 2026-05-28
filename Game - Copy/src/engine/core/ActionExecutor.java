package engine.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ActionExecutor {
    private JsonArray actions;
    private EventExecutor eventExecutor;

    public static class ActionResult {
        public String response;
        public List<EventExecutor.Consequence> consequences;
        public boolean success;
    }

    public ActionExecutor(String actionsJsonPath, EventExecutor eventExecutor) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(actionsJsonPath)));
        this.actions = JsonParser.parseString(content).getAsJsonArray();
        this.eventExecutor = eventExecutor;
    }

    public ActionResult executeAction(String actionId, String argument, GameState state) {
        ActionResult result = new ActionResult();
        result.consequences = new ArrayList<>();

        JsonObject action = findAction(actionId);
        if (action == null) {
            result.success = false;
            result.response = "Άγνωστη ενέργεια.";
            return result;
        }

        JsonObject logic = action.getAsJsonObject("logic");

        // Check conditions
        if (!meetsConditions(logic.getAsJsonArray("conditions"), argument, state)) {
            result.success = false;
            result.response = logic.getAsJsonObject("failure").get("message").getAsString();
            return result;
        }

        // Execute success
        result.success = true;
        JsonObject successObj = logic.getAsJsonObject("success");
        String message = successObj.get("message").getAsString();
        result.response = processMessage(message, argument, state);

        // --- Η ΔΙΟΡΘΩΣΗ ΕΔΩ ---
        if (successObj.has("consequences")) {
            JsonArray consequences = successObj.get("consequences").getAsJsonArray();
            for (JsonElement consElem : consequences) {
                JsonObject cons = consElem.getAsJsonObject();
                EventExecutor.Consequence c = new EventExecutor.Consequence();
                c.type = cons.get("type").getAsString();
                c.data = cons;
                result.consequences.add(c);
            }
        }

        return result;
    }

    private JsonObject findAction(String actionId) {
        for (JsonElement actionElem : actions) {
            JsonObject action = actionElem.getAsJsonObject();
            if (action.get("id").getAsString().equals(actionId)) {
                return action;
            }
        }
        return null;
    }

    private boolean meetsConditions(JsonArray conditions, String argument, GameState state) {
        if (conditions == null || conditions.size() == 0) {
            return true;
        }

        for (JsonElement condElem : conditions) {
            JsonObject condition = condElem.getAsJsonObject();
            String type = condition.get("type").getAsString();

            switch (type) {
                case "npcInRoom":
                    String npcName = processTemplate(condition.get("npc").getAsString(), argument, state);
                    if (state.getCurrentRoom().getNpcByName(npcName) == null) {
                        return false;
                    }
                    break;

                case "itemInRoom":
                    String itemId = processTemplate(condition.get("item").getAsString(), argument, state);
                    if (!state.getCurrentRoom().getItems().containsKey(itemId.toLowerCase())) {
                        return false;
                    }
                    break;

                case "hasItem":
                    String hasItemId = processTemplate(condition.get("item").getAsString(), argument, state);
                    if (!state.hasItem(hasItemId)) {
                        return false;
                    }
                    break;

                case "room":
                    String roomId = condition.get("value").getAsString();
                    if (!state.getCurrentRoom().getId().equals(roomId)) {
                        return false;
                    }
                    break;
            }
        }

        return true;
    }

    private String processMessage(String message, String argument, GameState state) {
        message = message.replace("{argument}", argument != null ? argument : "");
        message = message.replace("{direction}", argument != null ? argument : "");

        if (message.contains("{roomName}")) {
            message = message.replace("{roomName}", state.getCurrentRoom().getName());
        }
        if (message.contains("{roomDescription}")) {
            message = message.replace("{roomDescription}", state.getCurrentRoom().getDescription());
        }

        if (message.contains("{inventory}")) {
            StringBuilder inv = new StringBuilder();
            if (state.getInventory().isEmpty()) {
                inv.append("τίποτα");
            } else {
                for (Item item : state.getInventory()) {
                    inv.append(item.getName()).append(", ");
                }
                inv.setLength(inv.length() - 2);
            }
            message = message.replace("{inventory}", inv.toString());
        }

        if (message.contains("{actions}")) {
            StringBuilder acts = new StringBuilder();
            for (JsonElement actionElem : actions) {
                JsonObject action = actionElem.getAsJsonObject();
                acts.append(action.get("description").getAsString()).append(", ");
            }
            acts.setLength(acts.length() - 2);
            message = message.replace("{actions}", acts.toString());
        }

        return message;
    }

    private String processTemplate(String template, String argument, GameState state) {
        return template.replace("{argument}", argument != null ? argument : "");
    }

    public void executeConsequences(List<EventExecutor.Consequence> consequences, GameState state) {
        eventExecutor.executeConsequences(consequences, state);
    }

    public String getActionIdFromAlias(String alias) {
        for (JsonElement actionElem : actions) {
            JsonObject action = actionElem.getAsJsonObject();
            JsonArray aliases = action.getAsJsonArray("aliases");
            for (JsonElement aliasElem : aliases) {
                if (aliasElem.getAsString().equalsIgnoreCase(alias)) {
                    return action.get("id").getAsString();
                }
            }
        }
        return null;
    }
}