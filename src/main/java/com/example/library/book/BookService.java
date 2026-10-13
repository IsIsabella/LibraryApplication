package com.example.library.book;

import com.example.library.book.BookDTO.BookResponse;
import com.example.library.book.BookDTO.CreateBookRequest;
import com.example.library.book.BookDTO.SetActiveRequest;
import com.example.library.book.exception.BookByIdNotFoundException;
import com.example.library.book.exception.BookByVersionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

/**
 * Основная логика приложения
 * Потокобезопасен, т.к. изменения состояния в полях нет
 *
 * @author e.basalaeva
 */
@Service
@ThreadSafe
public class BookService {

    private final IBookRepository bookRepository;

    /**
     * Конструктор сервиса для репозитория книг
     *
     * @param bookRepository репозиторий книг
     * @throws NullPointerException если входной параметр null
     */
    public BookService(IBookRepository bookRepository) {
        if (bookRepository == null) {
            throw new NullPointerException("bookRepository");
        }

        this.bookRepository = bookRepository;
    }

    /**
     * Список с записями о книгах
     *
     * @return список DTO; пустой, если книг нет
     */
    @Transactional(readOnly = true)
    public List<BookResponse> list() {
        return this.bookRepository.findAllByOrderByCreatedAtAscIdAsc().stream()
            .map(BookResponse::from)
            .toList();
    }

    /**
     * Создание новой записи, с удалением лишних пробелов
     *
     * @param request данные из HTTP-запроса
     * @return созданная книга с id и version от БД
     * @throws NullPointerException если входной параметр null
     */
    @Transactional
    public BookResponse create(CreateBookRequest request) {
        if (request == null) {
            throw new NullPointerException("request");
        }
        Book saved = this.bookRepository.save(new Book(
            request.active(), request.icon(),
            request.name().strip(), request.description().strip()));
        return BookResponse.from(saved);
    }

    /**
     * Смена флага активности
     *
     * @param id      Id записи о книге
     * @param request новый статус и ожидаемая версия
     * @return обновлённая книга
     * @throws BookByIdNotFoundException      если книги нет
     * @throws BookByVersionNotFoundException если версия устарела
     * @throws NullPointerException           если входной параметр null
     */
    @Transactional
    public BookResponse setActive(Long id, SetActiveRequest request) {
        if (request == null) {
            throw new NullPointerException("request");
        }
        Book book = this.bookRepository.findById(id)
            .orElseThrow(() -> new BookByIdNotFoundException(id));
        if (book.getVersion() != request.version()) {
            throw new BookByVersionNotFoundException();
        }
        book.setActive(request.active());
        return BookResponse.from(this.bookRepository.saveAndFlush(book));
    }

    /**
     * Удаление по Id и версии
     *
     * @param id      Id записи о книге
     * @param version ожидаемая версия
     * @throws BookByIdNotFoundException      если книги нет
     * @throws BookByVersionNotFoundException если версия не совпала
     */
    @Transactional
    public void delete(Long id, long version) {
        int deleted = this.bookRepository.deleteByIdAndVersion(id, version);
        if (deleted == 1) {
            return;
        }
        if (this.bookRepository.existsById(id)) {
            throw new BookByVersionNotFoundException();
        }
        throw new BookByIdNotFoundException(id);
    }
}
