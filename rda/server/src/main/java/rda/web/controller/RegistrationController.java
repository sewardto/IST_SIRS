package rda.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import rda.persistence.model.User;
import rda.service.IUserService;
import rda.web.dto.UserDto;
import rda.web.error.UserAlreadyExistException;

import javax.validation.Valid;
import java.util.Date;

@RestController
public class RegistrationController {
    private final IUserService service;
    @Autowired
    public RegistrationController(IUserService service) {
        this.service = service;
    }

    @GetMapping(value = "/login")
    public String login(@RequestParam("name") String name) {
        service.findUserByUsername(name).setTime(new Date());
        return "login success";
    }

    @PostMapping(value = "/registration")
    public String registerUserAccount(
            @RequestBody @Valid UserDto accountDto,
            BindingResult result) {
        User registered = new User();
        if (!result.hasErrors()) {
            registered = createUserAccount(accountDto);
        }
        if (registered == null) {
            result.rejectValue("email", "message.regError");
        }
        if (result.hasErrors()) {
            return "failed";
        }
        else {
            return "succeeded";
        }
    }
    private User createUserAccount(UserDto accountDto) {
        User registered = null;
        try {
            registered = service.registerNewUserAccount(accountDto);
        } catch (UserAlreadyExistException e) {
            return null;
        }
        return registered;
    }
}