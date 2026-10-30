package g2.be.repository;

import g2.be.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn bảng rooms.
 * Hỗ trợ: lấy phòng theo khoa, tìm kiếm, kiểm tra trùng tên.
 * 
 * @author Nam
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    /**
     * Lấy danh sách phòng thuộc 1 khoa
     * Dùng khi xem chi tiết khoa hoặc filter phòng theo khoa
     */
    List<Room> findAllByIsDeletedFalseOrderByRoomIdAsc();

    /**
     * Tìm kiếm phòng theo tên (không phân biệt hoa thường)
     */
    List<Room> findByDepartment_DeptIdAndIsDeletedFalseOrderByRoomIdAsc(Integer deptId);

    /**
     * Lấy danh sách phòng theo khoa có phân trang
     */
    List<Room> findByDepartment_DeptIdAndIsDeletedFalse(Integer deptId);

    /**
     * Kiểm tra tên phòng đã tồn tại chưa
     */
    Optional<Room> findByRoomIdAndIsDeletedFalse(String roomId);

    /**
     * Kiểm tra tên phòng đã tồn tại chưa (loại trừ phòng hiện tại)
     */
    Boolean existsByRoomNameAndIsDeletedFalse(String roomName);

    /**
     * Kiểm tra roomId đã tồn tại chưa
     */
    Boolean existsByRoomNameAndIsDeletedFalseAndRoomIdNot(String roomName, String roomId);

    Boolean existsByRoomIdAndIsDeletedFalse(String roomId);

    List<Room> findByStatusAndIsDeletedFalse(String status);
}
