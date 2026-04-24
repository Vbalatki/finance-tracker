package com.finance.finance_tracker.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDto {
    private Long id;

    @NotBlank(message = "Название роли не может быть пустым")
    @Pattern(regexp = "^[A-Z_]+$", message = "Название роли должно содержать только заглавные буквы и подчёркивания")
    @Size(min = 2, max = 50, message = "Название роли должно быть от 2 до 50 символов")
    private String name;

    public String getDisplayName() {
        return name != null ? name.replace("ROLE_", "") : "";
    }
}
