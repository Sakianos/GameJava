package engine.commands;

import engine.core.GameState;
import engine.core.Room;

public class LookCommand implements Command {

    @Override
    public String getKeyword() {
        return "look";
    }

    @Override
    public String execute(GameState state, String argument) {
        Room currentRoom = state.getCurrentRoom();

        // 1. Περίπτωση: Σκέτο "look" -> Εμφάνιση περιγραφής δωματίου
        if (argument == null || argument.trim().isEmpty()) {
            return currentRoom.getDescription();
        }

        // 2. Περίπτωση: "look [κάτι]" (π.χ. look table)
        String target = argument.toLowerCase().trim();

        /* Αντί να παίρνουμε το description απευθείας από το Map,
           καλούμε τη δυναμική μέθοδο της Room που ελέγχει τα placeholders {}.
        */
        return currentRoom.getDynamicInspectableDescription(target);
    }
}