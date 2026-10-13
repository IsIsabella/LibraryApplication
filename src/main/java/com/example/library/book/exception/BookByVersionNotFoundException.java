package com.example.library.book.exception;

import com.example.library.book.ApiExceptionHandler;

import javax.annotation.concurrent.Immutable;

/**
 * Класс для обработки ошибки - книга по версии не найдена, {@link ApiExceptionHandler}
 * превращает в HTTP 409
 *
 * @author e.basalaeva
 */
@Immutable
public class BookByVersionNotFoundException extends RuntimeException {

    public BookByVersionNotFoundException() {
        super("Книга была изменена в другом окне. Повторите действие");
    }
}
