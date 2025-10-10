package com.BuildMoneyManager.MoneyManager.service;

import com.BuildMoneyManager.MoneyManager.entity.ProfileEntity;
import com.BuildMoneyManager.MoneyManager.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AppUserDetailService implements UserDetailsService {

    private final ProfileRepository profileRepository;

//    Return user detials back to spring security
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        ProfileEntity existingUser = profileRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Profile is not found with email:" + email));
        return User.builder()
                .username(existingUser.getEmail())
                .password(existingUser.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }

}
