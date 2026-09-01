package ng.edu.unilag.tradeup.web.dto;

import ng.edu.unilag.tradeup.domain.User;

/** The compact view of a student shown on cards and message threads. */
public record UserSummary(
        Long id,
        String fullName,
        String initials,
        String department,
        String campusLocation,
        int completedTrades,
        boolean admin) {

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getFullName(),
                user.initials(),
                user.getDepartment(),
                user.getCampusLocation(),
                user.getCompletedTrades(),
                user.isAdmin());
    }
}
