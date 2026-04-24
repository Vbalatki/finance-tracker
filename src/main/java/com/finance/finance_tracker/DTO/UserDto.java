package com.finance.finance_tracker.DTO;

import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class UserDto {
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Surname cannot be blank")
    @Size(min = 2, max = 255, message = "Surname must not exceed 255 characters")
    private String surname;

    @NotNull
    @Past(message = "Date of birth must be in the past")
    private LocalDate birthday;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 255, message = "Password must be 8-255 characters")
    private String password;

    private boolean active = true;

    private List<Account> accounts;

    private List<Category> categories;

    private Set<RoleDto> roles;
}
