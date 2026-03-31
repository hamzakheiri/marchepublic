package fr._42.marchepublic.service;


import fr._42.marchepublic.exception.DuplicateEmailException;
import fr._42.marchepublic.exception.ForbiddenException;
import fr._42.marchepublic.exception.InvalidCredentialsException;
import fr._42.marchepublic.exception.ResourceNotFoundException;
import fr._42.marchepublic.model.Role;
import fr._42.marchepublic.model.User;
import fr._42.marchepublic.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersService {

    final private PasswordEncoder encoder;
    final private UsersRepository usersRepository;

    @Autowired
    public UsersService(
            PasswordEncoder encoder,
            UsersRepository usersRepository) {
        this.encoder = encoder;
        this.usersRepository = usersRepository;
    }


    public User createUser(User user) {
        Role role = user.getRole() == null ? Role.USER : user.getRole();
        usersRepository.findByEmail(user.getEmail()).ifPresent((u) -> {
                throw new DuplicateEmailException("Email already is use");
        });
        User CreatedUser = new User(null, user.getUsername(), encoder.encode(user.getPassword()), user.getEmail(), role);
        return usersRepository.save(CreatedUser);
    }

    public User registerUser(String username, String email, String password) {
        User user = new User(null, username, password, email, Role.USER);
        return createUser(user);
    }

    public User authenticateUser(String email, String password) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!encoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return user;
    }

    public List<User> listUsersForAdmin(Long requesterId) {
        requireManagementAccess(requesterId);
        return usersRepository.findAll();
    }

    public User updateUserRoleForAdmin(Long requesterId, Long userId, Role newRole) {
        User requester = requireManagementAccess(requesterId);

        if (newRole == null) {
            throw new IllegalArgumentException("role is required");
        }

        User targetUser = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!canAssignRole(requester.getRole(), targetUser.getRole(), newRole)) {
            throw new ForbiddenException("Insufficient privileges for this role assignment");
        }

        targetUser.setRole(newRole);
        return usersRepository.save(targetUser);
    }

    public void deleteUserForAdmin(Long requesterId, Long userId) {
        User requester = requireManagementAccess(requesterId);

        User targetUser = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (requesterId.equals(targetUser.getId())) {
            throw new IllegalArgumentException("You cannot remove your own account");
        }

        if (targetUser.getRole() == Role.SADMIN) {
            throw new ForbiddenException("SADMIN account cannot be removed");
        }

        if (!canDeleteRole(requester.getRole(), targetUser.getRole())) {
            throw new ForbiddenException("Insufficient privileges for this removal");
        }

        usersRepository.delete(targetUser);
    }

    private User requireManagementAccess(Long requesterId) {
        if (requesterId == null) {
            throw new ForbiddenException("Admin or SADMIN role required");
        }

        User requester = usersRepository.findById(requesterId)
                .orElseThrow(() -> new ForbiddenException("Admin or SADMIN role required"));

        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.SADMIN) {
            throw new ForbiddenException("Admin or SADMIN role required");
        }

        return requester;
    }

    private boolean canAssignRole(Role requesterRole, Role targetCurrentRole, Role targetNewRole) {
        if (requesterRole == Role.SADMIN) {
            return true;
        }

        if (requesterRole == Role.ADMIN) {
            return roleRank(targetCurrentRole) < roleRank(Role.ADMIN)
                    && (targetNewRole == Role.TECHNICIAN || targetNewRole == Role.USER);
        }

        return false;
    }

    private boolean canDeleteRole(Role requesterRole, Role targetRole) {
        if (requesterRole == Role.SADMIN) {
            return targetRole != Role.SADMIN;
        }

        if (requesterRole == Role.ADMIN) {
            return targetRole == Role.TECHNICIAN || targetRole == Role.USER;
        }

        return false;
    }

    private int roleRank(Role role) {
        return switch (role) {
            case USER -> 1;
            case TECHNICIAN -> 2;
            case ADMIN -> 3;
            case SADMIN -> 4;
        };
    }
}
