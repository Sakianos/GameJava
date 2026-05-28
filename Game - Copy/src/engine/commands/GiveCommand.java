package engine.commands;

import engine.core.GameState;

/**
 * Generic GiveCommand — δεν ξέρει τίποτα για συγκεκριμένα παιχνίδια.
 *
 * Format: give <amount> [coins] ή give <item> [to <npc>]
 *
 * Όλη η λογική (τι γίνεται όταν δίνεις χρήματα σε φρουρό κλπ)
 * ορίζεται στο events.json.
 */
public class GiveCommand implements Command {

    @Override
    public String getKeyword() { return "give"; }

    @Override
    public String execute(GameState state, String argument) {
        if (argument == null || argument.isBlank()) {
            return "Τι θέλεις να δώσεις; (π.χ. give 50 coins)";
        }

        String[] parts = argument.trim().split("\\s+");

        // Περίπτωση: give <αριθμός> [coins]
        try {
            int amount = Integer.parseInt(parts[0]);
            if (state.getCoins() < amount) {
                return "Δεν έχεις αρκετά νομίσματα! (Έχεις: " + state.getCoins() + ")";
            }
            // Αν φτάσαμε εδώ χωρίς event να πιάσει → δεν υπάρχει κανείς να δεχτεί
            return "Δεν υπάρχει κανείς εδώ να δεχτεί τα νομίσματα.";
        } catch (NumberFormatException ignored) {
            // Δεν είναι αριθμός → προσπαθεί να δώσει item
        }

        // Περίπτωση: give <item> [to <npc>]
        String itemId = parts[0].toLowerCase();
        if (!state.hasItem(itemId)) {
            return "Δεν έχεις '" + parts[0] + "' για να δώσεις.";
        }

        return "Δεν υπάρχει κανείς εδώ να δεχτεί το " + parts[0] + ".";
    }
}