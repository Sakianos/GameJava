package engine.core;

import java.util.List;

public class Npc {

    private String id;
    private String name;
    private List<DialogueLine> dialogue;

    private transient int     currentLineIndex = 0;
    private transient boolean finished         = false;

    public static class DialogueLine {
        public String  line;
        public String  gives_item;
        public boolean closes;
    }

    public static class TalkResult {
        public String  text;
        public boolean finished;
    }

    public TalkResult nextLine(GameState state) {
        TalkResult result = new TalkResult();

        if (finished || dialogue == null || dialogue.isEmpty()) {
            result.text     = name + " δεν έχει άλλα να σου πει.";
            result.finished = true;
            return result;
        }

        DialogueLine current = dialogue.get(currentLineIndex);
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": \"").append(current.line).append("\"");

        if (current.gives_item != null && !current.gives_item.isBlank()) {
            Item item = state.getCurrentRoom().removeItem(current.gives_item);
            if (item == null) {
                item = new Item(current.gives_item, current.gives_item,
                        "Δώρο από " + name, "Πήρες το αντικείμενο.");
            }
            state.addToInventory(item);
            sb.append("\n  → Έλαβες: ").append(item.getName());
        }

        currentLineIndex++;

        boolean isLast = currentLineIndex >= dialogue.size();
        if (current.closes || isLast) {
            finished        = true;
            result.finished = true;
            sb.append("\n  [ Ο ").append(name).append(" γυρίζει την πλάτη του. ]");
        } else {
            int remaining = dialogue.size() - currentLineIndex;
            sb.append("\n  [ Συνέχισε: talk ").append(id)
                    .append(" | Απομένουν ").append(remaining).append(" γραμμές ]");
        }

        result.text = sb.toString();
        return result;
    }

    public void resetDialogue() {
        currentLineIndex = 0;
        finished         = false;
    }

    public String  getId()      { return id; }
    public String  getName()    { return name; }
    public boolean isFinished() { return finished; }

    public String getDialogue() {
        if (dialogue == null || dialogue.isEmpty()) return "";
        return dialogue.get(0).line;
    }
}