package ng.edu.unilag.tradeup.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Condition;
import ng.edu.unilag.tradeup.domain.Conversation;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.domain.Offer;
import ng.edu.unilag.tradeup.domain.Report;
import ng.edu.unilag.tradeup.domain.SavedListing;
import ng.edu.unilag.tradeup.domain.TradeIntent;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.ConversationRepository;
import ng.edu.unilag.tradeup.repository.ListingRepository;
import ng.edu.unilag.tradeup.repository.OfferRepository;
import ng.edu.unilag.tradeup.repository.ReportRepository;
import ng.edu.unilag.tradeup.repository.SavedListingRepository;
import ng.edu.unilag.tradeup.repository.UserRepository;
import ng.edu.unilag.tradeup.service.ReferenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with a working marketplace.
 *
 * <p>The accounts are the eleven students of Group 15, and the catalogue is the
 * kind of thing that actually changes hands on the Akoka campus. Several trades
 * are seeded as already completed, which is what gives the impact figures on the
 * landing page something real to add up.
 *
 * <p>This runs only when the users table is empty, so restarting the app never
 * duplicates anything.
 */
@Component
@org.springframework.core.annotation.Order(1)
@ConditionalOnProperty(name = "tradeup.seed.enabled", havingValue = "true", matchIfMissing = false)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /**
     * The password given to every seeded account.
     *
     * <p>Set {@code TRADEUP_SEED_PASSWORD} to choose it. When it is not set a
     * random one is generated and logged once at startup — which means a
     * checkout of this repository can never be used to sign in to somebody
     * else's deployment, because no password is written down here.
     */
    private final String demoPassword;

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ConversationRepository conversationRepository;
    private final OfferRepository offerRepository;
    private final ReportRepository reportRepository;
    private final SavedListingRepository savedListingRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReferenceGenerator referenceGenerator;

    public DataSeeder(
            UserRepository userRepository,
            ListingRepository listingRepository,
            ConversationRepository conversationRepository,
            OfferRepository offerRepository,
            ReportRepository reportRepository,
            SavedListingRepository savedListingRepository,
            PasswordEncoder passwordEncoder,
            ReferenceGenerator referenceGenerator,
            @Value("${tradeup.seed.password:}") String configuredPassword) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.conversationRepository = conversationRepository;
        this.offerRepository = offerRepository;
        this.reportRepository = reportRepository;
        this.savedListingRepository = savedListingRepository;
        this.passwordEncoder = passwordEncoder;
        this.referenceGenerator = referenceGenerator;
        this.demoPassword = configuredPassword.isBlank() ? randomPassword() : configuredPassword;
    }

    /** A throwaway password with enough entropy that guessing it is hopeless. */
    private static String randomPassword() {
        byte[] bytes = new byte[18];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Database already has data; skipping the seed.");
            return;
        }

        log.info("Seeding TradeUp with the Group 15 roster and a starting catalogue.");

        Map<String, User> students = seedStudents();
        List<Listing> listings = seedListings(students);
        seedActivity(students, listings);

        log.info(
                "Seeded {} students and {} listings. Sign in with any seeded email and the password: {}",
                students.size(),
                listings.size(),
                demoPassword);
        log.info("Moderator rights are granted separately, through tradeup.security.admin-emails.");
    }

    // ---------------------------------------------------------------------
    // Students
    // ---------------------------------------------------------------------

    private Map<String, User> seedStudents() {
        record Seed(String key, String matric, String name, String email, String department, String hall, String bio) {}

        List<Seed> seeds = List.of(
                new Seed("okiki", "240817017", "Adebowale Okiki David", "okiki.adebowale@live.unilag.edu.ng",
                        "Data Science", "Jaja Hall",
                        "Final year data science. I keep the TradeUp board tidy and I never haggle for long."),
                new Seed("deborah", "240806153", "Bakare Deborah Oluwatosin", "deborah.bakare@live.unilag.edu.ng",
                        "Mathematics", "Moremi Hall",
                        "Maths student. Most of what I list is stats textbooks I have finished with."),
                new Seed("trust", "252605503", "Bello Trust Osereme", "trust.bello@live.unilag.edu.ng",
                        "Mathematics", "Eni Njoku Hall", "Happy to swap. I am usually around the Faculty of Science."),
                new Seed("victor", "252609502", "Fatoyinbo Victor Ayomikun", "victor.fatoyinbo@live.unilag.edu.ng",
                        "Data Science", "Mariere Hall", "Selling off the things I no longer carry to class."),
                new Seed("joan", "240805034", "Obi Omasirichukwu Joan", "joan.obi@live.unilag.edu.ng",
                        "Computer Science", "Kofo Ademola Hall",
                        "CS student. I would rather my old gear went to a first year than into a bin."),
                new Seed("mistura", "240313022", "Adebayo Mistura Temitope", "mistura.adebayo@live.unilag.edu.ng",
                        "Science Education", "Queen Amina Hall", "Science education. Ask me about lab kit."),
                new Seed("abdurrahman", "240805036", "Adeniran Abdurrahman Adebolaji",
                        "abdurrahman.adeniran@live.unilag.edu.ng", "Computer Science", "Makama Bida Hall",
                        "I fix and resell small electronics around campus."),
                new Seed("quadri", "240817008", "Lasisi Quadri Toluwalase", "quadri.lasisi@live.unilag.edu.ng",
                        "Data Science", "Jaja Hall", "Data science. I built a chunk of this site."),
                new Seed("blessing", "240805111", "Harrison Blessing Idoreyin", "blessing.harrison@live.unilag.edu.ng",
                        "Computer Science", "Moremi Hall", "Trading up my hostel kit one item at a time."),
                new Seed("jumoke", "252609512", "Olawunmi Afolabi Olajumoke", "jumoke.olawunmi@live.unilag.edu.ng",
                        "Data Science", "Honours Hall", "New here. Looking for a cheap desk lamp and a kettle."),
                new Seed("abdulmalik", "240817013", "Salami Abdulmalik Ayobami", "abdulmalik.salami@live.unilag.edu.ng",
                        "Data Science", "Sodeinde Hall", "Second hand first. That is the whole idea."));

        Map<String, User> students = new java.util.LinkedHashMap<>();
        for (Seed seed : seeds) {
            User user = new User(
                    seed.email(), seed.matric(), seed.name(), passwordEncoder.encode(demoPassword), seed.department());
            user.setCampusLocation(seed.hall());
            user.setBio(seed.bio());

            students.put(seed.key(), userRepository.save(user));
        }
        return students;
    }

    // ---------------------------------------------------------------------
    // Catalogue
    // ---------------------------------------------------------------------

    private List<Listing> seedListings(Map<String, User> students) {
        record Seed(
                String ownerKey,
                String title,
                String description,
                Category category,
                Condition condition,
                TradeIntent intent,
                Long naira,
                String swapWanted,
                String location,
                String image,
                int generation) {}

        List<Seed> seeds = List.of(
                new Seed("deborah", "MTH 201 and MTH 202 textbook set",
                        "Both course texts for second year maths, plus my own margin notes on the harder proofs. Covers are worn at the corners but every page is intact and nothing is torn out.",
                        Category.TEXTBOOKS, Condition.GOOD, TradeIntent.BOTH, 3500L,
                        "Any second year statistics text", "Faculty of Science car park",
                        "/brand/item-textbooks.webp", 2),

                new Seed("victor", "Casio scientific calculator",
                        "Got me through two years of engineering maths. Every key works and the display is clear. Comes with the slide cover, no case.",
                        Category.ELECTRONICS, Condition.GOOD, TradeIntent.SELL, 8000L, null,
                        "Mariere Hall lobby", "/brand/item-calculator.webp", 3),

                new Seed("jumoke", "Adjustable study desk lamp",
                        "Warm light, folds flat, bulb included and working. I am moving to a room that already has one.",
                        Category.HOSTEL_ESSENTIALS, Condition.LIKE_NEW, TradeIntent.SELL, 4500L, null,
                        "Honours Hall gate", "/brand/item-desklamp.webp", 1),

                new Seed("mistura", "White lab coat, size M",
                        "Standard issue coat, washed and pressed. One small ink mark on the left cuff that did not come out. Finished with practicals so it should go to someone starting theirs.",
                        Category.LAB_EQUIPMENT, Condition.GOOD, TradeIntent.BOTH, 3000L,
                        "A pair of safety goggles", "Education Quadrangle", "/brand/item-labcoat.webp", 2),

                new Seed("abdurrahman", "Compact mini fridge",
                        "Single door, cools properly, quiet enough to sleep next to. Heavy, so you will need help carrying it down. Serious buyers only.",
                        Category.ELECTRONICS, Condition.FAIR, TradeIntent.SELL, 45000L, null,
                        "Makama Bida Hall", "/brand/item-minifridge.webp", 2),

                new Seed("joan", "Three tier wooden bookshelf",
                        "Light oak, flat pack, no missing screws. Held my whole CS reading list for two years. Comes apart in five minutes.",
                        Category.FURNITURE, Condition.GOOD, TradeIntent.BOTH, 12000L,
                        "A desk chair in decent shape", "Kofo Ademola Hall", "/brand/item-bookshelf.webp", 1),

                new Seed("trust", "Technical drawing set",
                        "Compass, dividers and set squares in the original tin. Everything still holds its setting. I have moved on to doing it all on a laptop.",
                        Category.STATIONERY, Condition.LIKE_NEW, TradeIntent.SWAP, null,
                        "A graphics tablet, or a good scientific calculator", "Faculty of Science",
                        "/brand/item-drafting.webp", 1),

                new Seed("quadri", "Over ear wired headphones",
                        "Matte charcoal, folds flat, no bluetooth so the battery is never a problem. Ear pads are honest about their age but the sound is clean.",
                        Category.ELECTRONICS, Condition.GOOD, TradeIntent.BOTH, 9000L,
                        "A mechanical keyboard", "Jaja Hall", "/brand/item-headphones.webp", 2),

                new Seed("blessing", "Canvas laptop backpack",
                        "Deep indigo, padded laptop sleeve fits a 15 inch machine, all zips run smoothly. One small repair to the inside seam, done properly.",
                        Category.CLOTHING, Condition.GOOD, TradeIntent.SELL, 7500L, null,
                        "Moremi Hall", "/brand/item-backpack.webp", 3),

                new Seed("abdulmalik", "Desk fan",
                        "Three speeds, all working, cage guard intact. Gets you through the dry season. Selling because my new room has a ceiling fan.",
                        Category.HOSTEL_ESSENTIALS, Condition.FAIR, TradeIntent.SELL, 11000L, null,
                        "Sodeinde Hall", "/brand/item-fan.webp", 2),

                new Seed("okiki", "Small whiteboard with marker tray",
                        "Aluminium frame, wipes completely clean, no ghosting. Good for working through problem sets on your wall.",
                        Category.STATIONERY, Condition.LIKE_NEW, TradeIntent.BOTH, 4000L,
                        "A desk lamp", "Jaja Hall", "/brand/item-whiteboard.webp", 1),

                new Seed("deborah", "Stainless steel electric kettle",
                        "Boils fast, no limescale, lid closes properly. I am leaving the hostel and cannot take it with me.",
                        Category.HOSTEL_ESSENTIALS, Condition.GOOD, TradeIntent.SELL, 6500L, null,
                        "Moremi Hall", "/brand/item-kettle.webp", 2),

                new Seed("joan", "First year CS reading, four books",
                        "The four texts everyone is told to buy in first year. Bought them all, honestly only opened two. Would rather they went round again than sat on my shelf.",
                        Category.TEXTBOOKS, Condition.LIKE_NEW, TradeIntent.BOTH, 6000L,
                        "Second year CS texts", "Faculty of Science", "/brand/item-textbooks.webp", 1),

                new Seed("abdulmalik", "Second scientific calculator",
                        "Spare one I no longer need. Slightly older model, everything functional, a few scuffs on the back.",
                        Category.ELECTRONICS, Condition.WELL_USED, TradeIntent.SELL, 5000L, null,
                        "Sodeinde Hall", "/brand/item-calculator.webp", 4),

                new Seed("abdurrahman", "Study lamp, clip on",
                        "Clips to a bed frame or shelf edge. Bright, and the arm actually stays where you put it.",
                        Category.HOSTEL_ESSENTIALS, Condition.GOOD, TradeIntent.BOTH, 3800L,
                        "A power bank", "Makama Bida Hall", "/brand/item-desklamp.webp", 2),

                new Seed("victor", "Lab coat, size L",
                        "Larger size, barely worn, I ordered the wrong one and never got round to swapping it.",
                        Category.LAB_EQUIPMENT, Condition.LIKE_NEW, TradeIntent.SELL, 3500L, null,
                        "Mariere Hall", "/brand/item-labcoat.webp", 1));

        List<Listing> saved = new ArrayList<>();
        for (Seed seed : seeds) {
            Listing listing = new Listing(
                    referenceGenerator.next(listingRepository::existsByReference),
                    students.get(seed.ownerKey()),
                    seed.title(),
                    seed.description(),
                    seed.category(),
                    seed.condition(),
                    seed.intent());

            listing.setPriceKobo(seed.naira() == null ? null : seed.naira() * 100);
            listing.setSwapWanted(seed.swapWanted());
            listing.setPickupLocation(seed.location());
            listing.setImageUrls(List.of(seed.image()));
            listing.setOwnerGeneration(seed.generation());
            listing.setViewCount(12 + (seed.title().length() * 7) % 180);

            saved.add(listingRepository.save(listing));
        }
        return saved;
    }

    // ---------------------------------------------------------------------
    // Trades, conversations and one report to review
    // ---------------------------------------------------------------------

    private void seedActivity(Map<String, User> students, List<Listing> listings) {
        // Four handovers that already happened, so the impact figures are real.
        for (int index : new int[] {1, 3, 9, 11}) {
            listings.get(index).complete();
        }

        // Someone asking about the bookshelf.
        Listing bookshelf = listings.get(5);
        Conversation aboutBookshelf = conversationRepository.save(
                new Conversation(bookshelf, students.get("jumoke")));
        aboutBookshelf.post(students.get("jumoke"), "Hi, is the bookshelf still available? I am in Honours Hall.");
        aboutBookshelf.post(students.get("joan"), "Yes it is. I can help you carry it over this weekend.");
        aboutBookshelf.post(students.get("jumoke"), "That would be great. Saturday afternoon?");
        conversationRepository.save(aboutBookshelf);

        // And about the drafting set, which is swap only.
        Listing draftingSet = listings.get(6);
        Conversation aboutDrafting = conversationRepository.save(
                new Conversation(draftingSet, students.get("abdulmalik")));
        aboutDrafting.post(
                students.get("abdulmalik"), "Would you take a scientific calculator for the drawing set?");
        conversationRepository.save(aboutDrafting);

        // A cash offer waiting on Joan, and a swap offer waiting on Trust.
        offerRepository.save(Offer.cash(
                bookshelf, students.get("jumoke"), 10_000_00L, "Would you take ten thousand? I can collect it myself."));
        offerRepository.save(Offer.swap(
                draftingSet, students.get("abdulmalik"), listings.get(13), "Straight swap for my spare calculator?"));

        // A few saves, so the counts on the cards are not all zero.
        savedListingRepository.save(new SavedListing(students.get("jumoke"), listings.get(2)));
        savedListingRepository.save(new SavedListing(students.get("jumoke"), listings.get(11)));
        savedListingRepository.save(new SavedListing(students.get("blessing"), listings.get(5)));
        savedListingRepository.save(new SavedListing(students.get("victor"), listings.get(7)));

        // One open report so the moderation queue has something in it.
        reportRepository.save(new Report(
                listings.get(13),
                students.get("blessing"),
                Report.Reason.MISLEADING,
                "The photos look like a newer model than the one described in the text."));
    }
}
