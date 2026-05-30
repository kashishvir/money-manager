package com.BuildMoneyManager.MoneyManager.controller;

import com.BuildMoneyManager.MoneyManager.dto.AiRequestDTO;
import com.BuildMoneyManager.MoneyManager.dto.AiResponseDTO;
import com.BuildMoneyManager.MoneyManager.service.AiAdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiAdvisorController {

    private final AiAdvisorService aiAdvisorService;

    @PostMapping("/advisor")
    public ResponseEntity<AiResponseDTO> getSavingInsights(@RequestBody AiRequestDTO requestDTO) {
        AiResponseDTO response = aiAdvisorService.getSavingInsights(requestDTO);
        return ResponseEntity.ok(response);
    }
}
