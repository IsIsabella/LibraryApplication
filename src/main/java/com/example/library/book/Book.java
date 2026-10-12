package com.example.library.book;

import jakarta.persistence.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;

/**
 * JPA-сущность (строка таблицы books)
 * <p>
 * Клиентские поля: id, active, icon, name, description
 * Технические поля: version (защита от lost update), createdAt (сортировка)
 * <p>
 * <p>
 * Не потокобезопасен, т.к. каждый поток/транзакция работает со своим экземпляром из репозитория
 *
 * @author e.basalaeva
 */
@Entity
@Table(name = "books")
@NotThreadSafe
public class Book {

    /**
     * Id записи о книге
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Статус активности
     */
    @Column(nullable = false)
    private boolean active;

    /**
     * Ключ иконки
     */
    @Column(nullable = false)
    private String icon;

    /**
     * Название книги
     */
    @Column(nullable = false)
    private String name;

    /**
     * Описание книги
     */
    @Column(nullable = false)
    private String description;

    /**
     * Счетчик для @Version
     */
    @Version
    private long version;

    /**
     * Момент создания
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Конструктор JPA для создания через рефлексию
     */
    protected Book() {
    }

    /**
     * Конструктор для передачи параметров
     *
     * @param active      статус активности
     * @param icon        ключ иконки
     * @param name        название книги
     * @param description описание книги
     */
    public Book(boolean active, String icon, String name, String description) {
        if (icon.isEmpty() || name.isEmpty() || description.isEmpty()) {
            throw new IllegalArgumentException("Входной параметр не может быть пустым");
        }
        this.active = active;
        this.icon = icon;
        this.name = name;
        this.description = description;
    }

    /**
     * Hibernate вызывает перед первым INSERT
     */
    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isActive() {
        return this.active;
    }

    /**
     * Сеттер для установки статуса активности
     *
     * @param active новое значение флага
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return this.id;
    }

    public String getIcon() {
        return this.icon;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public long getVersion() {
        return this.version;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
