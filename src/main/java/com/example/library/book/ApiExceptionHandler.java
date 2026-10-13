package com.example.library.book;

import com.example.library.book.exception.BookByIdNotFoundException;
import com.example.library.book.exception.BookByVersionNotFoundException;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.annotation.concurrent.ThreadSafe;
import java.util.stream.Collectors;

/**
 * Обработчик исключений преобразует внутренние ошибки приложения в стандартизированный
 * JSON-ответ в формате RFC 7807 (Problem Details)
 *
 * @author e.basalaeva
 */
@RestControllerAdvice
@ThreadSafe
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Ошибка, когда книга не найдена по ID
     *
     * @param exception BookNotFoundException
     * @return статус 404
     */
    @ExceptionHandler(BookByIdNotFoundException.class)
    ProblemDetail notFound(BookByIdNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * Ошибка, когда два пользователя или процесса одновременно пытаются обновить одну и ту же
     * запись в БД, а также книга не найдена по версии
     *
     * @param exception исключение конфликта
     * @return статус 409
     */
    @ExceptionHandler({BookByVersionNotFoundException.class,
        OptimisticLockingFailureException.class})
    ProblemDetail conflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            new BookByVersionNotFoundException().getMessage());
    }

    /**
     * Ошибка доступа к БД
     *
     * @param exception ошибка БД
     * @return статус 503
     */
    @ExceptionHandler(DataAccessException.class)
    ProblemDetail db(DataAccessException exception) {
        log.error("Ошибка доступа к БД", exception);
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "База данных временно недоступна, попробуйте позже");
    }

    /**
     * Ошибки валидации данных
     *
     * @param exception ошибка валидации
     * @param headers   заголовки
     * @param status    статус
     * @param request   запрос
     * @return статус 400
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .distinct()
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail));
    }

    /**
     * Любая другая ошибка
     *
     * @param exception любая непредвиденная ошибка
     * @return статус 500
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception) {
        log.error("Непредвиденная ошибка", exception);
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Внутренняя ошибка сервера");
    }
}
