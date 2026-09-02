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

---

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

---

## 2026-09-01 — фикс задвоенного префикса /profile/profile/settings (🟣)

**Что делали:** UserController висит на @RequestMapping("/profile"),
а settingsPage/updateTheme/updateDefaultCurrency дополнительно
указывали "/profile/settings..." в своих @GetMapping/@PostMapping —
итоговый путь получался /profile/profile/settings(...), а не
/profile/settings(...). Ссылка "Настройки" в шапке и оба AJAX-запроса
в users/settings.html (смена темы, смена валюты) уже указывали на
правильный /profile/settings... — то есть баг был исключительно в
контроллере, шаблоны трогать не пришлось.

**Пропатчено:**
1. UserController — убран задвоенный префикс "/profile" в трёх
   маппингах (settingsPage, updateTheme, updateDefaultCurrency).
2. UserControllerTest — добавлены 4 теста: GET /profile/settings,
   регрессионная проверка именно на путь без задвоения, POST .../theme
   и POST .../currency с проверкой аргументов через ArgumentCaptor.

**Не тронуто:** header.html, users/settings.html — пути там были
верными изначально.

**Не проверено:** mvn verify не запускался (нет сети до Maven Central
в песочнице) — правки вычитаны вручную.

---

## 2026-09-01 — фикс двойной загрузки Bootstrap JS ломавшей dropdown в админке (🟣)

**Что делали:** по репорту "не открывается маленькое модальное окно с
настройками/логаутом в админ-панели" нашли причину: admin/dashboard.html
подключал Bootstrap JS дважды — один раз через layout/footer.html
(версия 5.3.8, совпадает с CSS из head.html), второй раз явным
<script> в хвосте файла (версия 5.1.3). Bootstrap 5 регистрирует
делегированный обработчик клика на data-bs-toggle при каждом
выполнении своего скрипта — при двойной загрузке обработчик
навешивается дважды, из-за чего клик открывает и тут же закрывает
элемент (open + close в одном клике). Затрагивало и user-dropdown в
шапке (Профиль/Настройки/Выход), и модалки "Назначить роли" на той же
странице — оба используют data-bs-toggle.
Тот же паттерн (та же пара версий, тот же дубль) нашёлся в
transactions/create.html — там симптом не проявлялся (на странице нет
dropdown/modal), но баг идентичный, поправлено заодно.

**Пропатчено:**
1. admin/dashboard.html — убран дублирующий <script> с bootstrap 5.1.3,
   оставлен только footer (5.3.8).
2. transactions/create.html — убран тот же дублирующий <script>.

**Не тронуто (отдельные тикеты на будущее):**
- accounts/detail.html, accounts/edit.html — footer не подключён
  вообще, страницы работают на голом Bootstrap 5.1.3 без футера сайта.
- accounts/create.html — сверх того сломана структура HTML: </body>
  закрывается сразу после header, весь контент формы формально вне
  body, плюс повторный </body></html> в конце файла. Браузеры это
  проглатывают через авто-коррекцию парсера, но структура невалидна и
  требует отдельного исправления.

**Не проверено:** mvn verify не запускался (нет доступа к Maven
Central в песочнице); визуальная проверка dropdown/модалок в реальном
браузере после патча тоже не проводилась — вычитано только по логике
двойной регистрации обработчиков Bootstrap.

---

## 2026-09-01 — фикс отсутствующего footer / сломанной структуры HTML
в accounts/detail.html и accounts/create.html (🟣, продолжение находки
про двойную загрузку Bootstrap в admin/dashboard.html)

