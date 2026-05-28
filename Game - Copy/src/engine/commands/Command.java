package engine.commands;

import engine.core.GameState;

public interface Command {
    // Αυτή είναι η μέθοδο που καλεί ο Parser
    String execute(GameState state, String argument);

    String getKeyword();
}