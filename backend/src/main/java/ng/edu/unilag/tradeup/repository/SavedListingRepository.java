package ng.edu.unilag.tradeup.repository;

import java.util.List;
import java.util.Optional;
import ng.edu.unilag.tradeup.domain.SavedListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedListingRepository extends JpaRepository<SavedListing, Long> {

    Optional<SavedListing> findByUserIdAndListingId(Long userId, Long listingId);

    @Query("""
            select s from SavedListing s
            join fetch s.listing l
            join fetch l.owner
            where s.user.id = :userId
            order by s.createdAt desc
            """)
    List<SavedListing> findAllForUser(@Param("userId") Long userId);

    /** Used to mark the save state on cards without a query per listing. */
    @Query("select s.listing.id from SavedListing s where s.user.id = :userId")
    List<Long> findSavedListingIds(@Param("userId") Long userId);

    long countByListingId(Long listingId);
}