**Что делали:** после фикса двойной загрузки Bootstrap JS в
admin/dashboard.html/transactions/create.html прошлись по остальным
шаблонам accounts/* на тот же паттерн (явный <script> с bootstrap
5.1.3 вместо/вместе с layout/footer, который тащит 5.3.8). Нашли два
разных дефекта:

1. accounts/detail.html — footer вообще не подключался, страница
   работала на отдельном голом Bootstrap 5.1.3 без сайтового футера
   и без версии, совпадающей с CSS в head.html (5.3.8).
2. accounts/create.html — сверх того сломана структура: </body>
   закрывался сразу после header (весь контент формы формально шёл
   вне body), плюс дублирующийся Bootstrap-скрипт и повторный
   </body></html> в конце файла. Браузеры проглатывали это через
   авто-коррекцию парсера, поэтому визуально страница не разваливалась,
   но HTML был невалиден.

**Пропатчено:**
1. accounts/detail.html — заменён <script bootstrap@5.1.3> на
   <div th:replace="~{layout/footer :: footer}"></div>.
2. accounts/create.html — убран преждевременный </body> после header,
   убран дублирующий script, добавлен footer перед единственным
   финальным </body>.

**Передано другому агенту (🔵):** accounts/edit.html — тот же паттерн
(голый bootstrap 5.1.3 вместо footer), но этот файл уже в работе у 🔵
в этом же раунде по другой задаче (hidden-поля userId/balance) — фикс
передан ему как дополнение к текущему диффу, чтобы не редактировать
файл параллельно из двух зон.

**Не проверено:** mvn verify не запускался (нет сети до Maven Central
в песочнице); визуальная проверка в браузере тоже не проводилась —
структура HTML проверена вручную построчно.

---

## 2026-09-01 — фикс падения /budgets (ArithmeticException) + находка N+1
в RecurringCommitment (🟣)

**Что делали:** budgets/list.html содержал вложенный дублирующийся
progress-bar div. Внешний считал ширину безопасно через
BigDecimal.doubleValue(), внутренний — напрямую через BigDecimal-
арифметику в SpEL (currentSpending * 100 / monthlyLimit) без
scale/rounding. BigDecimal.divide(BigDecimal) без явного scale
бросает ArithmeticException на любом нецелом делении без конечной
десятичной дроби (например 1000.00/3000.00) — то есть страница
/budgets реально падала 500-кой почти для любой пары "потрачено/
лимит", кроме кратных чисел.

**Пропатчено:** budgets/list.html — убран дублирующий внутренний
progress-bar div, оставлен только безопасный внешний расчёт.

**Важный гэп, зафиксированный, не исправленный сегодня:**
BudgetControllerTest (standalone MockMvc) НЕ может поймать этот
класс багов — standalone-сборка не рендерит реальные Thymeleaf-
шаблоны, только имя view. Баг дожил до продакшена именно поэтому.
Полноценная защита требует @SpringBootTest с реальным рендерингом —
отдельная по объёму задача, требует поднятия контекста с БД, не
делалась в рамках этого патча.

**Передано 🟢 как доп. задача:** RecurringCommitmentRepository.
findByUserIdOrderByDayOfMonth фетчит r.category, но не r.account —
N+1 на account при маппинге RecurringCommitmentDto для каждого
планового платежа. Тот же класс проблемы, что уже чинили в Budget/
Transaction/BankImport ранее, пропущен для этого репозитория.

**Не проверено:** mvn verify не запускался (нет сети до Maven
Central в песочнице) — правки вычитаны вручную, включая арифметику
BigDecimal.divide (проверено по JavaDoc поведения, не runtime).

---

## 2026-09-01 — фикс валидации bankCode при привязке + сверка 6 внешних
находок, 2 из которых не подтвердились (🟣)

**Проверка входного списка находок:** получен список из 6 пунктов.
Два — password_reset_tokens (таблица/чистка токенов) и
LoginAttemptService (rate limiting) — ссылаются на код, которого в
проекте НЕТ. Ни одной Liquibase-миграции, ни сущности, ни контроллера
под это не существует; README прямо подтверждает отсутствие обеих
фич в разделе "Известные ограничения". Задачи под них не заведены.
Если это фичи из более свежей версии кода — нужен актуальный срез,
не пересказ.

**Пропатчено (bankCode validation):**
1. BankImportServiceImpl — добавлена проверка bankCode против
   SUPPORTED_BANK_CODES (TBANK, ALFA) в начале linkAccount(), до
   похода в БД. Раньше проверка существовала только в
   resolveConnector() на этапе syncTransactions — можно было создать
   счёт с произвольным bankCode, который никогда не засинкается.
2. BankImportService — javadoc дополнен новым @throws.
3. BankImportServiceImplTest — 2 новых теста (unknown/null bankCode).
   BankIntegrationController менять не пришлось — уже оборачивает
   вызов в try/catch и показывает e.getMessage() как flash-ошибку.

**Передано с уточнением (не как есть):**
- CurrencyController.convert() → 🤎. Исходная формулировка неточна:
  для `to` защита УЖЕ есть (IllegalArgumentException -> 400,
  протестировано). Реальная проблема — в `from`: мусорное значение
  не крашится, а тихо проваливается в фолбэк на статические курсы
  (catch(Exception) в getExchangeRates), конвертация выполняется по
  курсам, не связанным с переданным from — silent wrong answer, не
  crash. Задача передана с этим уточнением.
- CalendarController year/month → 🟢, доп. пункт к их текущему списку.
- RecurringCommitmentDto.userId → 🔵, с явным указанием, что удаление
  требует правки RecurringCommitmentMapper + существующего теста,
  не тривиальный one-file fix.

**Не проверено:** mvn verify не запускался (нет сети до Maven Central
в песочнице) — правки вычитаны вручную.