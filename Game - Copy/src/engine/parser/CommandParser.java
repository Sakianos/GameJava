package engine.parser;

import engine.commands.Command;
import engine.commands.CommandRegistry;
import engine.core.GameState;

public class CommandParser {
    private final CommandRegistry registry;
    private final GreekSemanticParser semanticParser;

    public CommandParser(CommandRegistry registry, GreekSemanticParser semanticParser) {
        this.registry = registry;
        this.semanticParser = semanticParser;
    }

    public String parseAndExecute(String input, GameState state) {
        if (input == null || input.trim().isEmpty()) {
            return "Τι θέλεις να κάνεις;";
        }

        String verbId = "";
        String finalArgument = "";

        // 1. Προσπάθεια ανάλυσης μέσω του GreekSemanticParser
        GreekSemanticParser.ParseResult semanticResult = null;
        if (semanticParser != null) {
            semanticResult = semanticParser.parse(input);
        }

        if (semanticResult != null) {
            // ΔΙΟΡΘΩΣΗ: Χρήση του .verb αντί για .command
            // και του .fullArgument για ευκολία
            verbId = semanticResult.verb;
            finalArgument = semanticResult.fullArgument;
        } else {
            // 2. Fallback: Απλό split αν αποτύχει ο semantic parser
            String[] words = input.trim().toLowerCase().split("\\s+");
            verbId = words[0];
            finalArgument = (words.length > 1) ? words[1] : "";
        }

        // 3. Αναζήτηση και εκτέλεση της εντολής
        Command command = registry.getCommand(verbId);

        if (command == null) {
            return "Δεν ξέρω πώς να κάνω '" + verbId + "'.";
        }

        return command.execute(state, finalArgument);
    }
}