package com.finance.finance_tracker.service;

import java.time.LocalDateTime;

public interface BankImportService {

    /**
     * Создаёт новый Account, привязанный к внешнему банковскому счёту,
     * без импорта истории — сама синхронизация запускается отдельно.
     *
     * * @throws com.finance.finance_tracker.exception.InvalidDataException
     *         если bankCode не входит в список поддерживаемых банков
     *
     * @throws com.finance.finance_tracker.exception.DuplicateEntityException
     *         если этот банковский счёт уже привязан к другому Account
     */
    Long linkAccount(Long userId, String bankCode, String externalAccountNumber, String accountName,
                     com.finance.finance_tracker.entity.enums.Currency currency);

    /**
     * Синхронизирует транзакции привязанного счёта за период. Уже
     * импортированные ранее операции (по externalId) пропускаются —
     * безопасно вызывать повторно с пересекающимися периодами.
     *
     * @return сколько новых транзакций реально добавлено
     * @throws com.finance.finance_tracker.exception.InvalidDataException
     *         если счёт не привязан ни к какому банку
     */
    int syncTransactions(Long accountId, LocalDateTime from, LocalDateTime to);
}