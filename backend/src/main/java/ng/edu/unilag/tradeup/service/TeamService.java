package ng.edu.unilag.tradeup.service;

import java.util.List;
import ng.edu.unilag.tradeup.web.dto.TeamMember;
import org.springframework.stereotype.Service;

/**
 * Group 15, COS202 Computer Programming II, University of Lagos.
 *
 * <p>The roster is served from the backend rather than hard-coded into the
 * frontend so that the names on the site and the names in the submitted
 * proposal come from a single place.
 */
@Service
public class TeamService {

    private static final List<TeamMember> MEMBERS = List.of(
            new TeamMember("240817017", "Adebowale Okiki David", "Data Science", "Team Captain"),
            new TeamMember("240806153", "Bakare Deborah Oluwatosin", "Mathematics", "Assistant Team Captain"),
            new TeamMember("252605503", "Bello Trust Osereme", "Mathematics", "Member"),
            new TeamMember("252609502", "Fatoyinbo Victor Ayomikun", "Data Science", "Member"),
            new TeamMember("240805034", "Obi Omasirichukwu Joan", "Computer Science", "Member"),
            new TeamMember("240313022", "Adebayo Mistura Temitope", "Science Education", "Member"),
            new TeamMember("240805036", "Adeniran Abdurrahman Adebolaji", "Computer Science", "Member"),
            new TeamMember("240817008", "Lasisi Quadri Toluwalase", "Data Science", "Lead Developer"),
            new TeamMember("240805111", "Harrison Blessing Idoreyin", "Computer Science", "Member"),
            new TeamMember("252609512", "Olawunmi Afolabi Olajumoke", "Data Science", "Member"),
            new TeamMember("240817013", "Salami Abdulmalik Ayobami", "Data Science", "Member"));

    public List<TeamMember> members() {
        return MEMBERS;
    }
}
