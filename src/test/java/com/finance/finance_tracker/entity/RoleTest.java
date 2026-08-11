package com.finance.finance_tracker.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контрактный тест, не диагноз текущего состояния файла: если оба теста
 * зелёные — Role.equals/hashCode уже приведены к id-based паттерну,
 * применённому во всех остальных сущностях проекта. Если второй тест
 * падает — Role по-прежнему использует Lombok @Data equals по всем полям,
 * и мутация name в RoleServiceImpl.update() после вставки роли в
 * User.roles (HashSet) ломает contains()/remove() для этого объекта.
 */
class RoleTest {

    @Test
    @DisplayName("equals — только по id (как у остальных сущностей)")
    void equals_sameId_equalRegardlessOfName() {
        Role a = new Role(); a.setId(1L); a.setName("ROLE_MANAGER");
        Role b = new Role(); b.setId(1L); b.setName("ROLE_SENIOR_MANAGER");

        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("мутация name после добавления в HashSet не должна ломать contains()")
    void mutatingNameAfterHashSetInsertion_doesNotBreakContains() {
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_MANAGER");

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        role.setName("ROLE_SENIOR_MANAGER"); // то же, что делает RoleServiceImpl.update()

        assertThat(roles).contains(role);
    }
}
