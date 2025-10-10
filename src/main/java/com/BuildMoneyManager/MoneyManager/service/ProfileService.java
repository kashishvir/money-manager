package com.BuildMoneyManager.MoneyManager.service;

import com.BuildMoneyManager.MoneyManager.dto.AuthDTO;
import com.BuildMoneyManager.MoneyManager.dto.ProfileDTO;
import com.BuildMoneyManager.MoneyManager.entity.ProfileEntity;
import com.BuildMoneyManager.MoneyManager.repository.ProfileRepository;
import com.BuildMoneyManager.MoneyManager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${app.activation.url}")
    private String activationURL;

    public ProfileDTO registerProfile(ProfileDTO profileDTO){
        ProfileEntity newProfile = dtoToEntity(profileDTO);
        newProfile.setActivationToken(UUID.randomUUID().toString());
//        newProfile.setPassword(passwordEncoder.encode(newProfile.getPassword()));
        newProfile = profileRepository.save(newProfile);
        profileDTO = EntitytoDto(newProfile);

        //send Activation Link
        String activationLink = activationURL+"/api/v1.0/activate?token="+newProfile.getActivationToken();
        String subject = "Activate your Money Manager Account";
        String body = "Hi "+ newProfile.getFullName()+", Click on the following link to activate youe account:"+activationLink;
        emailService.sendEmail(newProfile.getEmail(), subject, body);
        return profileDTO;
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

    public boolean activationProfile(String activationToken){
        return profileRepository.findByActivationToken(activationToken)
                .map(profile->{
                    profile.setIsActive(true);
                    profileRepository.save(profile);
                    return true;
                })
                .orElse(false);
    }

    public boolean isAccountActive(String email){
        return profileRepository.findByEmail(email)
                .map(ProfileEntity::getIsActive)
                .orElse(false);
    }

    public ProfileEntity getCurrentProfile(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return profileRepository.findByEmail(authentication.getName())
                .orElseThrow(()-> new UsernameNotFoundException("Profile not found with email:" + authentication.getName()));
    }

    public ProfileDTO getPublicProfile(String email){
        ProfileEntity currUser = null;
        if(email==null){
            currUser = getCurrentProfile();
        }else{
            currUser = profileRepository.findByEmail(email)
                    .orElseThrow(()-> new UsernameNotFoundException("Profile not found with email: "+email));
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
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword()));
            //Generate jwt token
            String token = jwtUtil.generateToken(authDTO.getEmail());
            return Map.of(
                    "token",token,
                    "user", getPublicProfile(authDTO.getEmail())
            );
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
