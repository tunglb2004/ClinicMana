package g2.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyReviewRequest {

    @NotBlank(message = "Noi dung phan hoi khong duoc de trong")
    @Size(max = 1000, message = "Noi dung phan hoi khong duoc vuot qua 1000 ky tu")
    private String replyContent;
}
