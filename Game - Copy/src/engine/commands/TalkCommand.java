package engine.commands;

import engine.core.GameState;
import engine.core.Npc;

public class TalkCommand implements Command {

    @Override
    public String getKeyword() { return "talk"; }

    @Override
    public String execute(GameState state, String argument) {
        if (argument == null || argument.isBlank()) {
            return "Με ποιον θέλεις να μιλήσεις;";
        }

        Npc npc = state.getCurrentRoom().getNpcByName(argument.trim());

        if (npc == null) {
            return "Δεν υπάρχει κανένας με το όνομα '" + argument.trim() + "' εδώ.";
        }

        return npc.nextLine(state).text;
    }
}