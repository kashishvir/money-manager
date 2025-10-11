package com.BuildMoneyManager.MoneyManager.controller;

import com.BuildMoneyManager.MoneyManager.dto.AuthDTO;
import com.BuildMoneyManager.MoneyManager.dto.ProfileDTO;
import com.BuildMoneyManager.MoneyManager.entity.ProfileEntity;
import com.BuildMoneyManager.MoneyManager.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/register")
    public ResponseEntity<ProfileDTO> registerProfile(@RequestBody ProfileDTO profileDTO){
        ProfileDTO registeredProfile = profileService.registerProfile(profileDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredProfile);
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activationProfile(@RequestParam String token){
        boolean isActived = profileService.activationProfile(token);

        if(isActived){
            return ResponseEntity.ok("Profile is Activated Successfully");
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Activation token not found or already used.");
        }
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<String> deleteProfile(@PathVariable String email) {
        profileService.deleteProfileByEmail(email);
        return ResponseEntity.ok("Profile deleted successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(@RequestBody AuthDTO authDTO){
        try {
            if(!profileService.isAccountActive(authDTO.getEmail())){
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("message", "Account is not active. Please activate your account first.")
                );
            }
            Map<String,Object> response = profileService.authenticateAndGeneratetoken(authDTO);
            return ResponseEntity.ok(response);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("message",e.getMessage())
            );
        }
    }
}
