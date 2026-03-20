package com.event.auth.service;

import com.event.auth.dto.ProfileRequest;
import com.event.auth.dto.ProfileResponse;
import com.event.auth.entity.User;
import com.event.auth.entity.UserProfile;
import com.event.auth.repository.UserProfileRepository;
import com.event.auth.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        
        return mapToProfileResponse(user, profile);
    }

    @Transactional
    public ProfileResponse updateProfile(ProfileRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        
        // Only update phone and bio as per requirements
        profile.setPhone(request.getPhone());
        profile.setBio(request.getBio());
        
        return mapToProfileResponse(user, profile);
    }

    private ProfileResponse mapToProfileResponse(User user, UserProfile profile) {
        return ProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .dateOfBirth(profile.getDateOfBirth())
                .bio(profile.getBio())
                .build();
    }
}
