package engine.parser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

/**
 * Zork-style Greek parser με intent pattern matching.
 *
 * Δύο στρώματα κατανόησης:
 *
 *  1. VERB PARSING (γρήγορο):
 *     "πάρε το σπαθί" → grab + sword
 *     "use key on door" → use + key + on + door
 *
 *  2. INTENT PATTERNS (φυσική γλώσσα):
 *     "τι έχει πάνω στο τραπέζι" → look + table
 *     "ακούω φασαρία έξω"        → look + window
 *     "τι έχεις εσύ"             → talk + {npc in context}
 *
 * ΔΕΝ περιέχει hardcoded γλωσσολογικά δεδομένα.
 * Όλα φορτώνονται από parser.json.
 */
public class GreekSemanticParser {

    // ── Config ───────────────────────────────────────────────────────────────
    private Set<String>              articles;
    private Set<String>              noiseWords;
    private Map<String, String>      prepWordToType;   // "στο" → "on"
    private Map<String, Set<String>> prepositions;
    private Map<String, String>      directions;       // "βόρεια" → "north"
    private Map<String, String>      verbBehaviors;    // "move" → "direction"
    private List<IntentPattern>      intentPatterns;

    // Verb aliases από ActionRegistry
    private final Map<String, String> verbAliases;

    // ── IntentPattern ────────────────────────────────────────────────────────

    private static class IntentPattern {
        String      description;
        Pattern     compiled;
        int         captureGroup;
        String      mapsToVerb;
        String      mapsToTarget;  // μπορεί να είναι "{capture}", "{capture_or_npc}", "{capture_or_room}"

        static IntentPattern from(Map<String, Object> raw) {
            IntentPattern ip  = new IntentPattern();
            ip.description    = (String) raw.get("description");
            ip.compiled       = Pattern.compile((String) raw.get("pattern"),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            ip.captureGroup   = ((Number) raw.get("capture_group")).intValue();
            Map<?, ?> mapsTo  = (Map<?, ?>) raw.get("maps_to");
            ip.mapsToVerb     = (String) mapsTo.get("verb");
            ip.mapsToTarget   = (String) mapsTo.get("target");
            return ip;
        }
    }

    // ── ParseResult ──────────────────────────────────────────────────────────

    public static class ParseResult {
        public final String verb;
        public final String noun;
        public final String prepType;
        public final String target;
        public final String fullArgument;
        /** true αν βρέθηκε μέσω intent pattern (φυσική γλώσσα) */
        public final boolean fromIntent;

        public ParseResult(String verb, String noun, String prepType,
                           String target, boolean fromIntent) {
            this.verb         = verb;
            this.noun         = noun != null ? noun : "";
            this.prepType     = prepType;
            this.target       = target;
            this.fromIntent   = fromIntent;
            this.fullArgument = (target != null) ? this.noun + " " + target : this.noun;
        }

        @Override
        public String toString() {
            return String.format("ParseResult{verb='%s', noun='%s', prep='%s', target='%s', intent=%b}",
                    verb, noun, prepType, target, fromIntent);
        }
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public GreekSemanticParser(Map<String, String> verbAliases, String parserJsonPath) throws IOException {
        this.verbAliases = verbAliases;
        loadConfig(parserJsonPath);
    }

    // ── Config loading ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadConfig(String path) throws IOException {
        String content  = new String(Files.readAllBytes(Paths.get(path)));
        Gson   gson     = new Gson();
        Map<String, Object> root = gson.fromJson(content,
                new TypeToken<Map<String, Object>>(){}.getType());

        articles   = new HashSet<>((List<String>) root.get("articles"));
        noiseWords = new HashSet<>((List<String>) root.get("noise_words"));

        Map<String, List<String>> rawPreps = (Map<String, List<String>>) root.get("prepositions");
        prepositions   = new HashMap<>();
        prepWordToType = new HashMap<>();
        for (var e : rawPreps.entrySet()) {
            Set<String> words = new HashSet<>(e.getValue());
            prepositions.put(e.getKey(), words);
            for (String w : words) prepWordToType.put(w.toLowerCase(), e.getKey());
        }

        directions    = new HashMap<>();
        ((Map<String, String>) root.get("directions"))
                .forEach((k, v) -> directions.put(k.toLowerCase(), v));

        verbBehaviors = new HashMap<>();
        ((Map<String, String>) root.get("verb_behaviors"))
                .forEach((k, v) -> verbBehaviors.put(k.toLowerCase(), v));

        // Intent patterns
        intentPatterns = new ArrayList<>();
        List<Map<String, Object>> rawPatterns =
                (List<Map<String, Object>>) root.get("intent_patterns");
        if (rawPatterns != null) {
            for (var raw : rawPatterns) {
                try {
                    intentPatterns.add(IntentPattern.from(raw));
                } catch (Exception e) {
                    System.err.println("Σφάλμα φόρτωσης pattern: " + raw.get("description"));
                }
            }
        }
    }

    // ── Κύρια μέθοδος parse() ────────────────────────────────────────────────

