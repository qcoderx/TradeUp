package ng.edu.unilag.tradeup.web.dto;

import java.time.Instant;
import java.util.List;
import ng.edu.unilag.tradeup.domain.User;

/** A students public profile page, with whatever they currently have listed. */
public record UserProfile(
        Long id,
        String fullName,
        String initials,
        String department,
        String campusLocation,
        String bio,
        int completedTrades,
        Instant joinedAt,
        List<ListingCard> activeListings) {

    public static UserProfile from(User user, List<ListingCard> activeListings) {
        return new UserProfile(
                user.getId(),
                user.getFullName(),
                user.initials(),
                user.getDepartment(),
                user.getCampusLocation(),
                user.getBio(),
                user.getCompletedTrades(),
                user.getCreatedAt(),
                activeListings);
    }
}
