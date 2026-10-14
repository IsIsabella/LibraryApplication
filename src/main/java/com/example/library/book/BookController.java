package com.example.library.book;

import com.example.library.book.BookDTO.BookResponse;
import com.example.library.book.BookDTO.CreateBookRequest;
import com.example.library.book.BookDTO.SetActiveRequest;
import com.example.library.book.exception.BookByIdNotFoundException;
import com.example.library.book.exception.BookByVersionNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;
import java.util.List;

/**
 * REST API для управления записями о книгах
 *
 * @author e.basalaeva
 */
@RestController
@RequestMapping("/api/books")
@ThreadSafe
public class BookController {

    private final BookService bookService;

    /**
     * Создает REST-контроллер записей
     *
     * @param bookService сервис книг
     * @throws NullPointerException если входной параметр null
     */
    public BookController(BookService bookService) {
        if (bookService == null) {
            throw new NullPointerException("bookService");
        }

        this.bookService = bookService;
    }

    /**
     * Запрос GET /api/books возвращает список книг
     */
    @GetMapping
    public List<BookResponse> list() {
        return this.bookService.list();
    }

    /**
     * Запрос POST /api/books создает новую запись
     *
     * @param request тело запроса
     * @return ответ 201
     * @throws NullPointerException если входной параметр null
     */
    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        if (request == null) {
            throw new NullPointerException("request");
        }

        BookResponse created = this.bookService.create(request);
        return ResponseEntity
            .created(URI.create("/api/books/" + created.id()))
            .body(created);
    }

    /**
     * Запрос PATCH /api/books/{id}/active изменяет статус
     *
     * @param id      идентификатор
     * @param request статус и версия
     * @return обновленная запись о книге
     * @throws BookByIdNotFoundException      ответ 404
     * @throws BookByVersionNotFoundException ответ 409
     * @throws NullPointerException           если входной параметр null
     */
    @PatchMapping("/{id}/active")
    public BookResponse setActive(
        @PathVariable Long id,
        @Valid @RequestBody SetActiveRequest request) {
        if (request == null) {
            throw new NullPointerException("request");
        }

        return this.bookService.setActive(id, request);
    }

    /**
     * Запрос DELETE /api/books/{id}?version=N удаляет запись
     * <p>
     * Без version обязателен: без него Spring вернет 400
     * </p>
     *
     * @param id      идентификатор
     * @param version ожидаемая версия
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam long version) {
        this.bookService.delete(id, version);
    }
}
