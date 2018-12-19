package rda.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import rda.persistence.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);
}