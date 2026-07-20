# План перехода с MySQL на PostgreSQL

## Важная находка перед началом

В `pom.xml` **нет зависимости `liquibase-core`**, хотя в проекте есть полноценные
changelog-файлы (`src/main/resources/db/changelog/**`). Это значит, что сегодня
Liquibase **не подключён к приложению вообще** — Spring Boot его не запускает
при старте. Судя по всему, схема в вашей текущей MySQL была создана как-то
иначе (вручную, отдельным прогоном liquibase CLI, или Hibernate когда-то
создал её сам). Это надо починить **в любом случае**, независимо от перехода
на Postgres — иначе схема БД не воспроизводима из репозитория. Но для миграции
на Postgres это ещё и хорошая новость: как только Liquibase заработает,
поднять чистую Postgres-схему станет вопросом одной команды, а не ручного
пересоздания таблиц.

Это включено в план как шаг 0.

## Почему миграция в целом должна пройти гладко

- Схема уже описана декларативно через Liquibase YAML (`autoIncrement: true`,
  абстрактные типы `BIGINT`/`VARCHAR`/`DECIMAL`/`BOOLEAN`/`TIMESTAMP`/`TEXT`) —
  Liquibase сам транслирует это в диалект целевой БД, специфичного для MySQL
  синтаксиса (`AUTO_INCREMENT`, backtick-идентификаторы и т.п.) в changelog'ах
  не используется.
- Список таблиц не содержит зарезервированных в PostgreSQL слов
  (`users`, `accounts`, `categories`, `budgets`, `transactions`, `audit`,
  `roles`, `user_roles` — всё безопасно; единственный потенциально спорный
  вариант, `user` в единственном числе, у вас и не используется).
- Денежные суммы — `DECIMAL(19,2)` / Java `BigDecimal` — тип есть в обеих СУБД
  один в один (`numeric(19,2)`).
- Enum-поля (`TransactionType`, `Currency`) хранятся как `@Enumerated(STRING)`
  → обычный `VARCHAR`, СУБД-независимо.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` — Postgres 10+
  поддерживает identity-колонки нативно, Hibernate 6 умеет туда отображать
  эту стратегию корректно.

То есть настоящая работа — это не "переписать всю схему", а аккуратно
пройтись по конфигурации и нескольким местам, которые теоретически могут
повести себя по-разному.

---

## Шаг 0. Подключить Liquibase (сделать один раз, ещё на MySQL)

1. Добавить в `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.liquibase</groupId>
       <artifactId>liquibase-core</artifactId>
   </dependency>
   ```
   (версия подтянется из `spring-boot-starter-parent`, отдельно указывать не нужно).

2. Добавить в `application.yaml`:
   ```yaml
   spring:
     liquibase:
       enabled: true
       change-log: classpath:db/changelog/db.changelog-master.yaml
   ```

3. **На текущей MySQL** (где уже есть данные) — сделать `baseline`, а не
   позволить Liquibase попытаться заново накатить `CREATE TABLE` на
   существующие таблицы:
   ```bash
   mvn liquibase:changelogSync \
     -Dliquibase.url=jdbc:mysql://localhost:3306/finance_tracker \
     -Dliquibase.username=root \
     -Dliquibase.password=root1
   ```
   Это пометит все существующие changeset'ы как "уже применённые" в служебной
   таблице `DATABASECHANGELOG`, ничего физически не меняя. После этого
   перезапустите приложение и убедитесь, что оно стартует без ошибок
   Liquibase — только тогда переходите к шагу 1.

---

## Шаг 1. Добавить поддержку PostgreSQL параллельно с MySQL

Не удаляйте MySQL-зависимость сразу — держите обе, пока не убедитесь, что
Postgres работает.

`pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Шаг 2. Локальная Postgres для проверки

Добавьте в `docker-compose.yml` (или отдельный `docker-compose.postgres.yml`
для параллельного теста, не трогая рабочий MySQL-стенд):

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: finance_tracker
      POSTGRES_USER: finance_tracker
      POSTGRES_PASSWORD: finance_tracker
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:
```

## Шаг 3. Профиль `postgres` в приложении

Не переключайте `application.yaml` напрямую — заведите отдельный профиль,
чтобы можно было гонять оба варианта параллельно и откатиться одной опцией
`--spring.profiles.active`.

`src/main/resources/application-postgres.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/finance_tracker
    username: finance_tracker
    password: finance_tracker
    driver-class-name: org.postgresql.Driver
  jpa:
    properties:
      hibernate:
        # Hibernate 6 умеет автоопределять диалект по JDBC-метаданным,
        # но явное указание чуть быстрее стартует и однозначнее в логах.
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

Запуск для проверки:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Шаг 4. Прогнать Liquibase на чистой Postgres и проверить схему

При старте с профилем `postgres` Liquibase сам применит все changeset'ы
(включая `V4__seed_data.yaml` с тестовыми пользователями/счетами) к пустой
базе. Проверьте:

```bash
docker exec -it <postgres-container> psql -U finance_tracker -d finance_tracker -c "\dt finance_tracker.*"
```

Сверьте список таблиц и количество строк в `users`/`accounts`/`categories`/
`budgets` с тем, что ожидаете из seed-changeset'а.

## Шаг 5. Функциональная проверка приложения на Postgres

Пройдите вручную (или через уже написанные тесты, но те у нас юнит — с
замоканными репозиториями, реальный JDBC они не проверяют) основные сценарии:

- регистрация / логин;
- создание счёта, пополнение/снятие (проверка `BigDecimal`-арифметики и
  сохранения `balance`);
