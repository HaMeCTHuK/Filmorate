package ru.java.practicum.filmorate.storage;

import ru.java.practicum.filmorate.model.Mpa;

import java.util.List;

public interface MpaStorage extends AbstractStorage<Mpa> {

    List<Mpa> getAll();

    Mpa get(Long id);

    Mpa createMpa(int id, String name); // добавлено для работоспособности тестов
}
