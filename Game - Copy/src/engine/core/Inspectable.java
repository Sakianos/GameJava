package engine.core;

public class Inspectable {
    private String description;

    /**
     * Η ελληνική λέξη που αντιστοιχεί στο id αυτού του inspectable.
     * Χρησιμοποιείται από τον HintRenderer για να βρει πού να βάλει το hint.
     *
     * Παράδειγμα στο map.json:
     *   "table": { "keyword": "τραπέζι", "description": "..." }
     *
     * Αν δεν οριστεί, ο HintRenderer χρησιμοποιεί το id (π.χ. "table").
     */
    private String keyword;

    public String getDescription() { return description; }
    public String getKeyword()     { return keyword; }
}