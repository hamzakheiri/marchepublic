package fr._42.marchepublic.controller;

import fr._42.marchepublic.controller.dto.LoginRequest;
import fr._42.marchepublic.controller.dto.LoginResponse;
import fr._42.marchepublic.controller.dto.SignupRequest;
import fr._42.marchepublic.controller.dto.SignupResponse;
import fr._42.marchepublic.model.User;
import fr._42.marchepublic.service.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsersService usersService;

    public AuthController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("username, email and password are required");
        }

        User createdUser = usersService.registerUser(
                request.getUsername().trim(),
                request.getEmail().trim(),
                request.getPassword()
        );

        SignupResponse response = new SignupResponse(
                createdUser.getId(),
                createdUser.getUsername(),
                createdUser.getEmail(),
                createdUser.getRole().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("email and password are required");
        }

        User user = usersService.authenticateUser(
                request.getEmail().trim(),
                request.getPassword()
        );

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(response);
    }
}
