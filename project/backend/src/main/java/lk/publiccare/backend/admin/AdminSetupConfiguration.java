package lk.publiccare.backend.admin;

import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Minimal application configuration for the explicit, non-web first-admin command. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@EntityScan(basePackageClasses = PublicCareUser.class)
@EnableJpaRepositories(basePackageClasses = UserRepository.class)
@Import({AdminAccountSetupService.class, AdminSetupRunner.class})
@Profile("admin-setup")
public class AdminSetupConfiguration {

    @Bean
    PasswordEncoder adminSetupPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
