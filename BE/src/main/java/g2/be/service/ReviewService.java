package g2.be.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import g2.be.dto.request.CreateReviewRequest;
import g2.be.dto.request.ReplyReviewRequest;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.ReviewResponse;
import g2.be.entity.Appointment;
import g2.be.entity.Doctor;
import g2.be.entity.Patient;
import g2.be.entity.Review;
import g2.be.entity.ServiceOrder;
import g2.be.entity.User;
import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DoctorRepository;
import g2.be.repository.PatientRepository;
import g2.be.repository.ReviewRepository;
import g2.be.repository.ServiceOrderRepository;
import g2.be.repository.ServiceRepository;
import g2.be.repository.UserRepository;
import g2.be.util.ClinicTime;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        // Validate rating
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new AppException(ErrorCode.REVIEW_INVALID_RATING);
        }

        Patient patient = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        Doctor doctor = null;
        if (request.getDoctorId() != null) {
            doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
        }

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

            ensurePatientOwnsAppointment(patient, appointment);

            if (reviewRepository.existsActiveReview(
                    request.getAppointmentId(),
                    patient.getPatientId(),
                    doctor != null ? doctor.getDoctorId() : null,
                    request.getServiceId())) {
                throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }
        }

        g2.be.entity.Service reviewedService = null;
        if (request.getServiceId() != null) {
            reviewedService = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

            if (!appointmentIncludesService(appointment, request.getServiceId())) {
                throw new AppException(ErrorCode.SERVICE_NOT_FOUND);
            }
        }

        Review review = Review.builder()
                .patient(patient)
                .doctor(doctor)
                .service(reviewedService)
                .appointment(appointment)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        return ReviewResponse.fromEntity(review);
    }

        @Transactional(readOnly = true)
        public List<ReviewResponse> getMyAppointmentReviews(Long userId, String appointmentId) {
        Patient patient = patientRepository.findByUser_UserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        ensurePatientOwnsAppointment(patient, appointment);

        return reviewRepository.findActiveByAppointmentIdAndPatientId(appointmentId, patient.getPatientId()).stream()
            .map(ReviewResponse::fromEntity)
            .toList();
        }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(Long userId, int page, int size) {
        Patient patient = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByPatientId(patient.getPatientId(), pageable);
        return toPageResponse(reviewPage);
    }

    @Transactional
    public ReviewResponse replyReview(Long reviewId, ReplyReviewRequest request, Long userId) {
        Review review = reviewRepository.findByReviewIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (review.getReplyContent() != null) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_REPLIED);
        }

        User replyUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));

        review.setReplyContent(request.getReplyContent().trim());
        review.setReplyBy(replyUser);
        review.setReplyAt(ClinicTime.now());

        review = reviewRepository.save(review);
        return ReviewResponse.fromEntity(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByDoctor(String doctorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByDoctorId(doctorId, pageable);
        return toPageResponse(reviewPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByPatient(String patientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByPatientId(patientId, pageable);
        return toPageResponse(reviewPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAllReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findAllActive(pageable);
        return toPageResponse(reviewPage);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findByReviewIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        return ReviewResponse.fromEntity(review);
    }

    private void ensurePatientOwnsAppointment(Patient patient, Appointment appointment) {
        boolean ownAppointment = appointment.getPatient() != null
                && patient.getPatientId().equals(appointment.getPatient().getPatientId());
        boolean bookedForRelative = patient.getPatientId().equals(appointment.getBookedByPatientId());
        if (!ownAppointment && !bookedForRelative) {
            throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
    }

    private boolean appointmentIncludesService(Appointment appointment, String serviceId) {
        if (appointment == null || serviceId == null || serviceId.isBlank()) {
            return false;
        }

        boolean appointmentService = appointment.getServices() != null
                && appointment.getServices().stream()
                .anyMatch(service -> service != null && serviceId.equals(service.getServiceId()));
        if (appointmentService) {
            return true;
        }

        return serviceOrderRepository
                .findByMedicalRecord_Appointment_AppointmentIdOrderByCreatedAtDesc(appointment.getAppointmentId())
                .stream()
                .filter(order -> order != null && !"CANCELLED".equalsIgnoreCase(order.getStatus()))
                .map(ServiceOrder::getService)
                .anyMatch(service -> service != null && serviceId.equals(service.getServiceId()));
    }

    private PageResponse<ReviewResponse> toPageResponse(Page<Review> reviewPage) {
        return PageResponse.<ReviewResponse>builder()
                .content(reviewPage.getContent().stream()
                        .map(ReviewResponse::fromEntity)
                        .toList())
                .pageNumber(reviewPage.getNumber() + 1)
                .pageSize(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .first(reviewPage.isFirst())
                .build();
    }
}
