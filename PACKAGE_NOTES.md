# Пакет валидации (Зона 1) — что внутри и как накатывать

Собран на основе `finance-tracker-main.zip`, сверенного построчно с этим
пакетом. Роли (Зона 0) сюда сознательно не входят — там уже похоже кто-то
работает (см. Audit/Patch Log и историю чата).

## Как накатывать

1. **Сначала прочитай текущее состояние своих файлов**, не накатывай
   вслепую — с момента сборки этого пакета что-то ещё могло измениться.
2. Файлы в `validation/`, `testsupport/`, `dto/ChangePasswordDto.java` —
   новые, просто копируются.
3. Остальное — модификации существующих файлов. Смотри раздел
   «Требует ручной сверки» ниже **обязательно**, там не просто копипаста.
4. `mvn clean verify` в моей песочнице не запускался — нет доступа к
   Maven Central (та же ситуация, что в записи Audit Log от 2026-08-13).
   Все файлы вычитаны вручную построчно, но не скомпилированы.
5. После накатки — запись в Audit / Patch Log тем же форматом, что уже
   есть: что проверил, что поправил, что не гарантировано.

## Требует ручной сверки перед мерджем (не просто копипаста)

- **`dto/AccountDto.java`** — я убрал `@NotNull` с `userId` по описанию из
  Audit Log ("AccountDto.userId — убран @NotNull"), а не по свежему чтению
  файла байт-в-байт. Сверь весь файл целиком перед мерджем.
- **`test/controller/CategoryControllerTest.java`**, блок
  `editCategoryForm_*` (3 теста) — реконструирован по коду контроллера
  (он сверен дословно), а не списан с живого теста. Имена методов и точная
  форма проверок могут не совпасть с тем, что уже есть в репозитории.
  **Возьми одну версию, не обе** — иначе получится дублирование тестов
  на одно и то же поведение.
- **`controller/CategoryController.java`**, метод `editCategoryForm` —
  скопирован как есть из актуального репо (чужая правка, ownership-check
  на GET), я его не менял. Метод `updateCategory` в этом же файле — мой.

## Что внутри

**Новое:** `validation/` (UniqueEmail, UniqueAccountName, UniqueCategoryName
+ валидаторы, PasswordsMatch + валидатор, PasswordValidator),
`dto/ChangePasswordDto.java`, `testsupport/` (PermissiveConstraintValidatorFactory,
TestValidators), 5 тестов на новые валидаторы.

**Изменено:** `DataConstants` (MIN_PASSWORD_LENGTH String→int), `UserDto`/
`AccountDto`/`CategoryDto` (навешаны Unique*-аннотации), оба репозитория
(добавлены `*AndIdNot`-методы), `UserService`+`Impl` (changePassword на
ChangePasswordDto, убран дубль-check email), `UserController` +
`users/change-password.html` (форма на th:object), `CategoryServiceImpl`
(убраны дубли blank/duplicate), `CategoryController.updateCategory`
(на @Valid), `categories/edit.html` (форма на th:object), плюс точечные
правки в существующих тестах (добавлен `.setValidator(TestValidators.permissive())`,
убраны тесты дублирующегося поведения, тесты changePassword переписаны
под новую сигнатуру).

**Не включено:** всё, что касается `RoleDto`/`RoleServiceImpl`/
`AdminControllerTest`/`RoleServiceImplTest` — см. предупреждение про
Зону 0.
