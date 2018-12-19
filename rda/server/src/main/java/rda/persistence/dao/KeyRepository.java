package rda.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import rda.persistence.model.Key;

public interface KeyRepository extends JpaRepository<Key, Long> {
}