- создание транзакции, пересчёт баланса;
- бюджеты — сохранение и `getCurrentMonthExpenseByCategory` (там кастомный
  `@Query` с `YEAR(...)`/`MONTH(...)` — **это MySQL-специфичные функции**,
  см. отдельный пункт ниже, это единственное реальное место, требующее
  правки кода, а не только конфига);
- админ-панель — список пользователей, аудит.

### ⚠️ Единственное место, которое реально нужно переписать в коде

`TransactionRepository.getCurrentMonthExpenseByCategory`:
```java
@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
        "WHERE t.category.id = :categoryId " +
        "AND t.type = com.finance.finance_tracker.entity.enums.TransactionType.EXPENSE " +
        "AND YEAR(t.createdAt) = YEAR(CURRENT_DATE) " +
        "AND MONTH(t.createdAt) = MONTH(CURRENT_DATE)")
BigDecimal getCurrentMonthExpenseByCategory(@Param("categoryId") Long categoryId);
```
`YEAR(...)`/`MONTH(...)` — это JPQL-функции, транслируемые Hibernate в
диалект-специфичный SQL. На MySQL это маппится на встроенные `YEAR()`/`MONTH()`,
на PostgreSQL таких функций с такими именами нет — на Postgres запрос либо
не скомпилируется, либо (в зависимости от версии Hibernate) упадёт в рантайме.
Замените на диалект-независимый вариант через сравнение диапазона дат:

```java
@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
        "WHERE t.category.id = :categoryId " +
        "AND t.type = com.finance.finance_tracker.entity.enums.TransactionType.EXPENSE " +
        "AND t.createdAt >= :monthStart AND t.createdAt < :monthEnd")
BigDecimal getCurrentMonthExpenseByCategory(
        @Param("categoryId") Long categoryId,
        @Param("monthStart") LocalDateTime monthStart,
        @Param("monthEnd") LocalDateTime monthEnd);
```
и в `BudgetServiceImpl` посчитать границы месяца в Java
(`LocalDate.now().withDayOfMonth(1).atStartOfDay()` и `.plusMonths(1)`)
перед вызовом. Это заодно избавляет от зависимости от того, какой сейчас
часовой пояс у СУБД — сравнение дат в Java предсказуемее, чем `CURRENT_DATE`
на сервере БД.

**Сделайте эту правку и протестируйте её ещё на MySQL до переключения** —
она совместима с обеими СУБД, так что можно исправить заранее, отдельным
маленьким PR, не привязывая к моменту переезда.

## Шаг 6. Данные из прод/дев MySQL (если там уже есть реальные данные)

Если в текущей MySQL только seed-данные из `V4__seed_data.yaml` — просто
дайте Liquibase применить их заново на Postgres (шаг 4), ничего переносить
не нужно.

Если есть настоящие пользовательские данные, которые жалко терять — три
варианта, от простого к надёжному:

1. **pgloader** — специализированный инструмент именно для MySQL → Postgres,
   сам разбирается с типами и even auto_increment → sequence:
   ```bash
   pgloader mysql://root:root1@localhost/finance_tracker \
            postgresql://finance_tracker:finance_tracker@localhost/finance_tracker
   ```
   Плюс: быстро. Минус: нужно проверить результат руками (типы дат,
   кодировки).

2. **Экспорт через приложение** — написать разовый скрипт/эндпоинт, который
   вычитывает все сущности через JPA и пишет их через тот же JPA в Postgres.
   Плюс: гарантированно проходит через вашу бизнес-валидацию. Минус: дольше
   писать, для одной таблицы `audit` может быть медленно при большом объёме.

3. **CSV export/import** — `mysqldump --tab` → `COPY ... FROM` в Postgres.
   Надёжно для больших объёмов, но нужно вручную сопоставить колонки/типы.

Для вашего масштаба (судя по seed-данным — учебный/pet-проект) скорее всего
актуален только сценарий "данных по сути нет" — тогда шаги 1-5 полностью
покрывают миграцию, а этот шаг можно пропустить.

## Шаг 7. Переключение

1. В `application.yaml` поменять местами: то, что было в `application-postgres.yaml`,
   становится основной конфигурацией; MySQL-настройки — либо удалить, либо
   вынести в `application-mysql.yaml` (на случай отката).
2. Обновить `docker-compose.yml` (или тот, что использует CI) — заменить
   сервис MySQL на Postgres как основной.
3. Убрать/оставить `mysql-connector-j` в `pom.xml` — оставьте на один релиз
   про запас (не мешает), удалите следующим PR, когда убедитесь, что откат
   не понадобился.
4. Прогнать полный `mvn clean verify` (юнит-тесты не зависят от конкретной
   СУБД — они все на моках, так что это не проверка миграции, а просто
   регрессия по коду) + ручной прогон сценариев из шага 5 на окружении,
   максимально близком к продовому.

## Шаг 8. Дальше (не обязательно для миграции, но напрашивается)

- Добавить `@DataJpaTest` с **Testcontainers PostgreSQL** хотя бы для
  `TransactionRepository` и `BudgetRepository` — это единственный способ
  по-настоящему протестировать JPQL/SQL-специфику (как раз тот баг с
  `YEAR()/MONTH()` такой тест поймал бы автоматически, а из юнит-тестов на
  моках — никогда).
- В `AccountRepository`/`TransactionRepository` есть комментированные и
  простые `@Query` — стоит перепроверить каждый на диалект-специфичные
  функции, раз уже взялись за ревизию (кроме `getCurrentMonthExpenseByCategory`
  остальные выглядят СУБД-независимыми, но лишняя проверка не помешает).