    /**
     * @param rawInput   Η εντολή του παίκτη
     * @param contextNpc Το NPC που είναι στο τρέχον δωμάτιο (για {capture_or_npc})
     */
    public ParseResult parse(String rawInput, String contextNpc) {
        if (rawInput == null || rawInput.isBlank()) return null;

        String input = rawInput.trim().toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[;,.!?]", "");

        // ── Στρώμα 1: Verb parsing ───────────────────────────────────────────
        ParseResult verbResult = tryVerbParse(input);
        if (verbResult != null) return verbResult;

        // ── Στρώμα 2: Intent patterns ────────────────────────────────────────
        return tryIntentParse(input, contextNpc);
    }

    /** Overload χωρίς context NPC */
    public ParseResult parse(String rawInput) {
        return parse(rawInput, null);
    }

    // ── Στρώμα 1: Verb parsing ───────────────────────────────────────────────

    private ParseResult tryVerbParse(String input) {
        String[] tokens = input.split(" ");
        if (tokens.length == 0) return null;

        String verbId  = null;
        int    verbLen = 0;

        verbId = verbAliases.get(tokens[0]);
        if (verbId != null) {
            verbLen = 1;
        } else if (tokens.length >= 2) {
            verbId = verbAliases.get(tokens[0] + " " + tokens[1]);
            if (verbId != null) verbLen = 2;
        }

        if (verbId == null) return null;

        List<String> rest = new ArrayList<>();
        for (int i = verbLen; i < tokens.length; i++) {
            if (!noiseWords.contains(tokens[i])) rest.add(tokens[i]);
        }

        String behavior = verbBehaviors.getOrDefault(verbId, "noun_on_target");

        ParseResult r = switch (behavior) {
            case "direction"         -> parseDirection(verbId, rest);
            case "noun_strip_preps"  -> parseStripPreps(verbId, rest);
            case "noun_optional"     -> parseNounOptional(verbId, rest);
            case "empty"             -> new ParseResult(verbId, "", null, null, false);
            default                  -> parseNounOnTarget(verbId, rest);
        };

        return r;
    }

    // ── Στρώμα 2: Intent patterns ────────────────────────────────────────────

    private ParseResult tryIntentParse(String input, String contextNpc) {
        for (IntentPattern ip : intentPatterns) {
            Matcher m = ip.compiled.matcher(input);
            if (!m.find()) continue;

            // Εξαγωγή captured group
            String captured = null;
            if (ip.captureGroup > 0 && ip.captureGroup <= m.groupCount()) {
                captured = m.group(ip.captureGroup);
            }

            // Επίλυση target template
            String target = resolveTarget(ip.mapsToTarget, captured, contextNpc);

            if (target == null || target.isBlank()) continue;

            return new ParseResult(ip.mapsToVerb, target, null, null, true);
        }
        return null;
    }

    private String resolveTarget(String template, String captured, String contextNpc) {
        return switch (template) {
            case "{capture}" ->
                    captured != null ? captured : null;
            case "{capture_or_npc}" ->
                    (captured != null && !captured.isBlank()) ? captured : contextNpc;
            case "{capture_or_room}" ->
                    (captured != null && !captured.isBlank()) ? captured : "";
            default ->
                // Literal target (π.χ. "window")
                    template;
        };
    }

    // ── Behavior parsers ─────────────────────────────────────────────────────

    private ParseResult parseDirection(String verb, List<String> tokens) {
        Set<String> toPreps = prepositions.getOrDefault("to", Collections.emptySet());
        for (String t : tokens) {
            if (toPreps.contains(t) || articles.contains(t)) continue;
            String canonical = directions.get(t);
            return new ParseResult(verb,
                    canonical != null ? canonical : t,
                    null, null, false);
        }
        return new ParseResult(verb, "", null, null, false);
    }

    private ParseResult parseStripPreps(String verb, List<String> tokens) {
        List<String> filtered = new ArrayList<>();
        for (String t : tokens) {
            if (!articles.contains(t) && !prepWordToType.containsKey(t))
                filtered.add(t);
        }
        return new ParseResult(verb, String.join(" ", filtered), null, null, false);
    }

    private ParseResult parseNounOptional(String verb, List<String> tokens) {
        List<String> f = filterArticles(tokens);
        return new ParseResult(verb,
                f.isEmpty() ? "" : String.join("_", f),
                null, null, false);
    }

    private ParseResult parseNounOnTarget(String verb, List<String> tokens) {
        if (tokens.isEmpty()) return new ParseResult(verb, "", null, null, false);

        List<String> nounParts   = new ArrayList<>();
        String       prepType    = null;
        List<String> targetParts = new ArrayList<>();
        boolean      afterPrep   = false;

        for (String t : tokens) {
            if (!afterPrep) {
                String pt = prepWordToType.get(t);
                if (pt != null && !pt.equals("to")) {
                    prepType  = pt;
                    afterPrep = true;
                } else if (!articles.contains(t)) {
                    nounParts.add(t);
                }
            } else {
                if (!articles.contains(t)) targetParts.add(t);
            }
        }

        return new ParseResult(verb,
                String.join("_", nounParts),
                prepType,
                targetParts.isEmpty() ? null : String.join("_", targetParts),
                false);
    }

    private List<String> filterArticles(List<String> tokens) {
        List<String> r = new ArrayList<>();
        for (String t : tokens) if (!articles.contains(t)) r.add(t);
        return r;
    }
}