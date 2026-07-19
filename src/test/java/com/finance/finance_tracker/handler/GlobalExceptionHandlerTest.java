package com.finance.finance_tracker.handler;

import com.finance.finance_tracker.entity.error.ErrorResponse;
import com.finance.finance_tracker.exception.AccessDeniedException;
import com.finance.finance_tracker.exception.DuplicateEntityException;
import com.finance.finance_tracker.exception.EntityNotFoundException;
import com.finance.finance_tracker.exception.InsufficientFundsException;
import com.finance.finance_tracker.exception.InvalidAmountException;
import com.finance.finance_tracker.exception.InvalidDataException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link GlobalExceptionHandler}: проверяем, что каждому типу
 * исключений соответствует правильный HTTP-статус и сообщение в теле ответа.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("AccessDeniedException -> 403 FORBIDDEN")
    void handleAccessDenied_returnsForbidden() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDeniedException(new AccessDeniedException("Нет доступа"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Нет доступа");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("EntityNotFoundException -> 404 NOT_FOUND")
    void handleEntityNotFound_returnsNotFound() {
        ResponseEntity<ErrorResponse> response =
                handler.handleEntityNotFoundException(new EntityNotFoundException("Не найдено"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Не найдено");
    }

    @Test
    @DisplayName("InvalidDataException -> 400 BAD_REQUEST")
    void handleInvalidData_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidDataException(new InvalidDataException("Некорректные данные"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("InvalidAmountException -> 400 BAD_REQUEST")
    void handleInvalidAmount_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidAmountException(new InvalidAmountException("Некорректная сумма"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("DuplicateEntityException -> 409 CONFLICT")
    void handleDuplicateEntity_returnsConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDuplicateEntityException(new DuplicateEntityException("Уже существует"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("InsufficientFundsException -> 400 BAD_REQUEST")
    void handleInsufficientFunds_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInsufficientFundsException(new InsufficientFundsException("Недостаточно средств"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 BAD_REQUEST с деталями по полям")
    void handleValidationErrors_returnsBadRequestWithFieldDetails() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "email", "не должно быть пустым");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("email");
    }

    @Test
    @DisplayName("Любое другое исключение -> 500 INTERNAL_SERVER_ERROR без утечки деталей")
    void handleGenericException_returnsInternalServerErrorWithGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(new RuntimeException("что-то пошло не так"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Произошла внутренняя ошибка сервера");
    }
}
