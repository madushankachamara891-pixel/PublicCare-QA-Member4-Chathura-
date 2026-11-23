package lk.publiccare.backend.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Dedicated, non-web entry point for explicitly provisioning the first local administrator. */
public final class AdminSetupApplication {
    private AdminSetupApplication() { }

    public static void main(String[] args) {
        var context = new SpringApplicationBuilder(AdminSetupConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles("admin-setup")
                .run(args);
        int exitCode = SpringApplication.exit(context);
        if (exitCode != 0) System.exit(exitCode);
    }
}
