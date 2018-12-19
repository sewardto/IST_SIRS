package rda.persistence.model;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity(name = "User")
@Table(name = "user")
public class User {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "password", length = 60)
    @Size(min = 8 ,max = 32)
    private String password;

    @Column(name = "time_stamp")
    private Date time;

    @ManyToMany(mappedBy = "users")
    private Set<File> files = new HashSet<>();
}
