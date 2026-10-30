package g2.be.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import g2.be.entity.WorkingTimeSlot;

@Repository
public interface WorkingTimeSlotRepository extends JpaRepository<WorkingTimeSlot, Long> {

    /**
     * Tìm tất cả time slots không bị xóa
     */
    Page<WorkingTimeSlot> findByIsDeletedFalse(Pageable pageable);

    /**
     * Tìm tất cả time slots không bị xóa (List)
     */
    List<WorkingTimeSlot> findByIsDeletedFalse();

    /**
     * Tìm time slots theo trạng thái active
     */
    Page<WorkingTimeSlot> findByIsActiveAndIsDeletedFalse(Boolean isActive, Pageable pageable);

    /**
     * Tìm time slots theo tên (search)
     */
    @Query("SELECT w FROM WorkingTimeSlot w WHERE " +
           "LOWER(w.shiftName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND w.isDeleted = false")
    Page<WorkingTimeSlot> searchByShiftName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Kiểm tra tên ca làm việc đã tồn tại chưa
     */
    boolean existsByShiftNameAndIsDeletedFalse(String shiftName);

    /**
     * Kiểm tra tên ca làm việc đã tồn tại chưa (trừ slot hiện tại khi update)
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WorkingTimeSlot w " +
           "WHERE LOWER(w.shiftName) = LOWER(:shiftName) " +
           "AND w.slotId != :slotId " +
           "AND w.isDeleted = false")
    boolean existsByShiftNameAndSlotIdNot(
            @Param("shiftName") String shiftName,
            @Param("slotId") Long slotId
    );

    /**
     * Kiểm tra xem có time slot trùng giờ không
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WorkingTimeSlot w " +
           "WHERE w.isDeleted = false " +
           "AND (" +
           "   (w.startTime <= :startTime AND w.endTime > :startTime) OR " +
           "   (w.startTime < :endTime AND w.endTime >= :endTime) OR " +
           "   (w.startTime >= :startTime AND w.endTime <= :endTime)" +
           ")")
    boolean existsOverlappingSlot(
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Kiểm tra xem có time slot trùng giờ không (trừ slot hiện tại khi update)
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WorkingTimeSlot w " +
           "WHERE w.slotId != :slotId " +
           "AND w.isDeleted = false " +
           "AND (" +
           "   (w.startTime <= :startTime AND w.endTime > :startTime) OR " +
           "   (w.startTime < :endTime AND w.endTime >= :endTime) OR " +
           "   (w.startTime >= :startTime AND w.endTime <= :endTime)" +
           ")")
    boolean existsOverlappingSlotExcludingCurrent(
            @Param("slotId") Long slotId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Tìm time slot theo ID và không bị xóa
     */
    Optional<WorkingTimeSlot> findBySlotIdAndIsDeletedFalse(Long slotId);

       Optional<WorkingTimeSlot> findByStartTimeAndIsActiveTrueAndIsDeletedFalse(LocalTime startTime);

    @Query("SELECT w FROM WorkingTimeSlot w WHERE w.isActive = true " +
           "AND w.isDeleted = false " +
           "AND w.startTime <= :appointmentTime " +
           "AND w.endTime >= :appointmentTime " +
           "ORDER BY w.startTime DESC")
    List<WorkingTimeSlot> findActiveSlotsContainingTime(@Param("appointmentTime") LocalTime appointmentTime);

    /**
     * Tìm tất cả active time slots
     */
    @Query("SELECT w FROM WorkingTimeSlot w WHERE w.isActive = true " +
           "AND w.isDeleted = false " +
           "ORDER BY w.startTime")
    List<WorkingTimeSlot> findAllActiveSlots();
}
