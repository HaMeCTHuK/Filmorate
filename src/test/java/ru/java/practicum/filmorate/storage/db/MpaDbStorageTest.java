package ru.java.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.java.practicum.filmorate.exception.DataNotFoundException;
import ru.java.practicum.filmorate.model.Mpa;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class MpaDbStorageTest {

    private final JdbcTemplate jdbcTemplate;
    private MpaDbStorage mpaStorage;

    @BeforeEach
    void init() {
        mpaStorage = new MpaDbStorage(jdbcTemplate);
    }

    @Test
    void getAll() {

        List<Mpa> mpas = mpaStorage.getAll();
        assertNotNull(mpas);
        assertFalse(mpas.isEmpty());
    }

    @Test
    void get() {
        Mpa existingMpa = mpaStorage.getAll().get(0);

        Mpa retrievedMpa = mpaStorage.get(existingMpa.getId());
        assertNotNull(retrievedMpa);
        assertEquals(existingMpa.getId(), retrievedMpa.getId());
    }

    @Test
    void getNonExistentMpa() {
        MpaDbStorage mpaStorage = new MpaDbStorage(jdbcTemplate);
        assertThrows(DataNotFoundException.class, () -> mpaStorage.get(-1L));
    }

    @Test
    void create() {
        Mpa newMpa = Mpa.builder().id(7).name("New Mpa").build();
        Mpa createdMpa = mpaStorage.create(newMpa);

        assertNotNull(createdMpa);
        assertNotNull(createdMpa.getId());
        assertEquals(newMpa.getName(), createdMpa.getName());
    }

    @Test
    void update() {
        Mpa existingMpa = mpaStorage.getAll().get(0);
        String updatedName = "Updated Mpa";

        existingMpa.setName(updatedName);
        Mpa updatedMpa = mpaStorage.update(existingMpa);

        assertEquals(existingMpa.getId(), updatedMpa.getId());
        assertEquals(updatedName, updatedMpa.getName());
    }

    @Test
    void delete() {
        Mpa newMpa = Mpa.builder().name("ToDelete Mpa").build();
        Mpa createdMpa = mpaStorage.create(newMpa);

        assertNotNull(createdMpa);
        mpaStorage.delete(createdMpa.getId());
        assertThrows(DataNotFoundException.class, () -> mpaStorage.get(createdMpa.getId()));
    }
}
