package rda.persistence.model;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity(name = "File")
@Table(name = "file")
public class File {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "filePath")
    private String filePath;

    @Column(name = "fileKey")
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Key> keys;

    @Column(name = "IV")
    private String IV;

    @Column(name = "pgp_sig")
    @Length(max = 8192)
    private String pgp_sig;

    @Column(name = "sha256")
    @Length(max = 256)
    private String sha256;

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(name = "user_file", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "file_id"))
    private Set<User> users = new HashSet<>();

    public void addUser (User user) {
        users.add(user);
        user.getFiles().add(this);
    }

    public void addKeys(Key key){
        keys.add(key);
        key.setFile(this);
    }

    public String toString () {
        return filePath;
    }

}
