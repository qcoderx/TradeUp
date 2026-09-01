package ng.edu.unilag.tradeup.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ng.edu.unilag.tradeup.web.error.InvalidTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The listing lifecycle, tested directly on the entity.
 *
 * <p>These rules live on {@link Listing} rather than in a service, so they can
 * be checked without a database, a Spring context, or a web request.
 */
class ListingTest {

    private static User student(String name) {
        return new User(name + "@live.unilag.edu.ng", "24081" + name.length() + "017", name, "hash", "Data Science");
    }

    private static Listing listing(Category category, Condition condition) {
        return new Listing(
                "TU-TEST1",
                student("Ada Student"),
                "A used thing",
                "Something a student has finished with.",
                category,
                condition,
                TradeIntent.SELL);
    }

    @Nested
    @DisplayName("status transitions")
    class Transitions {

        @Test
        @DisplayName("a new listing starts available")
        void startsActive() {
            assertThat(listing(Category.TEXTBOOKS, Condition.GOOD).getStatus()).isEqualTo(ListingStatus.ACTIVE);
        }

        @Test
        @DisplayName("an available listing can be held and released")
        void reserveAndRelease() {
            Listing item = listing(Category.TEXTBOOKS, Condition.GOOD);

            item.reserve();
            assertThat(item.getStatus()).isEqualTo(ListingStatus.RESERVED);

            item.release();
            assertThat(item.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        }

        @Test
        @DisplayName("a listing cannot be held twice")
        void cannotReserveTwice() {
            Listing item = listing(Category.TEXTBOOKS, Condition.GOOD);
            item.reserve();

            assertThatThrownBy(item::reserve)
                    .isInstanceOf(InvalidTransitionException.class)
                    .hasMessageContaining("reserved");
        }

        @Test
        @DisplayName("a completed listing cannot be completed again")
        void cannotCompleteTwice() {
            Listing item = listing(Category.ELECTRONICS, Condition.GOOD);
            item.complete();

            assertThatThrownBy(item::complete).isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("a removed listing cannot be removed again")
        void cannotRemoveTwice() {
            Listing item = listing(Category.FURNITURE, Condition.FAIR);
            item.remove();

            assertThatThrownBy(item::remove)
                    .isInstanceOf(InvalidTransitionException.class)
                    .hasMessageContaining("already been closed");
        }

        @Test
        @DisplayName("a flagged listing can be put back by a moderator")
        void flagThenReinstate() {
            Listing item = listing(Category.OTHER, Condition.GOOD);

            item.flag();
            assertThat(item.getStatus()).isEqualTo(ListingStatus.FLAGGED);

            item.reinstate();
            assertThat(item.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("provenance")
    class Provenance {

        @Test
        @DisplayName("completing a trade advances the owner generation")
        void completingAdvancesGeneration() {
            Listing item = listing(Category.TEXTBOOKS, Condition.GOOD);
            assertThat(item.getOwnerGeneration()).isEqualTo(1);

            item.complete();

            assertThat(item.getOwnerGeneration()).isEqualTo(2);
            assertThat(item.getStatus()).isEqualTo(ListingStatus.COMPLETED);
        }

        @Test
        @DisplayName("completing a trade credits the owner")
        void completingCreditsOwner() {
            Listing item = listing(Category.TEXTBOOKS, Condition.GOOD);
            assertThat(item.getOwner().getCompletedTrades()).isZero();

            item.complete();

            assertThat(item.getOwner().getCompletedTrades()).isEqualTo(1);
        }

        @Test
        @DisplayName("a held item can still be handed over")
        void reservedCanComplete() {
            Listing item = listing(Category.TEXTBOOKS, Condition.GOOD);
            item.reserve();

            item.complete();

            assertThat(item.getStatus()).isEqualTo(ListingStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("impact")
    class Impact {

        @Test
        @DisplayName("a nearly new item saves the full category figure")
        void likeNewSavesFullAmount() {
            Listing item = listing(Category.ELECTRONICS, Condition.LIKE_NEW);

            assertThat(item.estimatedCo2SavedKg()).isEqualTo(Category.ELECTRONICS.co2SavedKgPerReuse());
        }

        @Test
        @DisplayName("a worn item saves less than a nearly new one")
        void wornSavesLess() {
            double likeNew = listing(Category.ELECTRONICS, Condition.LIKE_NEW).estimatedCo2SavedKg();
            double wellUsed = listing(Category.ELECTRONICS, Condition.WELL_USED).estimatedCo2SavedKg();

            assertThat(wellUsed).isLessThan(likeNew).isPositive();
        }
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("a listing is not owned by an unrelated student")
        void notOwnedByStranger() {
            Listing item = listing(Category.TEXTBOOKS, Condition.GOOD);

            assertThat(item.isOwnedBy(student("Other Person"))).isFalse();
            assertThat(item.isOwnedBy(null)).isFalse();
        }
    }
}
