package ng.edu.unilag.tradeup.domain;

/** Lifecycle of a listing, from draft through to a completed handover. */
public enum ListingStatus {

    DRAFT("Draft"),
    ACTIVE("Available"),
    RESERVED("Reserved"),
    COMPLETED("Traded"),
    REMOVED("Removed"),
    FLAGGED("Under review");

    private final String label;

    ListingStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Only these appear in public browse results. */
    public boolean isPubliclyVisible() {
        return this == ACTIVE || this == RESERVED;
    }

    /** A completed or removed listing can no longer be negotiated over. */
    public boolean isTerminal() {
        return this == COMPLETED || this == REMOVED;
    }
}
