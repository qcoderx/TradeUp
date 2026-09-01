package ng.edu.unilag.tradeup.repository;

import java.util.List;
import java.util.Optional;
import ng.edu.unilag.tradeup.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByListingIdAndBuyerId(Long listingId, Long buyerId);

    /**
     * Every thread a student is part of, whether they started it or their own
     * listing was the subject. Newest activity first.
     */
    @Query("""
            select distinct c from Conversation c
            join fetch c.listing l
            join fetch l.owner
            join fetch c.buyer
            where c.buyer.id = :userId or l.owner.id = :userId
            order by c.updatedAt desc
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);
}
