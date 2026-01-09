package com.pulse.service.user;

import com.pulse.dto.user.UserInfoResponse;
import com.pulse.entity.user.User;
import com.pulse.exception.user.UserNotFoundException;
import com.pulse.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        return UserInfoResponse.of(user);
    }
}
