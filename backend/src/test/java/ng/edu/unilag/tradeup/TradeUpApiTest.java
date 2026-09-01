package ng.edu.unilag.tradeup;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end checks over the real HTTP layer.
 *
 * <p>These go through the controllers, security filter, services and database
 * exactly as a browser would, so they catch the wiring problems that unit tests
 * on their own cannot.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeUpApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    /**
     * Accounts need a unique 9-digit matric number and a unique email. A counter
     * gives both, and unlike a timestamp its width is the same on every platform.
     */
    private static final java.util.concurrent.atomic.AtomicLong MATRIC =
            new java.util.concurrent.atomic.AtomicLong(240_000_000L);

    private static String nextMatric() {
        return String.valueOf(MATRIC.incrementAndGet());
    }

    private String sellerToken;
    private String buyerToken;
    private long sellerId;

    @BeforeEach
    void signUpTwoStudents() throws Exception {
        String sellerMatric = nextMatric();
        String buyerMatric = nextMatric();

        JsonNode seller = register("Seller Student", "seller" + sellerMatric + "@live.unilag.edu.ng", sellerMatric);
        JsonNode buyer = register("Buyer Student", "buyer" + buyerMatric + "@live.unilag.edu.ng", buyerMatric);

        sellerToken = seller.get("token").asText();
        sellerId = seller.get("user").get("id").asLong();
        buyerToken = buyer.get("token").asText();
    }

    // ---------------------------------------------------------------------
    // Registration and sign-in
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("registering returns a usable token and the new account")
    void registrationIssuesAToken() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Seller Student")))
                .andExpect(jsonPath("$.initials", is("SS")))
                .andExpect(jsonPath("$.admin", is(false)));
    }

    @Test
    @DisplayName("a bad password is rejected without saying which part was wrong")
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("identifier", "nobody@live.unilag.edu.ng", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("unauthenticated")))
                .andExpect(jsonPath("$.message", containsString("did not match")));
    }

    @Test
    @DisplayName("invalid registration details come back field by field")
    void validationReportsEveryField() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "fullName", "",
                                "email", "not-an-email",
                                "matricNumber", "12",
                                "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("invalid_request")))
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.matricNumber", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.fullName", notNullValue()));
    }

    // ---------------------------------------------------------------------
    // Listings
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("browsing is open to anyone, listing requires an account")
    void browsingIsPublicButWritingIsNot() throws Exception {
        mvc.perform(get("/api/listings")).andExpect(status().isOk());
        mvc.perform(get("/api/impact")).andExpect(status().isOk());
        mvc.perform(get("/api/team")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(11)));

        mvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validListing())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a new listing gets a reference code and shows up in browse")
    void createAndBrowse() throws Exception {
        JsonNode created = createListing(sellerToken, validListing());

        assert created.get("reference").asText().startsWith("TU-");

        mvc.perform(get("/api/listings").param("q", "Thermodynamics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items[0].title", containsString("Thermodynamics")));
    }

    @Test
    @DisplayName("a listing for sale must carry a price")
    void sellingNeedsAPrice() throws Exception {
        Map<String, Object> body = validListing();
        body.put("priceKobo", null);

        mvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Set a price")));
    }

    @Test
    @DisplayName("a swap-only listing must say what it wants")
    void swappingNeedsATarget() throws Exception {
        Map<String, Object> body = validListing();
        body.put("intent", "SWAP");
        body.put("priceKobo", null);
        body.remove("swapWanted");

        mvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("in exchange")));
    }

    @Test
    @DisplayName("one student cannot edit or delete another student's listing")
    void listingsAreOwnerOnly() throws Exception {
        long listingId = createListing(sellerToken, validListing()).get("id").asLong();

        mvc.perform(delete("/api/listings/" + listingId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("forbidden")));

        mvc.perform(post("/api/listings/" + listingId + "/complete")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("completing a trade advances provenance and moves the impact figures")
    void completingATradeCountsTowardsImpact() throws Exception {
        double before = impactCo2();
        long listingId = createListing(sellerToken, validListing()).get("id").asLong();

        mvc.perform(post("/api/listings/" + listingId + "/complete")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/listings/" + listingId).header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName", is("COMPLETED")))
                .andExpect(jsonPath("$.ownerGeneration", is(2)));

        assert impactCo2() > before : "a completed trade should raise the CO2 total";
    }

    @Test
    @DisplayName("a completed listing disappears from public browse")
    void completedListingsLeaveTheBoard() throws Exception {
        long listingId = createListing(sellerToken, validListing()).get("id").asLong();

        mvc.perform(post("/api/listings/" + listingId + "/complete")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isNoContent());

        // Anonymous visitors get a 404 rather than a hidden listing.
        mvc.perform(get("/api/listings/" + listingId)).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------------
    // Offers and messages
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("accepting an offer holds the item and declines the others")
    void acceptingAnOfferReservesTheItem() throws Exception {
        long listingId = createListing(sellerToken, validListing()).get("id").asLong();

        MvcResult offered = mvc.perform(post("/api/listings/" + listingId + "/offers")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("kind", "CASH", "amountKobo", 250_000, "note", "Can collect today."))))
                .andExpect(status().isCreated())
                .andReturn();

        long offerId = json.readTree(offered.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mvc.perform(post("/api/offers/" + offerId + "/accept").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        mvc.perform(get("/api/listings/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName", is("RESERVED")));
    }

    @Test
    @DisplayName("you cannot make an offer on your own listing")
    void noSelfOffers() throws Exception {
        long listingId = createListing(sellerToken, validListing()).get("id").asLong();

        mvc.perform(post("/api/listings/" + listingId + "/offers")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("kind", "CASH", "amountKobo", 100_000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("your own listing")));
    }

    @Test
    @DisplayName("messaging opens one thread per item and both sides can read it")
    void messagingCreatesASingleThread() throws Exception {
        long listingId = createListing(sellerToken, validListing()).get("id").asLong();

        mvc.perform(post("/api/conversations/listing/" + listingId)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("body", "Is this still available?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages", hasSize(1)));

        // Asking again resumes the same thread rather than opening a second one.
        mvc.perform(post("/api/conversations/listing/" + listingId)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("body", "Still interested."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages", hasSize(2)));

        mvc.perform(get("/api/conversations").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // The seller sees it too, with the buyer as the counterpart.
        mvc.perform(get("/api/conversations").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].counterpart.fullName", is("Buyer Student")))
                .andExpect(jsonPath("$[0].unreadCount", is(2)));
    }

    @Test
    @DisplayName("a student cannot read a thread they are not part of")
    void threadsArePrivate() throws Exception {
        long listingId = createListing(sellerToken, validListing()).get("id").asLong();

        MvcResult thread = mvc.perform(post("/api/conversations/listing/" + listingId)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("body", "Hello"))))
                .andExpect(status().isCreated())
                .andReturn();

        long conversationId = json.readTree(thread.getResponse().getContentAsString())
                .get("id")
                .asLong();

        String outsiderMatric = nextMatric();
        String outsider = register(
                        "Nosy Student", "nosy" + outsiderMatric + "@live.unilag.edu.ng", outsiderMatric)
                .get("token")
                .asText();

        mvc.perform(get("/api/conversations/" + conversationId).header("Authorization", "Bearer " + outsider))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------
    // Moderation
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("the moderation queue is closed to ordinary students")
    void moderationIsAdminOnly() throws Exception {
        mvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a student can report someone else's listing once")
    void reportingIsOncePerStudent() throws Exception {
        long listingId = createListing(sellerToken, validListing()).get("id").asLong();

        mvc.perform(post("/api/listings/" + listingId + "/report")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("reason", "MISLEADING", "details", "Photo does not match."))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/listings/" + listingId + "/report")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("reason", "SPAM"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already reported")));
    }

    // ---------------------------------------------------------------------
    // Profiles
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("a public profile shows only what is currently available")
    void profileShowsActiveListings() throws Exception {
        createListing(sellerToken, validListing());

        mvc.perform(get("/api/users/" + sellerId + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Seller Student")))
                .andExpect(jsonPath("$.activeListings", hasSize(greaterThanOrEqualTo(1))));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private JsonNode register(String name, String email, String matric) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "fullName", name,
                                "email", email,
                                "matricNumber", matric,
                                "password", "TestPassword1",
                                "department", "Computer Science"))))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createListing(String token, Map<String, Object> body) throws Exception {
        MvcResult result = mvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** A listing body that passes every rule, for tests to vary one field of. */
    private Map<String, Object> validListing() {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", "Thermodynamics textbook, third edition");
        body.put("description", "Used for one session. Spine is intact and nothing is torn out.");
        body.put("category", "TEXTBOOKS");
        body.put("itemCondition", "GOOD");
        body.put("intent", "SELL");
        body.put("priceKobo", 350_000);
        body.put("swapWanted", "Any second year maths text");
        body.put("pickupLocation", "Faculty of Science");
        body.put("imageUrls", java.util.List.of());
        return body;
    }

    private double impactCo2() throws Exception {
        MvcResult result = mvc.perform(get("/api/impact")).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString())
                .get("co2SavedKg")
                .asDouble();
    }
}
