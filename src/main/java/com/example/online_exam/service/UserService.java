package com.example.online_exam.service;

import com.example.online_exam.dto.UserRegistrationDto;
import com.example.online_exam.entity.Role;
import com.example.online_exam.entity.StudentProfile;
import com.example.online_exam.entity.User;
import com.example.online_exam.exception.DuplicateResourceException;
import com.example.online_exam.exception.ResourceNotFoundException;
import com.example.online_exam.repository.StudentProfileRepository;
import com.example.online_exam.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       StudentProfileRepository studentProfileRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(UserRegistrationDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException(
                    "Username '" + dto.getUsername() + "' is already taken");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user = userRepository.save(user);

        if (dto.getRole() == Role.STUDENT) {
            validateStudentProfileFields(dto);
            StudentProfile profile = new StudentProfile(
                    dto.getStudentId(),
                    dto.getFullName(),
                    dto.getDepartment(),
                    dto.getEmail(),
                    user
            );
            studentProfileRepository.save(profile);
        }

        return user;
    }

    private void validateStudentProfileFields(UserRegistrationDto dto) {
        if (dto.getStudentId() == null || dto.getStudentId().isBlank()) {
            throw new IllegalArgumentException("Student ID is required for student accounts");
        }
        if (dto.getFullName() == null || dto.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required for student accounts");
        }
        if (dto.getDepartment() == null || dto.getDepartment().isBlank()) {
            throw new IllegalArgumentException("Department is required for student accounts");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required for student accounts");
        }
        if (studentProfileRepository.existsByStudentId(dto.getStudentId())) {
            throw new DuplicateResourceException(
                    "Student ID '" + dto.getStudentId() + "' is already registered");
        }
        if (studentProfileRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException(
                    "Email '" + dto.getEmail() + "' is already registered");
        }
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public List<User> findAllStudents() {
        return userRepository.findByRole(Role.STUDENT);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    public void toggleUserStatus(Long id) {
        User user = findById(id);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }
}
