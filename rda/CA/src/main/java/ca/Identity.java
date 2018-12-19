package ca;


import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;

import javax.persistence.*;
import java.sql.Timestamp;

@javax.persistence.Entity // This tells Hibernate to make a table out of this class
public class Identity {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    private String name;

    private String passwordHash;

	@Column(length = 8192)
    private String publicKey;

	private Timestamp lastLoginAttempt;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPasswordHash(){
	    return passwordHash;
    }

    public void setPasswordHash(String password){
		SHA3.DigestSHA3 md = new SHA3.DigestSHA3(256);
		md.update(password.getBytes());
		this.passwordHash = Hex.toHexString(md.digest());
    }

	public String getPublicKey(){
	    return publicKey;
    }

    public void setPublicKey(String publicKey){
	    this.publicKey = publicKey;
    }

    public Timestamp getLastLoginAttempt(){
		return lastLoginAttempt;
	}

	public void setLastLoginAttempt(Timestamp lastLoginAttempt){
		this.lastLoginAttempt = lastLoginAttempt;
	}

    public boolean passwordIsCorrect(String password){
		SHA3.DigestSHA3 md = new SHA3.DigestSHA3(256);
		md.update(password.getBytes());
		return passwordHash.equals(Hex.toHexString(md.digest()));
	}
}