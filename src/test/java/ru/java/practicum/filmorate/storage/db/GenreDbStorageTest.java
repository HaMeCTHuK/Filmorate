package ru.java.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.java.practicum.filmorate.exception.DataNotFoundException;
import ru.java.practicum.filmorate.model.Genre;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GenreDbStorageTest {

    private final JdbcTemplate jdbcTemplate;
    private GenreDbStorage genreStorage;

    @BeforeEach
    void init() {
        genreStorage = new GenreDbStorage(jdbcTemplate);
    }

    @Test
    void getAll() {

        List<Genre> genres = genreStorage.getAll();
        assertNotNull(genres);
        assertFalse(genres.isEmpty());
    }

    @Test
    void get() {

        Genre genre = genreStorage.getAll().get(0);
        Genre recivedGenreById = genreStorage.get(genre.getId());
        assertNotNull(recivedGenreById);
        assertEquals(genre.getId(), recivedGenreById.getId());
    }

    @Test
    void getNonExistentGenre() {

        assertThrows(DataNotFoundException.class, () -> genreStorage.get(-1L));
    }

    @Test
    void createGenre() {

        Genre newGenre = Genre.builder().id(10L).name("New Genre").build();
        Genre createdGenre = genreStorage.create(newGenre);

        assertNotNull(createdGenre);
        assertEquals(newGenre.getName(), createdGenre.getName());
    }

    @Test
    void update() {

        Genre existingGenre = genreStorage.getAll().get(0);
        String updatedName = "Updated Genre";

        existingGenre.setName(updatedName);
        Genre updatedGenre = genreStorage.update(existingGenre);

        assertEquals(existingGenre.getId(), updatedGenre.getId());
        assertEquals(updatedName, updatedGenre.getName());
    }

    @Test
    void delete() {

        Genre newGenre = Genre.builder().id(7L).name("ToDelete Genre").build();
        Genre createdGenre = genreStorage.create(newGenre);

        assertNotNull(createdGenre);

        genreStorage.delete(createdGenre.getId());

        assertThrows(DataNotFoundException.class, () -> genreStorage.get(createdGenre.getId()));
    }
}
