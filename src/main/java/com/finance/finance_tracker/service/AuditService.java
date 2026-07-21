package com.finance.finance_tracker.service;

import com.finance.finance_tracker.DTO.AuditDto;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Журнал аудита действий пользователей (создание/изменение/удаление сущностей).
 * Записи создаются автоматически через
 * {@link com.finance.finance_tracker.aspect.AuditAspect} для методов save/update/delete
 * сервисного слоя — вызывать {@link #log} напрямую из бизнес-кода обычно не требуется.
 */
public interface AuditService {

    /**
     * Сохраняет запись аудита. Выполняется асинхронно
     * ({@link org.springframework.scheduling.annotation.Async}). Если любое
     * из обязательных полей ({@code userId}, {@code username}, {@code action},
     * {@code entityType}) не заполнено — запись молча пропускается (не
     * бросает исключение, так как аудит не должен ронять основной поток
     * выполнения).
     *
     * @param userId     id пользователя, выполнившего действие
     * @param username   имя/email пользователя
     * @param action     действие, например {@code CREATE}/{@code UPDATE}/{@code DELETE}
     * @param entityType тип затронутой сущности, например {@code Account}
     * @param entityId   id затронутой сущности, может быть {@code null}
     * @param details    произвольные детали действия (аргументы, результат, ошибка)
     */
    void log(Long userId, String username, String action, String entityType, Long entityId, String details);

    /**
     * Возвращает все записи аудита без пагинации.
     *
     * @return список всех записей аудита
     */
    List<AuditDto> getAllAudits();

    /**
     * Возвращает последние {@code limit} записей аудита, отсортированные
     * по дате создания (сначала новые).
     *
     * @param limit максимальное количество записей, должно быть положительным
     * @return список последних записей аудита
     * @throws com.finance.finance_tracker.exception.InvalidDataException если {@code limit <= 0}
     */
    List<AuditDto> getRecentLogs(int limit);

    /**
     * Возвращает страницу записей аудита, отсортированную по дате создания
     * (сначала новые).
     *
     * @param page номер страницы, начиная с 0
     * @param size размер страницы, должен быть положительным
     * @return страница записей аудита
     * @throws com.finance.finance_tracker.exception.InvalidDataException если {@code page < 0} или {@code size <= 0}
     */
    Page<AuditDto> getAuditLogs(int page, int size);
}
