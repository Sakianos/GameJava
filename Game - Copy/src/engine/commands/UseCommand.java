package engine.commands;

import engine.core.GameState;

/**
 * Generic UseCommand — δεν ξέρει τίποτα για συγκεκριμένα παιχνίδια.
 *
 * Format: use <item> [on/with <target>]
 * Παραδείγματα:
 *   use key
 *   use key on door
 *   use royal_poison on wine_vat
 *
 * Όλη η ειδική λογική (τι γίνεται όταν χρησιμοποιείς X σε Y)
 * ορίζεται στο events.json.
 *
 * Αν δεν υπάρχει event για αυτόν τον συνδυασμό,
 * επιστρέφουμε generic μήνυμα.
 */
public class UseCommand implements Command {

    @Override
    public String getKeyword() { return "use"; }

    @Override
    public String execute(GameState state, String argument) {
        if (argument == null || argument.isBlank()) {
            return "Τι θέλεις να χρησιμοποιήσεις;";
        }

        // Το argument έχει ήδη παρσαριστεί από τον GreekParser:
        // μπορεί να είναι "key" ή "key_door" (noun_target)
        String[] parts = argument.trim().split("\\s+", 2);
        String itemId = parts[0].toLowerCase();

        if (!state.hasItem(itemId)) {
            return "Δεν έχεις '" + parts[0] + "' στο σάκο σου.";
        }

        // Αν φτάσαμε εδώ, δεν υπήρξε event που να ταίριαζε.
        // (Ο ActionInterpreter ελέγχει events ΠΡΙΝ καλέσει αυτή τη μέθοδο)
        String target = parts.length > 1 ? parts[1] : null;
        if (target != null) {
            return "Δεν μπορείς να χρησιμοποιήσεις το " + parts[0] + " εκεί.";
        }
        return "Δεν φαίνεται να μπορείς να χρησιμοποιήσεις το " + parts[0] + " έτσι.";
    }
}