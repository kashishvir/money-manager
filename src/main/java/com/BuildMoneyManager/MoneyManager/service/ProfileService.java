package com.BuildMoneyManager.MoneyManager.service;

import com.BuildMoneyManager.MoneyManager.dto.AuthDTO;
import com.BuildMoneyManager.MoneyManager.dto.ProfileDTO;
import com.BuildMoneyManager.MoneyManager.entity.ProfileEntity;
import com.BuildMoneyManager.MoneyManager.repository.ProfileRepository;
import com.BuildMoneyManager.MoneyManager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${app.activation.url}")
    private String activationURL;

    public ProfileDTO registerProfile(ProfileDTO profileDTO) {
        log.info("🚀 Starting registration for email: {}", profileDTO.getEmail());

        ProfileEntity newProfile = dtoToEntity(profileDTO);
        // Automatically activate the account without email verification
        newProfile.setIsActive(true);

        try {
            newProfile = profileRepository.save(newProfile);
            log.info("✅ Profile saved successfully with ID: {}", newProfile.getId());
        } catch (Exception e) {
            log.error("❌ Error while saving profile: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register profile. Email might already exist.", e);
        }

        return EntitytoDto(newProfile);
    }

    public ProfileEntity dtoToEntity(ProfileDTO ProfileDTO){
        return ProfileEntity.builder()
                .id(ProfileDTO.getId())
                .fullName(ProfileDTO.getFullName())
                .email(ProfileDTO.getEmail())
                .password(passwordEncoder.encode(ProfileDTO.getPassword()))
                .profileImageUrl(ProfileDTO.getProfileImageUrl())
                .createdAt(ProfileDTO.getCreatedAt())
                .updatedAt(ProfileDTO.getUpdatedAt())
                .build();
    }

    public ProfileDTO EntitytoDto(ProfileEntity profileEntity){
        return ProfileDTO.builder()
                .id(profileEntity.getId())
                .fullName(profileEntity.getFullName())
                .email(profileEntity.getEmail())
                .password(profileEntity.getPassword())
                .profileImageUrl(profileEntity.getProfileImageUrl())
                .createdAt(profileEntity.getCreatedAt())
                .updatedAt(profileEntity.getUpdatedAt())
                .build();
    }

    public void deleteProfileByEmail(String email) {
        profileRepository.deleteByEmail(email);
    }

    public boolean activationProfile(String activationToken){
        log.info("🔑 Activation request received for token: {}", activationToken);
        return profileRepository.findByActivationToken(activationToken)
                .map(profile -> {
                    profile.setIsActive(true);
                    profileRepository.save(profile);
                    log.info("✅ Account activated for email: {}", profile.getEmail());
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("⚠️ Invalid activation token received");
                    return false;
                });
    }

    public boolean isAccountActive(String email){
        log.info("Checking if account is active for email: {}", email);
        return profileRepository.findByEmail(email)
                .map(ProfileEntity::getIsActive)
                .orElse(false);
    }

    public ProfileEntity getCurrentProfile(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Fetching current profile for authenticated user: {}", authentication.getName());
        return profileRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email:" + authentication.getName()));
    }

    public ProfileDTO getPublicProfile(String email){
        ProfileEntity currUser;
        if(email == null){
            currUser = getCurrentProfile();
        } else {
            currUser = profileRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + email));
        }
        return ProfileDTO.builder()
                .id(currUser.getId())
                .fullName(currUser.getFullName())
                .email(currUser.getEmail())
                .profileImageUrl(currUser.getProfileImageUrl())
                .createdAt(currUser.getCreatedAt())
                .updatedAt(currUser.getUpdatedAt())
                .build();
    }

    public Map<String, Object> authenticateAndGeneratetoken(AuthDTO authDTO){
        log.info("🔐 Authentication attempt for email: {}", authDTO.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword())
            );
            log.info("✅ Authentication successful for {}", authDTO.getEmail());

            // Check activation status
            if (!isAccountActive(authDTO.getEmail())) {
                log.warn("🚫 Account is not active for {}", authDTO.getEmail());
                throw new RuntimeException("Account is not active. Please activate your account first.");
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(authDTO.getEmail());
            log.info("🎟️ JWT token generated for {}", authDTO.getEmail());

            return Map.of(
                    "token", token,
                    "user", getPublicProfile(authDTO.getEmail())
            );
        } catch (Exception e) {
            log.error("❌ Authentication failed for {}: {}", authDTO.getEmail(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
