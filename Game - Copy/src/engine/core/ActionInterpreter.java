package engine.core;

import engine.commands.Command;
import engine.commands.CommandRegistry;
import engine.parser.GreekSemanticParser;

import java.util.List;
import java.util.Map;

/**
 * Ο κεντρικός διερμηνέας εντολών.
 *
 * Pipeline:
 *   raw input
 *     → GreekParser          (φυσική γλώσσα → ParseResult)
 *     → EventExecutor        (ελέγχει events.json για match)
 *     → CommandRegistry      (fallback στα Command objects)
 *
 * Δεν περιέχει καμία game-specific λογική.
 */
public class ActionInterpreter {

    private final GreekSemanticParser parser;
    private final ActionRegistry actionRegistry;
    private final EventExecutor eventExecutor;
    private final CommandRegistry commandRegistry;

    public ActionInterpreter(ActionRegistry actionRegistry,
                             EventExecutor eventExecutor,
                             CommandRegistry commandRegistry,
                             String parserJsonPath) throws java.io.IOException {
        this.actionRegistry  = actionRegistry;
        this.eventExecutor   = eventExecutor;
        this.commandRegistry = commandRegistry;

        // Parser παίρνει aliases από ActionRegistry + linguistics από parser.json
        Map<String, String> aliases = actionRegistry.getAliasToActionMap();
        this.parser = new GreekSemanticParser(aliases, parserJsonPath);
    }

    // ── Κύρια μέθοδος ────────────────────────────────────────────────────────

    public String interpret(String rawInput, GameState state) {
        if (rawInput == null || rawInput.isBlank()) {
            return "Τι θέλεις να κάνεις;";
        }

        // ── STEP 1: Parse ────────────────────────────────────────────────────
        GreekSemanticParser.ParseResult pr = parser.parse(rawInput);

        if (pr == null) {
            return unknownCommand(rawInput);
        }

        String verbId   = pr.verb;
        String noun     = pr.noun;     // κύριο αντικείμενο
        String prepType = pr.prepType; // "on", "with", ή null
        String target   = pr.target;  // δευτερεύον αντικείμενο

        // ── STEP 2: Έλεγξε events.json ──────────────────────────────────────
        // Για talk: targetNpc = noun
        // Για use:  item = noun, target = target
        // Για move: direction = noun
        String eventNpc  = "talk".equals(verbId) ? noun : "";
        String eventItem = noun;

        EventExecutor.EventResult eventResult =
                eventExecutor.executeEvent(verbId, noun, eventNpc, state);

        // Αν δεν βρέθηκε event με noun, δοκίμασε με "noun_target"
        if (eventResult == null && target != null) {
            eventResult = eventExecutor.executeEvent(verbId, noun, target, state);
        }

        if (eventResult != null) {
            if (eventResult.consequences != null && !eventResult.consequences.isEmpty()) {
                eventExecutor.executeConsequences(eventResult.consequences, state);
            }
            return eventResult.response;
        }

        // ── STEP 3: Fallback στα Command objects ────────────────────────────
        Command cmd = commandRegistry.getCommand(verbId);
        if (cmd != null) {
            // Φτιάχνουμε το argument ανάλογα με το verb
            String argument = buildArgument(verbId, noun, target);
            return cmd.execute(state, argument);
        }

        // ── STEP 4: Built-in handlers για κινήσεις κλπ ──────────────────────
        return handleBuiltin(verbId, noun, target, state);
    }

    // ── Built-in handlers ────────────────────────────────────────────────────

    private String handleBuiltin(String verbId, String noun, String target, GameState state) {
        return switch (verbId) {
            case "move"    -> handleMove(noun, state);
            case "look",
                 "examine" -> handleLook(noun, state);
            case "grab"    -> handleGrab(noun, state);
            case "check"   -> handleCheck(state);
            case "list"    -> handleList(state);
            default        -> "Δεν μπορείς να κάνεις αυτό εδώ.";
        };
    }

    private String handleMove(String direction, GameState state) {
        if (direction == null || direction.isBlank()) {
            return "Πού θέλεις να πας;";
        }
        String nextRoomId = state.getCurrentRoom().getExit(direction.toLowerCase());
        if (nextRoomId == null) {
            return "Δεν μπορείς να πας " + direction + " από εδώ.";
        }
        state.setCurrentRoom(nextRoomId);
        Room next = state.getCurrentRoom();
        return "\nΠηγαίνεις " + direction + "...\n" +
                "══════════════════════════════\n" +
                "  " + next.getName() + "\n" +
                "══════════════════════════════\n" +
                next.getDescription();
    }

    private String handleLook(String target, GameState state) {
        if (target == null || target.isBlank()) {
            return state.getCurrentRoom().getDescription();
        }
        return state.getCurrentRoom().getDynamicInspectableDescription(target);
    }

    private String handleGrab(String itemId, GameState state) {
        if (itemId == null || itemId.isBlank()) {
            return "Τι θέλεις να πάρεις;";
        }
        Item item = state.getCurrentRoom().removeItem(itemId);
        if (item == null) {
            return "Δεν υπάρχει '" + itemId + "' εδώ.";
        }
        state.addToInventory(item);
        return item.getPickupMessage();
    }

    private String handleCheck(GameState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════╗\n");
        sb.append(  "║   ΑΝΤΙΚΕΙΜΕΝΑ ΣΤΟ ΣΑΚΟ  ║\n");
        sb.append(  "╚══════════════════════════╝\n");
        if (state.getInventory().isEmpty()) {
            sb.append("Ο σάκος σου είναι άδειος.\n");
        } else {
            for (Item item : state.getInventory()) {
                sb.append("  • ").append(item.getName())
                        .append(": ").append(item.getDescription()).append("\n");
            }
        }
        sb.append("\nΝομίσματα: ").append(state.getCoins()).append("\n");
        return sb.toString();
    }

    private String handleList(GameState state) {
        Room room = state.getCurrentRoom();
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════╗\n");
        sb.append(  "║   ΔΙΑΘΕΣΙΜΕΣ ΕΝΕΡΓΕΙΕΣ  ║\n");
        sb.append(  "╚══════════════════════════╝\n");

        sb.append("\nΚΙΝΗΣΗ:\n");
        room.getExits().forEach((dir, id) ->
                sb.append("  • πήγαινε ").append(dir).append("\n"));

        sb.append("\nΑΝΤΙΚΕΙΜΕΝΑ:\n");
        if (room.getItems().isEmpty()) {
            sb.append("  (κανένα)\n");
        } else {
            room.getItems().forEach((id, item) ->
                    sb.append("  • πάρε ").append(item.getName()).append("\n"));
        }

        sb.append("\nΠΡΟΣΩΠΑ:\n");
        if (room.getNpcs().isEmpty()) {
            sb.append("  (κανένας)\n");
        } else {
            room.getNpcs().forEach(npc ->
                    sb.append("  • μίλα ").append(npc.getName()).append("\n"));
        }

        sb.append("\nΓΕΝΙΚΑ: check | list | quit\n");
        return sb.toString();
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    /**
     * Φτιάχνει το argument string που θα περαστεί στο Command.execute().
     * Για use: "royal_poison wine_vat"
     * Για talk: "king"
     * Για move: "north"
     */
    private String buildArgument(String verbId, String noun, String target) {
        if (target != null) {
            return noun + " " + target;
        }
        return noun;
    }

    private String unknownCommand(String input) {
        return "Δεν καταλαβαίνω '" + input.trim() + "'.\n" +
                "Γράψε 'help' για λίστα εντολών.";
    }
}