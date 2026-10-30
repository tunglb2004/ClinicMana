package g2.be.repository;

import g2.be.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn bảng doctors.
 * 
 * @author Nam
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, String> {

    // Tìm doctor theo userId (dùng khi gán/gỡ member)
    Optional<Doctor> findByUser_UserId(Long userId);

    // Lấy danh sách bác sĩ thuộc 1 khoa (dùng khi xem chi tiết khoa)
    List<Doctor> findByDepartment_DeptId(Integer deptId);

    // Lấy danh sách bác sĩ thuộc 1 phòng (dùng khi xem chi tiết phòng)
    List<Doctor> findByRoom_RoomId(String roomId);
}
