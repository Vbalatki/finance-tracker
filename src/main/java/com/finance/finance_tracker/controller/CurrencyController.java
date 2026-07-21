package com.finance.finance_tracker.controller;


import com.finance.finance_tracker.entity.error.ErrorResponse;
import com.finance.finance_tracker.service.CurrencyApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST-контроллер для получения курсов валют и конвертации сумм.
 *
 * <p>Единственный полноценный JSON REST API в этом приложении — остальные
 * контроллеры возвращают HTML-страницы (Thymeleaf). Именно поэтому Swagger
 * (см. {@code springdoc.paths-to-match: /api/**} в {@code application.yaml})
 * документирует только этот контроллер.
 *
 * <p>Публичное описание эндпоинтов для потребителей API — через
 * аннотации {@link Operation}/{@link ApiResponse} и Swagger UI
 * ({@code /swagger-ui.html}). Javadoc здесь дополняет их деталями
 * реализации, полезными команде разработки, а не внешним потребителям API.
 */
@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
@Tag(name = "Currency", description = "Курсы валют и конвертация сумм")
public class CurrencyController {

    private final CurrencyApiService currencyApiService;

    /**
     * Возвращает курсы валют относительно указанной базовой валюты.
     * Делегирует в {@link CurrencyApiService#getExchangeRates(String)},
     * который кэширует результат (без TTL — см. предупреждение в Javadoc
     * сервиса) и обращается к внешнему API курсов валют.
     *
     * @param base код базовой валюты, ISO 4217; по умолчанию {@code "USD"}
     * @return карта {@code код валюты -> курс относительно base}
     */
    @GetMapping("/rates")
    @Operation(
            summary = "Получить курсы валют относительно базовой",
            description = "Возвращает курсы всех доступных валют по отношению к указанной " +
                    "базовой валюте. Данные кэшируются на стороне сервера (см. @Cacheable " +
                    "на CurrencyApiServiceImpl) и обновляются не мгновенно."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Курсы валют успешно получены",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внешний сервис курсов валют недоступен или вернул ошибку",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Map<String, Double>> getRates(
            @Parameter(description = "Код базовой валюты, ISO 4217", example = "USD")
            @RequestParam(defaultValue = "USD") String base) {
        return ResponseEntity.ok(currencyApiService.getExchangeRates(base.toUpperCase()));
    }

    /**
     * Конвертирует сумму из одной валюты в другую. Делегирует в
     * {@link CurrencyApiService#convertCurrency(String, String, BigDecimal)}.
     *
     * @param from   код исходной валюты, ISO 4217
     * @param to     код целевой валюты, ISO 4217
     * @param amount сумма к конвертации
     * @return сконвертированная сумма
     * @throws IllegalArgumentException если курс для {@code to} не найден (обрабатывается
     *         {@link com.finance.finance_tracker.handler.GlobalExceptionHandler} как 400 Bad Request)
     */
    @GetMapping("/convert")
    @Operation(
            summary = "Сконвертировать сумму из одной валюты в другую",
            description = "Для одинаковых валют, нулевой или отрицательной суммы возвращает " +
                    "результат без обращения к внешнему API курсов."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Сумма сконвертирована",
                    content = @Content(schema = @Schema(implementation = BigDecimal.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Указан неизвестный код целевой валюты",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<BigDecimal> convert(
            @Parameter(description = "Код исходной валюты, ISO 4217", example = "USD")
            @RequestParam String from,
            @Parameter(description = "Код целевой валюты, ISO 4217", example = "RUB")
            @RequestParam String to,
            @Parameter(description = "Сумма к конвертации", example = "100.00")
            @RequestParam BigDecimal amount) {
        BigDecimal result = currencyApiService.convertCurrency(from, to, amount);
        return ResponseEntity.ok(result);
    }
}
