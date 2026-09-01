package ng.edu.unilag.tradeup.repository;

import java.util.List;
import ng.edu.unilag.tradeup.domain.Offer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    @EntityGraph(attributePaths = {"offeredBy", "offeredListing"})
    List<Offer> findByListingIdOrderByCreatedAtDesc(Long listingId);

    @EntityGraph(attributePaths = {"listing", "listing.owner", "offeredListing"})
    List<Offer> findByOfferedByIdOrderByCreatedAtDesc(Long userId);

    /** Offers waiting on a decision from this student across all of their listings. */
    @EntityGraph(attributePaths = {"listing", "offeredBy", "offeredListing"})
    List<Offer> findByListingOwnerIdAndStatusOrderByCreatedAtDesc(Long ownerId, Offer.Status status);

    long countByListingIdAndStatus(Long listingId, Offer.Status status);

    boolean existsByListingIdAndOfferedByIdAndStatus(Long listingId, Long offeredById, Offer.Status status);
}
