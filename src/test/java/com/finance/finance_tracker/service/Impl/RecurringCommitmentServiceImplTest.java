package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.dto.RecurringCommitmentDto;
import com.finance.finance_tracker.entity.RecurringCommitment;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.entity.enums.TransactionType;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.mapper.RecurringCommitmentMapper;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.RecurringCommitmentRepository;
import com.finance.finance_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringCommitmentServiceImplTest {

    @Mock private RecurringCommitmentRepository recurringCommitmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private RecurringCommitmentMapper recurringCommitmentMapper;

    @InjectMocks
    private RecurringCommitmentServiceImpl service;

    private User user;
    private RecurringCommitmentDto dto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        dto = new RecurringCommitmentDto();
        dto.setName("Netflix");
        dto.setAmount(new BigDecimal("799.00"));
        dto.setType(TransactionType.EXPENSE);
        dto.setDayOfMonth(15);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("создаёт новое плановое списание")
        void save_create_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(recurringCommitmentRepository.save(any(RecurringCommitment.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(recurringCommitmentMapper.toDto(any(RecurringCommitment.class))).thenReturn(dto);

            service.save(dto, 1L);

            verify(recurringCommitmentRepository).save(any(RecurringCommitment.class));
        }

        @Test
        @DisplayName("бросает AccessDeniedException при попытке изменить чужое списание")
        void save_update_notOwner_throws() {
            User otherUser = new User();
            otherUser.setId(999L);

            RecurringCommitment existing = new RecurringCommitment();
            existing.setId(10L);
            existing.setUser(otherUser);

            dto.setId(10L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(recurringCommitmentRepository.findById(10L)).thenReturn(Optional.of(existing));

            assertThrows(AccessDeniedException.class, () -> service.save(dto, 1L));
            verify(recurringCommitmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает EntityNotFoundException, если пользователь не найден")
        void save_userNotFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.save(dto, 1L));
        }
    }

    @Nested
    @DisplayName("toggleActive")
    class ToggleActive {

        @Test
        @DisplayName("переключает статус активности своим владельцем")
        void toggleActive_success() {
            RecurringCommitment commitment = new RecurringCommitment();
            commitment.setId(5L);
            commitment.setUser(user);
            commitment.setActive(true);

            when(recurringCommitmentRepository.findById(5L)).thenReturn(Optional.of(commitment));

            service.toggleActive(5L, 1L);

            assertThat(commitment.isActive()).isFalse();
        }

        @Test
        @DisplayName("бросает AccessDeniedException для чужого списания")
        void toggleActive_notOwner_throws() {
            User otherUser = new User();
            otherUser.setId(999L);

            RecurringCommitment commitment = new RecurringCommitment();
            commitment.setId(5L);
            commitment.setUser(otherUser);

            when(recurringCommitmentRepository.findById(5L)).thenReturn(Optional.of(commitment));

            assertThrows(AccessDeniedException.class, () -> service.toggleActive(5L, 1L));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("удаляет списание своим владельцем")
        void delete_success() {
            RecurringCommitment commitment = new RecurringCommitment();
            commitment.setId(5L);
            commitment.setUser(user);

            when(recurringCommitmentRepository.findById(5L)).thenReturn(Optional.of(commitment));

            service.delete(5L, 1L);

            verify(recurringCommitmentRepository).delete(commitment);
        }

        @Test
        @DisplayName("бросает AccessDeniedException для чужого списания")
        void delete_notOwner_throws() {
            User otherUser = new User();
            otherUser.setId(999L);

            RecurringCommitment commitment = new RecurringCommitment();
            commitment.setId(5L);
            commitment.setUser(otherUser);

            when(recurringCommitmentRepository.findById(5L)).thenReturn(Optional.of(commitment));

            assertThrows(AccessDeniedException.class, () -> service.delete(5L, 1L));
            verify(recurringCommitmentRepository, never()).delete(any());
        }
    }
}