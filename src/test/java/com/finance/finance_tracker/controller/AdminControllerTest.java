package com.finance.finance_tracker.controller;

import com.finance.finance_tracker.DTO.AuditDto;
import com.finance.finance_tracker.DTO.RoleDto;
import com.finance.finance_tracker.DTO.UserDto;
import com.finance.finance_tracker.service.AuditService;
import com.finance.finance_tracker.service.RoleService;
import com.finance.finance_tracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Тесты {@link AdminController}. Доступ к /admin/** ограничивается на уровне
 * SecurityConfig (hasRole("ADMIN")), сам контроллер это не проверяет —
 * поэтому SecurityContext здесь не требуется.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private RoleService roleService;
    @Mock
    private UserService userService;
    @Mock
    private AuditService auditService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminController controller = new AdminController(roleService, userService, auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /admin возвращает дашборд со списком пользователей, ролей и логов")
    void dashboard_returnsDashboardView() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(new UserDto()));
        when(roleService.findAll()).thenReturn(List.of(new RoleDto()));
        when(auditService.getRecentLogs(50)).thenReturn(List.of(new AuditDto()));

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @DisplayName("GET /admin/audit возвращает постраничный журнал аудита")
    void auditLogs_returnsAuditView() throws Exception {
        Page<AuditDto> page = new PageImpl<>(List.of(new AuditDto()));
        when(auditService.getAuditLogs(0, 50)).thenReturn(page);

        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit"));
    }

    @Test
    @DisplayName("POST /admin/roles/create с валидным именем создаёт роль и редиректит")
    void createRole_valid_redirects() throws Exception {
        mockMvc.perform(post("/admin/roles/create").param("name", "MANAGER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(roleService).create(any(RoleDto.class));
    }

    @Test
    @DisplayName("POST /admin/roles/create с некорректным именем не создаёт роль и пишет flash-ошибку")
    void createRole_invalidName_setsFlashErrorAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/admin/roles/create").param("name", "invalid lowercase"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("error"));

        verify(roleService, org.mockito.Mockito.never()).create(any());
    }

    @Test
    @DisplayName("POST /admin/users/{id}/roles назначает роли и редиректит")
    void updateUserRoles_success_redirects() throws Exception {
        mockMvc.perform(post("/admin/users/1/roles").param("roles", "2", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).assignRoles(eq(1L), any(List.class));
    }

    @Test
    @DisplayName("POST /admin/users/{id}/toggle переключает активность пользователя")
    void toggleUserActive_success_redirects() throws Exception {
        mockMvc.perform(post("/admin/users/1/toggle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).toggleActive(1L);
    }

    @Test
    @DisplayName("POST /admin/users/{id}/delete удаляет пользователя")
    void deleteUser_success_redirects() throws Exception {
        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).deleteUser(1L);
    }

    @Test
    @DisplayName("POST /admin/roles/{id}/delete при ошибке (например, стандартная роль) пишет flash-ошибку")
    void deleteRole_serviceThrows_setsFlashError() throws Exception {
        doThrow(new RuntimeException("Нельзя удалять стандартные роли"))
                .when(roleService).delete(anyLong());

        mockMvc.perform(post("/admin/roles/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("error"));
    }
}
