package com.example.library.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import javax.annotation.concurrent.Immutable;
import java.time.Instant;

/**
 * DTO для cущности "Книга"
 * Не отдается наружу, т.к. API не зависит от схемы БД, что позволяет избежать подмены
 * id/version/createdAt при создании
 *
 * @author e.basalaeva
 */
@Immutable
public final class BookDTO {

    private BookDTO() {
    }

    /**
     * Тело для запроса POST /api/books
     *
     * @param active      статус активности
     * @param icon        ключ иконки
     * @param name        название книги
     * @param description описание книги
     */
    @Immutable
    public record CreateBookRequest(
        boolean active,
        @NotBlank(message = "Выберите иконку")
        @Pattern(regexp = "icon[1-3]", message = "Неизвестная иконка")
        String icon,
        @NotBlank(message = "Название книги не может быть пустым")
        @Size(max = 300, message = "Название книги не длиннее 300 символов")
        String name,
        @NotBlank(message = "Описание книги не может быть пустым")
        @Size(max = 1000, message = "Описание не длиннее 1000 символов")
        String description) {
    }

    /**
     * Тело для запроса PATCH /api/books/{id}/active
     *
     * @param active  статус активности
     * @param version версия, которую клиент видел на экране
     */
    @Immutable
    public record SetActiveRequest(
        boolean active,
        @NotNull(message = "Не передана версия книги")
        Long version) {
    }

    /**
     * Ответ сервера
     *
     * @param id          Id записи о книге
     * @param active      статус активности
     * @param icon        ключ иконки
     * @param name        название книги
     * @param description описание книги
     * @param version     текущая версия
     * @param createdAt   момент создания
     */
    @Immutable
    public record BookResponse(Long id, boolean active, String icon, String name,
                               String description, long version, Instant createdAt) {

        /**
         * Преобразования сущности
         *
         * @param book сущность из БД
         * @return DTO для клиента
         */
        static BookResponse from(Book book) {
            return new BookResponse(
                book.getId(), book.isActive(), book.getIcon(),
                book.getName(), book.getDescription(),
                book.getVersion(), book.getCreatedAt());
        }
    }
}
