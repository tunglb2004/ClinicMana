package g2.be.repository;

import g2.be.entity.CashHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface CashHandoverRepository extends JpaRepository<CashHandover, Long> {

    List<CashHandover> findByBusinessDateAndFromCashierUserIdOrderByCreatedAtDesc(
            LocalDate businessDate,
            Long fromCashierUserId
    );

    List<CashHandover> findByBusinessDateAndToCashierUserIdOrderByCreatedAtDesc(
            LocalDate businessDate,
            Long toCashierUserId
    );

    @Query("""
            SELECT COALESCE(SUM(h.amount), 0)
            FROM CashHandover h
            WHERE h.businessDate BETWEEN :fromDate AND :toDate
              AND h.status IN :statuses
            """)
    BigDecimal sumAmountByBusinessDateBetweenAndStatusIn(@Param("fromDate") LocalDate fromDate,
                                                         @Param("toDate") LocalDate toDate,
                                                         @Param("statuses") Collection<String> statuses);

    @Query("""
            SELECT COALESCE(SUM(h.amount), 0)
            FROM CashHandover h
            WHERE h.businessDate BETWEEN :fromDate AND :toDate
              AND h.fromCashierUserId = :fromCashierUserId
              AND h.status IN :statuses
            """)
    BigDecimal sumAmountByBusinessDateBetweenAndFromCashierUserIdAndStatusIn(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("fromCashierUserId") Long fromCashierUserId,
            @Param("statuses") Collection<String> statuses
    );

    @Query("""
            SELECT h FROM CashHandover h
            WHERE h.businessDate BETWEEN :fromDate AND :toDate
              AND (:cashierUserId IS NULL OR h.fromCashierUserId = :cashierUserId OR h.toCashierUserId = :cashierUserId)
              AND (:status IS NULL OR :status = '' OR h.status = :status)
            ORDER BY h.createdAt DESC
            """)
    List<CashHandover> findManagerHandovers(@Param("fromDate") LocalDate fromDate,
                                            @Param("toDate") LocalDate toDate,
                                            @Param("cashierUserId") Long cashierUserId,
                                            @Param("status") String status);
}
