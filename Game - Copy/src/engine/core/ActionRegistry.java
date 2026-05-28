package engine.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Φορτώνει τις actions από το actions.json και κρατά
 * τη χαρτογράφηση alias → actionId.
 *
 * Χρησιμοποιείται από:
 *   - ActionInterpreter (για να δώσει aliases στον GreekParser)
 *   - EventExecutor (για να βρει αν υπάρχει event για action)
 */
public class ActionRegistry {

    private final Map<String, String> aliasToAction = new LinkedHashMap<>();
    private final Map<String, ActionDef> actions    = new LinkedHashMap<>();

    public static class ActionDef {
        public String       id;
        public List<String> aliases;
        public String       description;
    }

    public ActionRegistry(String actionsJsonPath) throws IOException {
        loadActions(actionsJsonPath);
    }

    private void loadActions(String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        Gson gson = new Gson();
        List<ActionDef> list = gson.fromJson(content,
                new TypeToken<List<ActionDef>>(){}.getType());

        for (ActionDef action : list) {
            actions.put(action.id, action);
            for (String alias : action.aliases) {
                aliasToAction.put(alias.toLowerCase().trim(), action.id);
            }
        }
    }

    /** Επιστρέφει το canonical action id για ένα alias. */
    public String getActionId(String alias) {
        return aliasToAction.get(alias.toLowerCase().trim());
    }

    public ActionDef getAction(String actionId) {
        return actions.get(actionId);
    }

    public boolean hasAction(String alias) {
        return aliasToAction.containsKey(alias.toLowerCase().trim());
    }

    /**
     * Επιστρέφει ολόκληρο τον alias map — χρησιμοποιείται
     * από τον GreekParser για να χτίσει τα verb patterns.
     */
    public Map<String, String> getAliasToActionMap() {
        return Collections.unmodifiableMap(aliasToAction);
    }
}
