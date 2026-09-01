# Audit / Patch Log

Журнал ревью и патчей от разных ИИ-агентов, работающих над проектом без
единой синхронизации между собой. Каждая запись — что проверялось, что
нашли, что поправили. Перед новыми правками сверяйтесь с последней записью,
а не только со своим собственным контекстом диалога.

---

## 2026-08-13 — сверка чек-листа IDOR / N+1 / soft-delete фиксов + патч

**Что делали:** построчно сверили фактическое состояние кода с чек-листом
из 19 пунктов (IDOR/mass assignment, N+1, soft delete, maven-failsafe и
т.д.), не доверяя прошлым описаниям. Затем пропатчили все найденные
расхождения. Полный текст чек-листа и построчные вердикты — в истории
диалога с пользователем, здесь только итог по коду.

**Результат сверки:** 16 из 19 пунктов уже были реализованы верно,
3 — частично, 0 — отсутствовали полностью.

**Пропатчено в этой сессии:**

1. `AccountDto.userId` — убран `@NotNull`. Поле никогда не должно приходить
   от клиента (userId ставится в `AccountController.createAccount` из
   SecurityContext). `@NotNull` был опасен тем, что `@Valid` отрабатывает
   ДО тела метода — валидация упала бы раньше, чем контроллер успевал
   проставить userId, если бы hidden-поле в форме отсутствовало.
2. `templates/accounts/create.html` — убран
   `<input type="hidden" th:field="*{userId}">`. userId больше нигде не
   приходит от клиента при создании счёта.
3. `AccountController.createAccountPage` — убран мёртвый пре-филл
   `accountDto.setUserId(...)`, был нужен только для убранного hidden-поля.
4. `AccountController.deleteAccount` — дублирующая inline-проверка владения
   заменена на `SecurityUtil.requireOwnership(account)`, как во всех
   остальных методах контроллера.
5. `AuditAspect.determineAction()` — убрана мёртвая ветка
   `"resetSpending".equals(methodName)`.
6. `AuditAspectTest.java` — убран `@Nested class ResetSpendingAction`,
   тестировавший уже удалённый отовсюду метод.
7. `BudgetServiceImpl.java` (метод `getBudgetsByUserId`) — убран
   комментарий-огрызок инструкции по патчу ("заменить блок начиная с..."),
   оставшийся от чьей-то чужой правки; заменён на обычное описание кода.
8. `SecurityConfig.java` — убран мёртвый закомментированный бин
   `successHandler()` (нигде не подключался к `formLogin`) + ставшие
   неиспользуемыми импорты (`GrantedAuthority`, `AuthenticationSuccessHandler`,
   `Set`, `Collectors`).
9. `Account.java` — в `@SQLDelete` добавлен `check = ResultCheckStyle.COUNT`.
   Без него условие `AND version = ?` в кастомном SQL ничего не защищало:
   по умолчанию Hibernate не проверяет, сколько строк реально обновил
   `@SQLDelete`, так что гонка "удалить счёт с устаревшим version" тихо
   прошла бы как no-op вместо `ObjectOptimisticLockingFailureException`.
   Это НЕ было в исходном чек-листе — доп. находка при ревью soft-delete.
10. `AccountOptimisticLockingIT.java` — добавлен тест
    `concurrentDelete_staleVersion_throwsInsteadOfSilentNoOp`, закрывает
    пункт 9 (delete-путь раньше вообще не был покрыт тестами, только save()).

**Не проверено / не гарантировано:**
- `mvn compile` / `test` / `verify` в этой сессии НЕ запускались — sandbox
  без доступа к Maven Central. Все правки вычитаны вручную (+ проверен
  баланс скобок), но не скомпилированы и не прогнаны. Перед мержем нужен
  обычный `mvn verify` от агента/CI с доступом к сети.
- Новый тест `concurrentDelete_staleVersion_throwsInsteadOfSilentNoOp`
  написан по аналогии с существующим `concurrentUpdate_...`, но ни разу не
  выполнялся — перепроверить его в первую очередь.

**Не тронуто намеренно (вне периметра задачи):**
- `templates/accounts/edit.html` тоже содержит
  `<input type="hidden" th:field="*{userId}">`, но `AccountServiceImpl.updateAccount`
  это поле вообще не читает (использует `account.getUser().getId()` из уже
  загруженной сущности) — реальной дыры там нет, трогать не стали, чтобы не
  расширять диапазон правки без запроса.

## 2026-09-01 — фикс сломанного роутинга /recurring + тест контроллера (🟣)

**Что делали:** recurring/list.html и recurring/form.html ссылались на
несуществующий путь /templates/recurring/... вместо /recurring/... —
контроллер RecurringCommitmentController замаплен на /recurring без
префикса. Кнопки "Добавить", toggle (пауза/плей), "Удалить" вели в 404.
Фича не была покрыта тестами вообще — RecurringCommitmentControllerTest
не существовал, поэтому регрессия не ловилась.

**Пропатчено:**
1. recurring/list.html — 3 ссылки/action поправлены на /recurring/...
2. recurring/form.html — form action и cancel-ссылка поправлены на /recurring
3. Добавлен RecurringCommitmentControllerTest — покрывает list/create/
   save (valid+invalid)/toggle (success+error)/delete (success+error)

**Не тронуто:** RecurringCommitmentService/Impl, RecurringCommitmentDto —
сама бизнес-логика была рабочей, ломался только фронт.

**Не проверено:** mvn verify не запускался (нет сети до Maven Central в
песочнице) — правки вычитаны вручную.