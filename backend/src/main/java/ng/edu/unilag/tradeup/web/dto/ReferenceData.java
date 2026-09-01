package ng.edu.unilag.tradeup.web.dto;

import java.util.List;

/**
 * Everything the frontend needs to build its filters and dropdowns, served once
 * so the labels never drift between the two sides of the app.
 */
public record ReferenceData(
        List<CategoryOption> categories, List<Option> conditions, List<Option> intents) {

    /** A category, with how many items are currently available in it. */
    public record CategoryOption(String name, String label, String slug, long availableCount) {}

    /** A plain enum choice with its display wording. */
    public record Option(String name, String label, String description) {}
}
