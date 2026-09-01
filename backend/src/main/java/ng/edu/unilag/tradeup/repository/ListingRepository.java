package ng.edu.unilag.tradeup.repository;

import java.util.List;
import java.util.Optional;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.domain.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Listing persistence.
 *
 * <p>Extending {@link JpaSpecificationExecutor} is what lets the browse screen
 * combine keyword, category, condition, intent and price filters in any
 * combination without a separate finder method for each permutation. The
 * predicates themselves live in {@code ListingSpecifications}.
 */
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    @EntityGraph(attributePaths = {"owner", "imageUrls"})
    Optional<Listing> findByReferenceIgnoreCase(String reference);

    @EntityGraph(attributePaths = {"owner", "imageUrls"})
    Page<Listing> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    List<Listing> findByOwnerIdAndStatus(Long ownerId, ListingStatus status);

    boolean existsByReference(String reference);

    long countByStatus(ListingStatus status);

    /** Fresh arrivals for the landing page, newest first. */
    @EntityGraph(attributePaths = {"owner", "imageUrls"})
    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status, Pageable pageable);

    /** Most-viewed available items, for the featured rail. */
    @EntityGraph(attributePaths = {"owner", "imageUrls"})
    List<Listing> findByStatusOrderByViewCountDesc(ListingStatus status, Pageable pageable);

    /**
     * Listing counts per category, used for the browse sidebar. Returns rows of
     * {@code [Category, Long]} so one query replaces one count per category.
     */
    @Query("""
            select l.category, count(l)
            from Listing l
            where l.status = :status
            group by l.category
            """)
    List<Object[]> countByCategory(@Param("status") ListingStatus status);

    /** Every completed trade, which is what the SDG 12 impact figures are built from. */
    @Query("select l from Listing l where l.status = ng.edu.unilag.tradeup.domain.ListingStatus.COMPLETED")
    List<Listing> findCompletedTrades();

    /** Total naira value that changed hands second-hand rather than new. */
    @Query("""
            select coalesce(sum(l.priceKobo), 0)
            from Listing l
            where l.status = :status and l.priceKobo is not null
            """)
    long sumPriceKoboByStatus(@Param("status") ListingStatus status);

    /** Similar items on the detail page: same category, excluding the item itself. */
    @EntityGraph(attributePaths = {"owner", "imageUrls"})
    List<Listing> findByCategoryAndStatusAndIdNot(
            Category category, ListingStatus status, Long excludedId, Pageable pageable);
}
