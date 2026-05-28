package engine.core;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Room {
    private String id;
    private String name;
    private String description;
    private Map<String, String> exits = new HashMap<>();
    private Map<String, Item> items = new HashMap<>();
    private Map<String, Inspectable> inspectables = new HashMap<>();
    private List<Npc> npcs = new ArrayList<>();

    public Room(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getDynamicInspectableDescription(String target) {
        if (!inspectables.containsKey(target.toLowerCase())) {
            return "Δεν βλέπω κάτι ενδιαφέρον στο '" + target + "'.";
        }

        String rawDesc = inspectables.get(target.toLowerCase()).getDescription();
        Pattern pattern = Pattern.compile("\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(rawDesc);

        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(rawDesc, lastEnd, matcher.start());
            String itemId = matcher.group(1).toLowerCase();
            if (items.containsKey(itemId)) {
                sb.append(items.get(itemId).getName());
            } else {
                sb.append("τίποτα");
            }
            lastEnd = matcher.end();
        }
        sb.append(rawDesc.substring(lastEnd));
        return sb.toString();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }

    public void addExit(String direction, String targetRoomId) {
        exits.put(direction.toLowerCase(), targetRoomId);
    }

    public String getExit(String direction) {
        return exits.get(direction.toLowerCase());
    }

    // Η ΔΙΟΡΘΩΣΗ: Προσθήκη getter για το Map των εξόδων
    public Map<String, String> getExits() {
        return exits;
    }

    public void addItem(Item item) {
        items.put(item.getId().toLowerCase(), item);
    }

    public Item removeItem(String itemId) {
        return items.remove(itemId.toLowerCase());
    }

    public Map<String, Item> getItems() {
        return items;
    }

    public void setInspectables(Map<String, Inspectable> inspectables) {
        if (inspectables != null) {
            this.inspectables = inspectables;
        }
    }

    public Map<String, Inspectable> getInspectables() {
        return inspectables;
    }

    public void addNpc(Npc npc) {
        this.npcs.add(npc);
    }

    public List<Npc> getNpcs() {
        return npcs;
    }

    public Npc getNpcByName(String name) {
        for (Npc npc : npcs) {
            if (npc.getName().equalsIgnoreCase(name) || npc.getId().equalsIgnoreCase(name)) {
                return npc;
            }
        }
        return null;
    }
}