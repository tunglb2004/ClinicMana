package g2.be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @Column(name = "room_id", length = 20)
    private String roomId;

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    @Column(name = "room_type", nullable = false, length = 20)
    private String roomType;

    @Column(length = 100)
    private String location;

    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Department department;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @JsonIgnore
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "base_avg_time")
    @Builder.Default
    private Integer baseAvgTime = 15;

    @Column(name = "current_queue_length")
    @Builder.Default
    private Integer currentQueueLength = 0;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "service_room",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private List<Service> services = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "room")
    @Builder.Default
    private List<Doctor> doctors = new ArrayList<>();
}
