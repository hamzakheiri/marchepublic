package fr._42.marchepublic.controller;

import fr._42.marchepublic.controller.dto.AdminUserResponse;
import fr._42.marchepublic.controller.dto.UpdateUserRoleRequest;
import fr._42.marchepublic.model.Role;
import fr._42.marchepublic.model.User;
import fr._42.marchepublic.service.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
public class AdminUsersController {
    private final UsersService usersService;

    public AdminUsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listUsers(@RequestHeader("X-User-Id") Long requesterId) {
        List<AdminUserResponse> users = usersService.listUsersForAdmin(requesterId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<AdminUserResponse> updateUserRole(
            @RequestHeader("X-User-Id") Long requesterId,
            @PathVariable Long userId,
            @RequestBody UpdateUserRoleRequest request
    ) {
        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new IllegalArgumentException("role is required");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("role must be one of: USER, TECHNICIAN, ADMIN, SADMIN");
        }

        User updatedUser = usersService.updateUserRoleForAdmin(requesterId, userId, role);
        return ResponseEntity.ok(toResponse(updatedUser));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader("X-User-Id") Long requesterId,
            @PathVariable Long userId
    ) {
        usersService.deleteUserForAdmin(requesterId, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
