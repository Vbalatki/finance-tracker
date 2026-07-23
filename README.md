# Финансовый трекер

Веб-приложение для управления личными финансами: счета, транзакции, категории, бюджеты. С поддержкой аудита действий пользователей, метриками и REST API для конвертации валют.

## Возможности

- **Управление счетами** — CRUD, пополнение, снятие, баланс по валютам.
- **Транзакции** — доходы/расходы с категориями, фильтрация, группировка по счетам.
- **Бюджеты** — установка месячных лимитов на категории, отслеживание трат.
- **Аудит** — асинхронное логирование операций создания/изменения/удаления с деталями.
- **Админ-панель** — управление пользователями, ролями, просмотр журнала аудита.
- **Курсы валют** — REST API для получения курсов и конвертации сумм (см. [Swagger](#swagger--openapi)).
- **Безопасность** — Spring Security, пароли хешируются BCrypt.

## Стек технологий

- Java 17, Spring Boot 3.5
- Spring Data JPA + Hibernate
- Spring Security
- **PostgreSQL 16** + **Liquibase** (управление схемой БД)
- Thymeleaf (веб-интерфейс)
- Lombok, MapStruct
- springdoc-openapi (Swagger UI)
- Micrometer + Prometheus + Grafana (метрики и мониторинг)
- JUnit 5, Mockito, JaCoCo (тесты и покрытие)
- Docker / Docker Compose
- Maven

## Быстрый старт (Docker)

Самый простой способ поднять всё окружение целиком:

```bash
cp .env.example .env
# при необходимости отредактируйте .env (пароль БД, ключ currency API)

docker compose up -d --build
```

Приложение будет доступно на **http://localhost:8080**.

Тестовые учётные записи (созданы seed-данными Liquibase):

| Email | Пароль | Роль |
|---|---|---|
| `ivan@example.com` | `password` | Администратор |
| `maria@example.com` | `password` | Пользователь |

Подробный пошаговый протокол запуска, проверки и частые проблемы —
в [`docs/docker-launch-protocol.md`](docs/docker-launch-protocol.md).

### Мониторинг (Prometheus + Grafana)

Отдельный стек, запускается вместе с основным через два `-f`, чтобы попасть
в одну Docker-сеть:

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d --build
```

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin / admin при первом входе)

Подробности — в [`monitoring/README.md`](monitoring/README.md).

## Локальный запуск без Docker

Нужен установленный PostgreSQL 16 и Java 17.

```bash
# создать базу и пользователя вручную, либо поднять только postgres из compose:
docker compose up -d postgres

mvn spring-boot:run
```

Конфигурация подключения к БД в `application.yaml` берётся из переменных
окружения с дефолтами (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`,
`DB_PASSWORD`) — без Docker подставятся дефолты, указывающие на
`localhost:5432`.

Схема БД создаётся автоматически при старте приложения через Liquibase
(`src/main/resources/db/changelog/`) — накатывать SQL руками не нужно.

## Swagger / OpenAPI

Документирован только раздел `/api/**` (реальный JSON REST API) — остальные
контроллеры возвращают HTML-страницы (Thymeleaf) и в Swagger не включены.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Тесты и покрытие кода

```bash
mvn clean verify
```

Прогоняет полный набор unit-тестов (сервисы через Mockito, контроллеры через
standalone MockMvc) и генерирует отчёт JaCoCo:

```
target/site/jacoco/index.html
```

## Структура проекта

```
src/main/java/com/finance/finance_tracker/
├── controller/       # Thymeleaf-контроллеры + REST-контроллер CurrencyController
├── service/           # интерфейсы сервисов (с Javadoc)
│   └── Impl/           # реализации
├── repository/        # Spring Data JPA репозитории
├── entity/             # JPA-сущности
├── DTO/                # объекты передачи данных
├── mapper/             # MapStruct-мапперы DTO <-> Entity
├── config/             # конфигурация (Security, OpenAPI, Web)
├── aspect/             # AOP-аспект аудита
├── handler/            # глобальный обработчик исключений
├── exception/          # кастомные исключения
└── Util/               # утилитные классы (форматирование валют, security-хелперы)

src/main/resources/
├── db/changelog/       # Liquibase changelog (схема БД + seed-данные)
├── templates/           # Thymeleaf-шаблоны
├── static/               # CSS/JS
└── application.yaml     # конфигурация приложения

src/test/java/          # unit-тесты (структура зеркалирует src/main)

docs/                    # дополнительная документация (протокол запуска, план миграции)
monitoring/              # конфигурация Prometheus/Grafana
```

## Известные ограничения

Список того, что осознанно не доведено до идеала на данный момент —
чтобы не удивляться при код-ревью:

- `AccountService.getTotalBalanceInCurrency` не выполняет реальную
  конвертацию — возвращает то же, что и `getTotalBalance` без учёта
  параметра `currency`.
- `UserService.getUserTotalExpenseInRub` обращается к внешнему API
  конвертации валют даже для уже рублёвых сумм (в отличие от
  `getUserTotalIncomeInRub`, где есть короткий путь) — расхождение в
  реализации, не намеренная оптимизация.
- `CategoryService.getAllCategories()` возвращает категории всех
  пользователей без фильтрации — используется в местах, доступных рядовому
  пользователю, что технически позволяет увидеть чужие названия категорий.
- Курсы валют кэшируются (`@Cacheable`) без TTL и без graceful fallback при
  недоступности внешнего API — при падении `exchangerate-api.com`
  соответствующие страницы (дашборд, счета) вернут 500 вместо деградации.
- Кэш метрик/данных — `ConcurrentMapCacheManager` по умолчанию (никакой
  специальный `CacheManager` не настроен), живёт в памяти процесса без
  вытеснения.

## CI/CD

Не настроен — сборка и тесты запускаются только вручную (`mvn clean verify`).