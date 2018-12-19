package rda.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import rda.persistence.model.File;

public interface FileRepository extends JpaRepository<File, Long> {
    File findByFilePath(String filePath);
}
