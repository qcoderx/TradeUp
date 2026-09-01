package ng.edu.unilag.tradeup.domain;

/**
 * How much life an item has left. Ordered best-first so the ordinal doubles as a
 * ranking when sorting search results by quality.
 */
public enum Condition {

    NEW("New", "Never used, still sealed or with tags"),
    LIKE_NEW("Like new", "Barely used, no visible wear"),
    GOOD("Good", "Used with light, honest wear"),
    FAIR("Fair", "Clear wear but fully working"),
    WELL_USED("Well used", "Works, but shows its age");

    private final String label;
    private final String description;

    Condition(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
