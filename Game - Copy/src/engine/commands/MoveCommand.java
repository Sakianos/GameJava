package engine.commands;

import engine.core.GameState;
import engine.core.Room;
import engine.core.SaveManager;

public class MoveCommand implements Command {

    @Override
    public String getKeyword() {
        return "go";
    }

    @Override
    public String execute(GameState state, String direction) {
        // 1. Παίρνουμε το τρέχον δωμάτιο
        Room currentRoom = state.getCurrentRoom();

        // 2. Ελέγχουμε αν υπάρχει έξοδος προς αυτή την κατεύθυνση (π.χ. "out")
        String nextRoomId = currentRoom.getExit(direction);

        if (nextRoomId == null) {
            return "Δεν μπορείς να πας από εκεί!";
        }

        // 3. Ενημερώνουμε το GameState για το νέο δωμάτιο
        state.setCurrentRoom(nextRoomId);
        SaveManager.save(state);

        // 4. Παίρνουμε το νέο δωμάτιο για να δείξουμε την περιγραφή του
        Room nextRoom = state.getCurrentRoom();

        return "\nΠηγαίνεις " + direction + "...\n" +
                "--- " + nextRoom.getName() + " ---\n" +
                nextRoom.getDescription();
    }
}