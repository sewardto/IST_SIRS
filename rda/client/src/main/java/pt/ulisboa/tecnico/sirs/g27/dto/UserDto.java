package pt.ulisboa.tecnico.sirs.g27.dto;

import org.springframework.http.HttpHeaders;
import pt.ulisboa.tecnico.sirs.g27.validation.ValidEmail;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UserDto {
    @NotEmpty
    @NotNull
    private String username;

    @NotEmpty
    @NotNull
    private String password;
    private String matchingPassword;

    @ValidEmail
    @NotEmpty
    @NotNull
    private String email;

    private String privateKey;
    private String publicKey;

    private HttpHeaders httpHeaders;

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getEmail() {
        return this.email;
    }

    public String getMatchingPassword() {
        return this.matchingPassword;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public HttpHeaders getHttpHeaders() {
        return this.httpHeaders;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setMatchingPassword(String matchingPassword){
        this.matchingPassword = matchingPassword;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPublicKey(String path) throws Exception{
        this.publicKey = new String(Files.readAllBytes(Paths.get(path)));
    }

    public void setPrivateKey(String path) throws Exception{
        this.privateKey = new String(Files.readAllBytes(Paths.get(path)));
    }

    public void setHttpHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }
}
