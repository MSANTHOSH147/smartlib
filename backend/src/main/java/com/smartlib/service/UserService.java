package com.smartlib.service;

import com.smartlib.entity.User;
import com.smartlib.exception.ResourceNotFoundException;
import com.smartlib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        ));
    }

    public User getUserByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with email: " + email
                        ));
    }

    public long getUserCount() {
        return userRepository.count();
    }

    public void deactivateUser(Long id) {

        User user = getUserById(id);

        user.setActive(false);

        userRepository.save(user);
    }

    public void activateUser(Long id) {

        User user = getUserById(id);

        user.setActive(true);

        userRepository.save(user);
    }
}