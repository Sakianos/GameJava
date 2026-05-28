package engine.parser;

import engine.commands.Command;
import engine.commands.CommandRegistry;
import engine.core.EventExecutor;
import engine.core.GameState;
import engine.core.Npc;

public class CommandParser {
    private final CommandRegistry registry;
    private final GreekSemanticParser semanticParser;
    private final EventExecutor eventExecutor;

    public CommandParser(CommandRegistry registry, GreekSemanticParser semanticParser) {
        this(registry, semanticParser, null);
    }

    public CommandParser(CommandRegistry registry, GreekSemanticParser semanticParser, EventExecutor eventExecutor) {
        this.registry = registry;
        this.semanticParser = semanticParser;
        this.eventExecutor = eventExecutor;
    }

    public String parseAndExecute(String input, GameState state) {
        if (input == null || input.trim().isEmpty()) {
            return "Τι θέλεις να κάνεις;";
        }

        String rawNormalized = input.trim().toLowerCase();
        String verbId = "";
        String noun = "";
        String target = null;
        GreekSemanticParser.ParseResult pr = null;

        if (semanticParser != null) {
            pr = semanticParser.parse(input);
        }

        if (pr != null) {
            verbId = pr.verb;
            noun = pr.noun != null ? pr.noun : "";
            target = pr.target;
        } else {
            String[] words = rawNormalized.split("\\s+");
            verbId = words[0];
            noun = words.length > 1 ? words[1] : "";
        }

        // Normalize to canonical command keyword (e.g. "take" → "grab", "go" → "go")
        Command cmd = registry.getCommand(verbId);
        String canonicalVerb = (cmd != null) ? cmd.getKeyword() : verbId;

        // Check events first
        if (eventExecutor != null) {
            // For talk: resolve typed NPC name/id to canonical NPC id for event matching
            String eventNpc = "";
            if ("talk".equals(canonicalVerb) && !noun.isEmpty()) {
                Npc npc = state.getCurrentRoom().getNpcByName(noun);
                eventNpc = (npc != null) ? npc.getId() : noun;
            }

            // Primary event check (verb + noun/npc)
            EventExecutor.EventResult eventResult =
                    eventExecutor.executeEvent(canonicalVerb, noun, eventNpc, state);

            // Secondary: use <item> on <target>
            if (eventResult == null && target != null) {
                eventResult = eventExecutor.executeEvent(canonicalVerb, noun, target, state);
            }

            // Custom events (story choices like "loyaltypath", "freedompath")
            if (eventResult == null && cmd == null) {
                eventResult = eventExecutor.executeEvent("custom", rawNormalized, "", state);
            }

            if (eventResult != null) {
                if (eventResult.consequences != null && !eventResult.consequences.isEmpty()) {
                    eventExecutor.executeConsequences(eventResult.consequences, state);
                }
                return eventResult.response;
            }
        }

        // Fallback to command
        if (cmd == null) {
            return "Δεν ξέρω πώς να κάνω '" + verbId + "'.";
        }

        String argument = (pr != null) ? pr.fullArgument : noun;
        return cmd.execute(state, argument);
    }
}
