package com.BuildMoneyManager.MoneyManager.dto;

import com.BuildMoneyManager.MoneyManager.entity.ProfileEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryDTO {
    private Long id;
    private Long profileId;
    private  String name;
    private String type;
    private String icon;
    private LocalDateTime createdAt;
    private  LocalDateTime updatedAt;
}
