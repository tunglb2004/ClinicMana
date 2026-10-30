package g2.be.config;

import g2.be.entity.Role;
import g2.be.entity.User;
import g2.be.repository.RoleRepository;
import g2.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Init Roles if not exist
        initRole("ROLE_ADMIN");
        initRole("ROLE_SYSTEM_ADMIN");
        initRole("ROLE_CLINIC_MANAGER");
        initRole("ROLE_MANAGER");
        initRole("ROLE_DOCTOR");
        initRole("ROLE_CLINICALDOCTOR");
        initRole("ROLE_SPECIALISTDOCTOR");
        initRole("ROLE_NURSE");
        initRole("ROLE_RECEPTIONIST");
        initRole("ROLE_CASHIER");
        initRole("ROLE_PATIENT");

        // Init Admin User
        if (!userRepository.existsByUsername("adminLomiroom")) {
            Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").get();
            Role managerRole = roleRepository.findByRoleName("ROLE_CLINIC_MANAGER").get();

            User admin = User.builder()
                    .username("adminLomiroom")
                    .email("admin@clinic.com")
                    .fullName("Administrator")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .isActive(true)
                    .isDeleted(false)
                    .roles(new HashSet<>(roleRepository.findAll())) // Give all roles
                    .build();
            userRepository.save(admin);
            System.out.println(">>> Created admin user: adminLomiroom / admin123");
        }
    }

    private void initRole(String roleName) {
        if (roleRepository.findByRoleName(roleName).isEmpty()) {
            roleRepository.save(Role.builder().roleName(roleName).build());
        }
    }
}
