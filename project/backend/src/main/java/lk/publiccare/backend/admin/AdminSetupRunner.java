package lk.publiccare.backend.admin;

import java.net.URI;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("admin-setup")
public class AdminSetupRunner implements ApplicationRunner {
    private final AdminAccountSetupService setupService;
    private final Environment environment;

    public AdminSetupRunner(AdminAccountSetupService setupService, Environment environment) {
        this.setupService = setupService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"true".equalsIgnoreCase(environment.getProperty("PUBLICCARE_ADMIN_SETUP_ENABLED", "false"))) {
            throw new IllegalStateException("Set PUBLICCARE_ADMIN_SETUP_ENABLED=true to run local admin setup.");
        }
        if (!isLoopbackPostgres(environment.getProperty("spring.datasource.url", ""))) {
            throw new IllegalStateException("Admin setup is restricted to a loopback PostgreSQL database.");
        }

        var result = setupService.createFirstAdmin(
                environment.getProperty("PUBLICCARE_ADMIN_FULL_NAME"),
                environment.getProperty("PUBLICCARE_ADMIN_EMAIL"),
                environment.getProperty("PUBLICCARE_ADMIN_PASSWORD"));
        System.out.println(result == AdminAccountSetupService.SetupResult.CREATED
                ? "Local administrator account created."
                : "Local administrator account was already configured; no changes made.");
    }

    private static boolean isLoopbackPostgres(String jdbcUrl) {
        String prefix = "jdbc:";
        if (!jdbcUrl.startsWith(prefix)) return false;
        try {
            URI uri = URI.create(jdbcUrl.substring(prefix.length()));
            String host = uri.getHost();
            return "postgresql".equalsIgnoreCase(uri.getScheme()) && host != null
                    && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1"));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
