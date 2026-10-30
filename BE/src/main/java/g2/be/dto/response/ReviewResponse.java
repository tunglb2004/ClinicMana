package g2.be.dto.response;

import g2.be.entity.Review;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long reviewId;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String departmentName;
    private String appointmentId;
    private String serviceId;
    private String serviceName;
    private Integer rating;
    private String comment;
    private String replyContent;
    private String replyByName;
    private LocalDateTime replyAt;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review review) {
        ReviewResponseBuilder builder = ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .rating(review.getRating())
                .comment(review.getComment())
                .replyContent(review.getReplyContent())
                .replyAt(review.getReplyAt())
                .createdAt(review.getCreatedAt());

        if (review.getPatient() != null) {
            builder.patientId(review.getPatient().getPatientId());
            if (review.getPatient().getUser() != null) {
                builder.patientName(review.getPatient().getUser().getFullName());
            }
        }

        if (review.getDoctor() != null) {
            builder.doctorId(review.getDoctor().getDoctorId());
            if (review.getDoctor().getUser() != null) {
                builder.doctorName(review.getDoctor().getUser().getFullName());
            }
            if (review.getDoctor().getDepartment() != null) {
                builder.departmentName(review.getDoctor().getDepartment().getDeptName());
            }
        }

        if (review.getAppointment() != null) {
            builder.appointmentId(review.getAppointment().getAppointmentId());
        }

        if (review.getService() != null) {
            builder.serviceId(review.getService().getServiceId());
            builder.serviceName(review.getService().getServiceName());
        }

        if (review.getReplyBy() != null) {
            builder.replyByName(review.getReplyBy().getFullName());
        }

        return builder.build();
    }
}
