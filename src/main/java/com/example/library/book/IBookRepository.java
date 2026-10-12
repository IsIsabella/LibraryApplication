package com.example.library.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.annotation.concurrent.Immutable;
import java.util.List;

/**
 * Репозиторий для {@link Book}
 *
 * @author e.basalaeva
 */
@Immutable
public interface IBookRepository extends JpaRepository<Book, Long> {

    /**
     * Сртировка по дате создания
     *
     * @return список всех книг
     */
    List<Book> findAllByOrderByCreatedAtAscIdAsc();

    /**
     * Удаление, только если id и версия совпадают
     *
     * @param id      идентификатор
     * @param version ожидаемая версия
     * @return число удалённых строк
     */
    @Modifying
    @Query("delete from Book i where i.id = :id and i.version = :version")
    int deleteByIdAndVersion(@Param("id") Long id, @Param("version") long version);
}
