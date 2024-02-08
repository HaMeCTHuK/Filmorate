package ru.java.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.java.practicum.filmorate.event.EventOperation;
import ru.java.practicum.filmorate.event.EventType;
import ru.java.practicum.filmorate.model.Film;
import ru.java.practicum.filmorate.model.Mpa;
import ru.java.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class EventsDbStorageTest {

    private final JdbcTemplate jdbcTemplate;
    private LikesDbStorage likeStorage;
    private FilmDbStorage filmStorage;
    private EventDbStorage eventsStorage;
    private UserDbStorage userStorage;
    private FriendsDbStorage friendsDbStorage;

    @BeforeEach
    void init() {
        GenreDbStorage genreDbStorage = new GenreDbStorage(jdbcTemplate);
        DirectorDbStorage directorDbStorage = new DirectorDbStorage(jdbcTemplate, genreDbStorage);
        likeStorage = new LikesDbStorage(jdbcTemplate, genreDbStorage, directorDbStorage);
        filmStorage = new FilmDbStorage(jdbcTemplate, likeStorage, directorDbStorage, genreDbStorage);
        eventsStorage = new EventDbStorage(jdbcTemplate);
        userStorage = new UserDbStorage(jdbcTemplate);
        friendsDbStorage = new FriendsDbStorage(jdbcTemplate);
    }

    @Test
    public void insertAddRemoveLikeEventInDb() {

        Film newFilm1 = new Film(
                "testFilmr1",
                "description1",
                LocalDate.of(1999,2,24),
                40,
                1,
                new Mpa(),
                10L);
        newFilm1.getMpa().setId(5);

        User newUser1 = new User(
                "test1@email.ru",
                "test1",
                "Ivan Petrov",
                LocalDate.of(1990, 1, 1));

        // Записываем фильмы в БД
        filmStorage.create(newFilm1);
        // Записываем пользователей в БД
        userStorage.create(newUser1);
        // Добавляем событие в БД
        likeStorage.addLike(newFilm1.getId(), newUser1.getId());

        eventsStorage.addEvent(newUser1.getId(), newFilm1.getId(), EventType.LIKE, EventOperation.ADD);
        assertThat(eventsStorage.getUserEvents(newUser1.getId()).get(0).getOperation().toString()).isEqualTo("ADD");

        eventsStorage.addEvent(newUser1.getId(), newFilm1.getId(), EventType.LIKE, EventOperation.REMOVE);
        assertThat(eventsStorage.getUserEvents(newUser1.getId()).get(1).getOperation().toString()).isEqualTo("REMOVE");
    }

    @Test
    public void insertRemoveLikeEventInDb() {

        User newUser1 = new User(
                "test1@email.ru",
                "test1",
                "Ivan Petrov",
                LocalDate.of(1990, 1, 1));

        User newUser2 = new User(
                "test2@email.ru",
                "test2",
                "Ivan Petrov",
                LocalDate.of(1990, 1, 1));

        // Записываем пользователей в БД
        userStorage.create(newUser1);
        userStorage.create(newUser2);

        friendsDbStorage.addFriend(newUser1.getId(), newUser2.getId());

        eventsStorage.addEvent(newUser1.getId(), newUser2.getId(), EventType.FRIEND, EventOperation.ADD);
        assertThat(eventsStorage.getUserEvents(newUser1.getId()).get(0).getOperation().toString()).isEqualTo("ADD");

        eventsStorage.addEvent(newUser1.getId(), newUser2.getId(), EventType.FRIEND, EventOperation.REMOVE);
        assertThat(eventsStorage.getUserEvents(newUser1.getId()).get(1).getOperation().toString()).isEqualTo("REMOVE");
    }
}
