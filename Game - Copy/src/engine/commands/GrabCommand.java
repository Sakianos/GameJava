package engine.commands;

import engine.core.GameState;
import engine.core.Item;
import engine.core.Room;
import engine.core.SaveManager;

public class GrabCommand implements Command {

    @Override
    public String getKeyword() {
        return "grab";
    }

    @Override
    public String execute(GameState state, String argument) {
        if (state.isInventoryFull()) {
            return "Το inventory σου είναι γεμάτο! (" + state.getMaxInventorySize() + "/" + state.getMaxInventorySize() + ")\nΆφησε κάτι κάτω πριν πάρεις νέο αντικείμενο.";
        }

        Room current = state.getCurrentRoom();
        Item item = current.removeItem(argument);

        if (item == null) return "Δεν υπάρχει αυτό εδώ.";

        state.addToInventory(item);
        SaveManager.save(state);

        return item.getPickupMessage();
    }
}