package com.finance.finance_tracker.service.impl;

import com.finance.finance_tracker.dto.RecurringCommitmentDto;
import com.finance.finance_tracker.entity.Account;
import com.finance.finance_tracker.entity.Category;
import com.finance.finance_tracker.entity.RecurringCommitment;
import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.mapper.RecurringCommitmentMapper;
import com.finance.finance_tracker.repository.AccountRepository;
import com.finance.finance_tracker.repository.CategoryRepository;
import com.finance.finance_tracker.repository.RecurringCommitmentRepository;
import com.finance.finance_tracker.repository.UserRepository;
import com.finance.finance_tracker.service.RecurringCommitmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.finance.finance_tracker.util.DataConstants.ACCOUNT_NOT_FOUND;
import static com.finance.finance_tracker.util.DataConstants.CATEGORY_NOT_FOUND;
import static com.finance.finance_tracker.util.DataConstants.RECURRING_COMMITMENT_NOT_FOUND;
import static com.finance.finance_tracker.util.DataConstants.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringCommitmentServiceImpl implements RecurringCommitmentService {

    private final RecurringCommitmentRepository recurringCommitmentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final RecurringCommitmentMapper recurringCommitmentMapper;

    @Override
    @Transactional
    public RecurringCommitmentDto save(RecurringCommitmentDto dto, Long userId) {
        log.debug("Сохранение планового списания: name={}, userId={}", dto.getName(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND + ", id: " + userId));

        RecurringCommitment commitment;
        if (dto.getId() != null) {
            commitment = recurringCommitmentRepository.findById(dto.getId())
                    .orElseThrow(() -> new EntityNotFoundException(RECURRING_COMMITMENT_NOT_FOUND + ", id: " + dto.getId()));
            if (!commitment.getUser().getId().equals(userId)) {
                log.warn("Попытка изменить чужое плановое списание: id={}, currentUserId={}", dto.getId(), userId);
                throw new AccessDeniedException("Нет доступа к этому плановому списанию");
            }
        } else {
            commitment = new RecurringCommitment();
        }

        commitment.setUser(user);
        commitment.setName(dto.getName().trim());
        commitment.setAmount(dto.getAmount());
        commitment.setType(dto.getType());
        commitment.setDayOfMonth(dto.getDayOfMonth());
        commitment.setActive(dto.isActive());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException(CATEGORY_NOT_FOUND + ", id: " + dto.getCategoryId()));
            if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Нет доступа к этой категории");
            }
            commitment.setCategory(category);
        } else {
            commitment.setCategory(null);
        }

        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND + ", id: " + dto.getAccountId()));
            if (!account.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Нет доступа к этому счету");
            }
            commitment.setAccount(account);
        } else {
            commitment.setAccount(null);
        }

        RecurringCommitment saved = recurringCommitmentRepository.save(commitment);
        log.info("Сохранено плановое списание: id={}, name={}, userId={}", saved.getId(), saved.getName(), userId);

        return recurringCommitmentMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringCommitmentDto> getByUserId(Long userId) {
        return recurringCommitmentRepository.findByUserIdOrderByDayOfMonth(userId).stream()
                .map(recurringCommitmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleActive(Long id, Long currentUserId) {
        RecurringCommitment commitment = recurringCommitmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RECURRING_COMMITMENT_NOT_FOUND + ", id: " + id));

        if (!commitment.getUser().getId().equals(currentUserId)) {
            log.warn("Попытка изменить статус чужого планового списания: id={}, currentUserId={}", id, currentUserId);
            throw new AccessDeniedException("Нет доступа к этому плановому списанию");
        }

        commitment.setActive(!commitment.isActive());
        recurringCommitmentRepository.save(commitment);
        log.info("Плановое списание id={} переключено: active={}", id, commitment.isActive());
    }

    @Override
    @Transactional
    public void delete(Long id, Long currentUserId) {
        RecurringCommitment commitment = recurringCommitmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RECURRING_COMMITMENT_NOT_FOUND + ", id: " + id));

        if (!commitment.getUser().getId().equals(currentUserId)) {
            log.warn("Попытка удалить чужое плановое списание: id={}, currentUserId={}", id, currentUserId);
            throw new AccessDeniedException("Нет доступа к этому плановому списанию");
        }

        recurringCommitmentRepository.delete(commitment);
        log.info("Удалено плановое списание id={}", id);
    }
}