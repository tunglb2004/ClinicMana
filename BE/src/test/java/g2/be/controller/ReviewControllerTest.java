package g2.be.controller;

import g2.be.dto.request.CreateReviewRequest;
import g2.be.dto.request.ReplyReviewRequest;
import g2.be.dto.response.ApiResponse;
import g2.be.dto.response.PageResponse;
import g2.be.dto.response.ReviewResponse;
import g2.be.security.UserDetailsImpl;
import g2.be.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReviewReturnsSuccessfulResponse() {
        setAuthenticatedUser(12L);
        CreateReviewRequest request = new CreateReviewRequest();
        ReviewResponse reviewResponse = mock(ReviewResponse.class);

        when(reviewService.createReview(12L, request)).thenReturn(reviewResponse);

        ApiResponse<ReviewResponse> response = reviewController.createReview(request);

        assertTrue(response.isSuccess());
        assertEquals("Cảm ơn bạn! Đánh giá của bạn đã được gửi.", response.getMessage());
        assertSame(reviewResponse, response.getData());
    }

    @Test
    void replyReviewReturnsSuccessfulResponse() {
        setAuthenticatedUser(12L);
        ReplyReviewRequest request = new ReplyReviewRequest();
        ReviewResponse reviewResponse = mock(ReviewResponse.class);

        when(reviewService.replyReview(5L, request, 12L)).thenReturn(reviewResponse);

        ApiResponse<ReviewResponse> response = reviewController.replyReview(5L, request);

        assertTrue(response.isSuccess());
        assertEquals("Đã đăng phản hồi thành công.", response.getMessage());
        assertSame(reviewResponse, response.getData());
    }

    @Test
    void getAllReviewsReturnsPagedResponse() {
        PageResponse<ReviewResponse> pageResponse = PageResponse.<ReviewResponse>builder()
                .content(List.of(mock(ReviewResponse.class)))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(reviewService.getAllReviews(0, 10)).thenReturn(pageResponse);

        ApiResponse<PageResponse<ReviewResponse>> response = reviewController.getAllReviews(1, 10);

        assertTrue(response.isSuccess());
        assertSame(pageResponse, response.getData());
    }

    @Test
    void getReviewByIdReturnsReviewDetail() {
        ReviewResponse reviewResponse = mock(ReviewResponse.class);

        when(reviewService.getReviewById(5L)).thenReturn(reviewResponse);

        ApiResponse<ReviewResponse> response = reviewController.getReviewById(5L);

        assertTrue(response.isSuccess());
        assertSame(reviewResponse, response.getData());
    }

    private void setAuthenticatedUser(long userId) {
        UserDetailsImpl userDetails = new UserDetailsImpl(userId, "reviewer", "reviewer@test.com", "secret", List.of());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}