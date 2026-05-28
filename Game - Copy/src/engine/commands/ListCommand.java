package engine.commands;

import engine.core.GameState;
import engine.core.Room;

public class ListCommand implements Command {

    @Override
    public String execute(GameState state, String argument) {
        StringBuilder result = new StringBuilder();
        result.append("\n=== Διαθέσιμες Ενέργειες ===\n");

        Room currentRoom = state.getCurrentRoom();
        result.append("Μπορείς να κάνεις τα εξής:\n\n");

        result.append("ΚΙΝΗΣΗ:\n");
        // Τώρα η getExits() λειτουργεί!
        for (String direction : currentRoom.getExits().keySet()) {
            result.append("  • πήγαινε ").append(direction).append(" (go ").append(direction).append(")\n");
        }

        result.append("\nΑΝΤΙΚΕΙΜΕΝΑ ΣΤΟ ΧΩΡΟ:\n");
        if (currentRoom.getItems().isEmpty()) {
            result.append("  • Δεν υπάρχουν αντικείμενα εδώ\n");
        } else {
            currentRoom.getItems().forEach((id, item) ->
                    result.append("  • πάρε ").append(item.getName()).append(" (grab ").append(id).append(")\n")
            );
        }

        result.append("\nΠΡΟΣΩΠΑ ΣΤΟΝ ΧΩΡΟ:\n");
        if (currentRoom.getNpcs().isEmpty()) {
            result.append("  • Δεν υπάρχουν άτομα εδώ\n");
        } else {
            currentRoom.getNpcs().forEach(npc ->
                    result.append("  • μίλα ").append(npc.getName()).append(" (talk ").append(npc.getName()).append(")\n")
            );
        }

        result.append("\nΕΠΙΣΚΟΠΗΣΗ:\n");
        if (currentRoom.getInspectables().isEmpty()) {
            result.append("  • Δεν υπάρχει κάτι να δεις\n");
        } else {
            currentRoom.getInspectables().forEach((name, inspectable) ->
                    result.append("  • δες ").append(name).append(" (look ").append(name).append(")\n")
            );
        }

        result.append("\nΓΕΝΙΚΕΣ ΕΝΤΟΛΕΣ:\n");
        result.append("  • έλεγχος (check) - Δες την αποθήκη σου\n");
        result.append("  • βοήθεια (help) - Δες διαθέσιμες ενέργειες\n");
        result.append("  • έξοδος (quit) - Φύγε από το παιχνίδι\n");

        return result.toString();
    }

    @Override
    public String getKeyword() {
        return "list";
    }
}