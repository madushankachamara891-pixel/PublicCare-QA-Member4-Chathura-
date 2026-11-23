package lk.publiccare.backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<PublicCareUser, Long> {
    Optional<PublicCareUser> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(UserRole role);
}
