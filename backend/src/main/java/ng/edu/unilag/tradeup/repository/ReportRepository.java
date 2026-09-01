package ng.edu.unilag.tradeup.repository;

import java.util.List;
import ng.edu.unilag.tradeup.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatusOrderByCreatedAtDesc(Report.Status status);

    boolean existsByListingIdAndReporterIdAndStatus(Long listingId, Long reporterId, Report.Status status);

    long countByStatus(Report.Status status);
}
