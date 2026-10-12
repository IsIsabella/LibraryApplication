package com.example.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа в приложение
 *
 * @author e.basalaeva
 */
@SpringBootApplication
public class LibraryApplication {

    /**
     * Запускает SpringBoot
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
