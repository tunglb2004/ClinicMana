package g2.be.service;

import g2.be.entity.Appointment;
import g2.be.entity.Doctor;
import g2.be.entity.Room;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.util.ClinicTime;
import g2.be.util.QueuePriorityRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadBalancingService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    private static final int RETURNING_VISIT_MINUTES = 5;

    public Doctor findOptimalDoctor(Integer deptId, String excludeDoctorId) {
        if (deptId == null) {
            return null;
        }

        List<Doctor> doctors = doctorRepository.findByDepartment_DeptId(deptId);
        Doctor bestDoctor = null;
        int lowestEwt = Integer.MAX_VALUE;

        for (Doctor doctor : doctors) {
            if (excludeDoctorId != null && excludeDoctorId.equals(doctor.getDoctorId())) {
                continue;
            }

            if (!isOperationalDoctorRoom(doctor)) {
                continue;
            }

            Room room = doctor.getRoom();
            int ewt = calculateEWT(doctor);

            log.debug("LoadBalancing: doctor={}, room={}, EWT={}min",
                    doctor.getDoctorId(), room.getRoomId(), ewt);

            if (ewt < lowestEwt) {
                lowestEwt = ewt;
                bestDoctor = doctor;
            }
        }

        if (bestDoctor != null) {
            log.info("LoadBalancing: dept={}, selected doctor={} room={} (EWT={}min)",
                    deptId, bestDoctor.getDoctorId(), bestDoctor.getRoom().getRoomId(), lowestEwt);
        } else {
            log.warn("LoadBalancing: no available room found for dept={}", deptId);
        }

        return bestDoctor;
    }

    private int calculateEWT(Doctor doctor) {
        Room room = doctor.getRoom();
        int baseAvgTime = room.getBaseAvgTime() != null ? room.getBaseAvgTime() : 15;

        List<Appointment> queue = appointmentRepository.findActiveQueueByDoctorId(
                doctor.getDoctorId(),
                ClinicTime.today(),
                ClinicTime.now());

        int totalMinutes = 0;
        for (Appointment appointment : queue) {
            int score = appointment.getPriorityScore() != null ? appointment.getPriorityScore() : 0;
            if (score >= QueuePriorityRules.RETURNING_FROM_PARACLINICAL) {
                totalMinutes += RETURNING_VISIT_MINUTES;
            } else {
                totalMinutes += baseAvgTime;
            }
        }

        return totalMinutes;
    }

    private boolean isOperationalDoctorRoom(Doctor doctor) {
        if (doctor == null || doctor.getDepartment() == null || doctor.getRoom() == null) {
            return false;
        }
        if (Boolean.TRUE.equals(doctor.getDepartment().getIsDeleted())
                || !Boolean.TRUE.equals(doctor.getDepartment().getIsActive())) {
            return false;
        }

        Room room = doctor.getRoom();
        return !Boolean.TRUE.equals(room.getIsDeleted())
                && "ACTIVE".equalsIgnoreCase(room.getStatus())
                && room.getDepartment() != null
                && !Boolean.TRUE.equals(room.getDepartment().getIsDeleted())
                && Boolean.TRUE.equals(room.getDepartment().getIsActive());
    }
}
