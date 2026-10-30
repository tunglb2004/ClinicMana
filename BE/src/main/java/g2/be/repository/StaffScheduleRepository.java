package g2.be.repository;

import g2.be.entity.StaffSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho bảng staff_schedules.
 * Truy vấn lịch làm việc của nhân viên.
 * 
 * @author Nam
 */
@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, Long> {

    // Lấy lịch theo ngày (phân trang)
    Page<StaffSchedule> findByWorkDate(LocalDate workDate, Pageable pageable);

    // Lấy lịch theo ngày (list)
    List<StaffSchedule> findByWorkDate(LocalDate workDate);

    // Lấy lịch theo user
    List<StaffSchedule> findByUser_UserId(Long userId);

    // Lấy lịch theo user trong khoảng ngày
    List<StaffSchedule> findByUser_UserIdAndWorkDateBetween(Long userId, LocalDate fromDate, LocalDate toDate);

    // Lấy lịch theo khoa + ngày (Doctor thuộc khoa)
    @Query("SELECT ss FROM StaffSchedule ss " +
            "LEFT JOIN Doctor d ON d.user = ss.user " +
            "LEFT JOIN Nurse n ON n.user = ss.user " +
            "WHERE ss.workDate = :workDate " +
            "AND (d.department.deptId = :deptId OR n.department.deptId = :deptId)")
    Page<StaffSchedule> findByWorkDateAndDeptId(
            @Param("workDate") LocalDate workDate,
            @Param("deptId") Integer deptId,
            Pageable pageable);

    // Lấy lịch theo ca + ngày (dùng cho lấy bác sĩ trực)
    List<StaffSchedule> findBySlot_SlotIdAndWorkDate(Long slotId, LocalDate workDate);

    // Kiểm tra trùng lịch: cùng user, cùng ca, cùng ngày
    boolean existsByUser_UserIdAndSlot_SlotIdAndWorkDate(Long userId, Long slotId, LocalDate workDate);

    // Kiểm tra trùng lịch khi update (trừ chính nó)
    boolean existsByUser_UserIdAndSlot_SlotIdAndWorkDateAndScheduleIdNot(
            Long userId, Long slotId, LocalDate workDate, Long scheduleId);

    // Lấy lịch theo userId + ngày (tìm phòng được gán trong staff_schedules)
    List<StaffSchedule> findByUser_UserIdAndWorkDate(Long userId, LocalDate workDate);

        List<StaffSchedule> findByUser_UserIdAndWorkDateAndSlot_SlotId(Long userId, LocalDate workDate, Long slotId);

        Optional<StaffSchedule> findFirstByUser_UserIdAndWorkDateAndSlot_SlotId(Long userId, LocalDate workDate, Long slotId);

        Optional<StaffSchedule> findFirstByUser_UserIdAndWorkDateAndSlot_SlotIdAndRoom_RoomId(
                        Long userId,
                        LocalDate workDate,
                        Long slotId,
                        String roomId);

        // Lấy toàn bộ lịch đang gắn với 1 phòng
        List<StaffSchedule> findByRoom_RoomId(String roomId);

        List<StaffSchedule> findByRoom_RoomIdAndWorkDateOrderBySlot_StartTimeAsc(String roomId, LocalDate workDate);

    // Đếm số bác sĩ đã gán vào phòng + ca + ngày
    @Query("SELECT COUNT(ss) FROM StaffSchedule ss " +
            "JOIN Doctor d ON d.user = ss.user " +
            "WHERE ss.room.roomId = :roomId AND ss.slot.slotId = :slotId AND ss.workDate = :workDate")
    long countDoctorsInRoomSlotDate(@Param("roomId") String roomId,
                                    @Param("slotId") Long slotId,
                                    @Param("workDate") LocalDate workDate);

    // Đếm số y tá đã gán vào phòng + ca + ngày
    @Query("SELECT COUNT(ss) FROM StaffSchedule ss " +
            "JOIN Nurse n ON n.user = ss.user " +
            "WHERE ss.room.roomId = :roomId AND ss.slot.slotId = :slotId AND ss.workDate = :workDate")
    long countNursesInRoomSlotDate(@Param("roomId") String roomId,
                                   @Param("slotId") Long slotId,
                                   @Param("workDate") LocalDate workDate);
}
