package rda.persistence.model;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "fileKey")
public class Key {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file")
    private File file;

    @Column(length = 8129)
    private String fileKey;
}
