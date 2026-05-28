package engine.commands;

import engine.core.GameState;
import engine.core.Item;

import java.util.Collection;

public class CheckCommand implements Command {

    @Override
    public String execute(GameState state, String argument) {
        StringBuilder result = new StringBuilder();
        result.append("\n=== Η Αποθήκευσή Σου ===\n");

        Collection<Item> inventory = state.getInventory();

        if (inventory.isEmpty()) {
            result.append("Η αποθήκη σου είναι άδεια.\n");
        } else {
            for (Item item : inventory) {
                result.append("• ").append(item.getName()).append(": ").append(item.getDescription()).append("\n");
            }
        }

        result.append("\nΧρήματα: ").append(state.getCoins()).append(" νομίσματα\n");
        return result.toString();
    }

    @Override
    public String getKeyword() {
        return "check";
    }
}
