package engine.commands;

import engine.core.CommandConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {
    private Map<String, Command> commands = new HashMap<>();

    // Constructor που δέχεται τη λίστα από το JSON
    public CommandRegistry(List<CommandConfig> configs) {
        loadCommands(configs);
    }

    private void loadCommands(List<CommandConfig> configs) {
        for (CommandConfig config : configs) {
            try {
                // Δυναμική δημιουργία του Command object από το όνομα της κλάσης στο JSON
                // Προσοχή: Το "engine.commands." πρέπει να είναι το σωστό package
                String fullClassName = "engine.commands." + config.getClassName();
                Class<?> clazz = Class.forName(fullClassName);
                Command cmd = (Command) clazz.getDeclaredConstructor().newInstance();

                // Καταχώρηση όλων των συνωνύμων (aliases)
                register(cmd, config.getAliases());

            } catch (Exception e) {
                System.err.println("Σφάλμα κατά τη φόρτωση της εντολής: " + config.getClassName());
                e.printStackTrace();
            }
        }
    }

    public void register(Command cmd, List<String> aliases) {
        for (String alias : aliases) {
            commands.put(alias.toLowerCase().trim(), cmd);
        }
    }

    public Command getCommand(String verb) {
        return commands.get(verb.toLowerCase().trim());
    }
}