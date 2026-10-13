package com.example.library.book.exception;

import com.example.library.book.ApiExceptionHandler;

import javax.annotation.concurrent.Immutable;

/**
 * Класс для обработки ошибки - книга по ID не найдена, {@link ApiExceptionHandler} превращает в
 * HTTP 404
 *
 * @author e.basalaeva
 */
@Immutable
public class BookByIdNotFoundException extends RuntimeException {

    /**
     * @param id идентификатор отсутствующей книги
     */
    public BookByIdNotFoundException(Long id) {
        super("Книга c ID = " + id + " не найдена");
    }
}
