package g2.be.service;

import g2.be.dto.request.CreateReviewRequest;
import g2.be.dto.response.ReviewResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Doctor;
import g2.be.entity.MedicalRecord;
import g2.be.entity.Patient;
import g2.be.entity.Review;
import g2.be.entity.Service;
import g2.be.entity.ServiceOrder;
import g2.be.entity.User;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.PatientRepository;
import g2.be.repository.ReviewRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.repository.ServiceRepository;
import g2.be.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createReviewAllowsServiceFromServiceOrder() {
        Patient patient = Patient.builder()
                .patientId("PAT-1")
                .user(User.builder().userId(10L).fullName("Patient").build())
                .build();
        Doctor doctor = Doctor.builder()
                .doctorId("DOC-1")
                .user(User.builder().userId(20L).fullName("Doctor").build())
                .build();
        Service service = Service.builder()
                .serviceId("SVC-XRAY")
                .serviceName("X-Ray")
                .price(BigDecimal.valueOf(150000))
                .build();
        Appointment appointment = Appointment.builder()
                .appointmentId("APT-1")
                .patient(patient)
                .services(new ArrayList<>())
                .build();
        MedicalRecord record = MedicalRecord.builder()
                .recordId("REC-1")
                .appointment(appointment)
                .patient(patient)
                .build();
        ServiceOrder order = ServiceOrder.builder()
                .orderId("ORD-1")
                .medicalRecord(record)
                .service(service)
                .status("COMPLETED")
                .build();

        when(patientRepository.findByUser_UserId(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById("DOC-1")).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findById("APT-1")).thenReturn(Optional.of(appointment));
        when(serviceRepository.findById("SVC-XRAY")).thenReturn(Optional.of(service));
        when(serviceOrderRepository.findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc("APT-1"))
                .thenReturn(List.of(order));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(1L);
            return review;
        });

        CreateReviewRequest request = CreateReviewRequest.builder()
                .appointmentId("APT-1")
                .doctorId("DOC-1")
                .serviceId("SVC-XRAY")
                .rating(5)
                .comment("Good service")
                .build();

        ReviewResponse response = reviewService.createReview(10L, request);

        assertEquals("SVC-XRAY", response.getServiceId());
        assertEquals("DOC-1", response.getDoctorId());
        assertEquals("APT-1", response.getAppointmentId());
    }
}
