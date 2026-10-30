package g2.be.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Token Authentication - Nhập token vào đây (không cần thêm 'Bearer ')",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clinic Management System API")
                        .version("1.0")
                        .description("API quản lý phòng khám - Hệ thống đặt lịch khám, quản lý bệnh nhân, bác sĩ")
                        .contact(new Contact()
                                .name("G2 Team")
                                .email("support@clinic.com")))
                .servers(List.of(new Server()
                        .url(publicBaseUrl)
                        .description("Active Server")));
    }
}

