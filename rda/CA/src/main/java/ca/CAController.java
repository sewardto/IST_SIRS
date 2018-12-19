package ca;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.ArrayList;


@Controller
@RequestMapping(path="/")
public class CAController {
    @Autowired
    private IdentityRepository identityRepository;

    @PostMapping(path="/save")
    public @ResponseBody String storeKey (@RequestParam String name
            , @RequestParam String password, @RequestParam String publicKey) {
        if(identityRepository.findByName(name) != null) {
            return "Error: Key for this identity already exists.";
        } else if(!name.matches("^.{8,32}$")){
            return "Error: Name Policy: Length between 8 and 32 characters";
        } else if(!password.matches("^.{8,32}$")) {
            return "Error: Password Policy: Length between 8 and 32 characters";
        } else if(publicKey == null || publicKey.trim().length() < 1024){
            return "Error: Invalid public key";
        } else {
            Identity n = new Identity();
            n.setName(name);
            n.setPasswordHash(password);
            n.setPublicKey(publicKey);
            n.setLastLoginAttempt(new Timestamp(System.currentTimeMillis() - 10 * 1000));
            identityRepository.save(n);
            return "Saved." + n.getLastLoginAttempt().toString();
        }
    }

    @PostMapping(path="/replace")
    public @ResponseBody String replaceKey(@RequestParam String name
            , @RequestParam String password, @RequestParam String publicKey) {
        Identity n = identityRepository.findByName(name);
        if(n == null) {
            return "Error: Identity doesn't exist.";
        } else if (!n.getLastLoginAttempt().before(new Timestamp(System.currentTimeMillis() - 10 * 1000))){
            return "Brute Force Protection: Try again later";
        } else if (!n.passwordIsCorrect(password)) {
            n.setLastLoginAttempt(new Timestamp(System.currentTimeMillis()));
            identityRepository.save(n);
            return "Error: Incorrect password. Try again in 10 seconds" + n.getLastLoginAttempt().toString();
        } else {
            n.setPublicKey(publicKey);
            n.setLastLoginAttempt(new Timestamp(System.currentTimeMillis() - 10 * 1000));
            identityRepository.save(n);
            return "Replaced.";
        }
    }

    @GetMapping(path="/key")
    public @ResponseBody String getKey(@RequestParam String name) {
        Identity n = identityRepository.findByName(name);
        if(n == null) {
            return "Error: Identity doesn't exist.";
        } else {
            return n.getPublicKey();
        }
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<String> getAllIdentity() {
        ArrayList<String> names = new ArrayList<>();
        Iterable<Identity> identities = identityRepository.findAll();
        for(Identity identity : identities) {
            names.add(identity.getName());
        }
        return names;
    }

    @GetMapping(path="/delete")
    public @ResponseBody String deleteEntity(@RequestParam String name, @RequestParam String password) {
        Identity n = identityRepository.findByName(name);
        if(n == null) {
            return "Error: Identity doesn't exist.";
        }else if (!n.getLastLoginAttempt().before(new Timestamp(System.currentTimeMillis() - 10 * 1000))){
            return "Brute Force Protection: Try again later";
        } else if (!n.passwordIsCorrect(password)) {
            n.setLastLoginAttempt(new Timestamp(System.currentTimeMillis()));
            identityRepository.save(n);
            return "Error: Incorrect password. Try again in 10 seconds" + n.getLastLoginAttempt().toString();
        } else {
            identityRepository.delete(n);
            return "Deleted";
        }
    }
}