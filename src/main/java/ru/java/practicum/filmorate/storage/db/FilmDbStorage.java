package ru.java.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.java.practicum.filmorate.exception.DataNotFoundException;
import ru.java.practicum.filmorate.model.Director;
import ru.java.practicum.filmorate.model.Film;
import ru.java.practicum.filmorate.model.Genre;
import ru.java.practicum.filmorate.model.Mpa;
import ru.java.practicum.filmorate.storage.FilmStorage;
import ru.java.practicum.filmorate.storage.LikesStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final DirectorDbStorage directorDbStorage;
    private final LikesStorage likesStorage;

    // Метод для добавления нового фильма
    @Override
    public Film create(Film film) {
        log.info("Отправляем данные для создания FILM в таблице");
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getDataSource())
                .withTableName("FILMS")
                .usingGeneratedKeyColumns("id");

        Number filmId = simpleJdbcInsert.executeAndReturnKey(getParams(film)); // Получаем id из таблицы при создании
        film.setId(filmId.longValue());

        // Добавляем информацию о жанрах в таблицу FILM_GENRE
        addGenresForFilm(filmId.longValue(), film.getGenres());
        // Добавляем информацию о жанрах в таблицу FILM_DIRECTORS
        addDirectorForFilm(filmId.longValue(), film.getDirectors());

        // Заполняем жанры, режиссеров
        film.setGenres(getGenresForFilm(filmId.longValue()));
        film.setDirectors(getDirectorsForFilm(filmId.longValue()));


        Mpa mpa = getMpaRating(film.getMpa());  // Получаем MPA из базы данных
        film.getMpa().setName(mpa.getName());  // Устанавливаем имя рейтинга MPA в объекте Film
        log.info("Добавлен объект: " + film);
        return film;
    }

    // Метод для обновления существующего фильма
    @Override
    public Film update(Film film) {
        film.setMpa(getMpaRatingById(film.getMpa().getId()));
        String sql = "UPDATE FILMS " +
                "SET NAME=?, " +
                "DESCRIPTION=?, " +
                "RELEASE_DATE=?, " +
                "DURATION=?, " +
                "RATING=?, " +
                "MPA_RATING_ID=? " +
                "WHERE ID=?";
        log.info("Пытаемся обновить информацию о film");
        int rowsUpdated = jdbcTemplate.update(sql, getParametersWithId(film));
        if (rowsUpdated == 0) {
            log.info("Данные о фильме не найдены");
            throw new DataNotFoundException("Данные о фильме не найдены");
        }

        // Удаляем устаревшие жанры
        String deleteGenresSql = "DELETE FROM FILM_GENRE WHERE film_id = ?";
        jdbcTemplate.update(deleteGenresSql, film.getId());
        // Добавляем новые жанры (если они не дублируются)
        Set<Long> existingGenreIds = new HashSet<>();
        for (Genre genre : film.getGenres()) {
            if (!existingGenreIds.contains(genre.getId())) {
                existingGenreIds.add(genre.getId());
                jdbcTemplate.update("INSERT INTO FILM_GENRE (film_id, genre_id) VALUES (?, ?)",
                        film.getId(), genre.getId());
            }
        }

        // Удаляем устаревших режисеров
        String deleteDirectorSql = "DELETE FROM FILM_DIRECTOR WHERE film_id = ?";
        jdbcTemplate.update(deleteDirectorSql, film.getId());
        // Добавляем новых режисеров (если они не дублируются)
        Set<Long> existingDirectorIds = new HashSet<>();
        for (Director director : film.getDirectors()) {
            if (!existingDirectorIds.contains(director.getId())) {
                existingDirectorIds.add(director.getId());
                jdbcTemplate.update("INSERT INTO FILM_DIRECTOR (film_id, director_id) VALUES (?, ?)",
                        film.getId(), director.getId());
            }
        }

        // Получаем обновленные жанры
        film.setGenres(getGenresForFilm(film.getId()));
        // Получаем обновленных режиссеров
        film.setDirectors(getDirectorsForFilm(film.getId()));

        log.info("Обновлен объект: " + film);
        return film;
    }

    // Метод для получения списка всех фильмов
    @Override
    public List<Film> getAll() {
        String sql = "SELECT f.*, m.rating_name AS mpa_rating_name " +
                "FROM FILMS f " +
                "LEFT JOIN MPARating m ON f.mpa_rating_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, FilmDbStorage::createFilm);

        // Добавляем жанры и режиссеров к каждому фильму
        for (Film film : films) {
            List<Genre> genres = getGenresForFilm(film.getId());
            List<Director> directors = getDirectorsForFilm(film.getId());

            film.setGenres(genres);
            film.setDirectors(directors);
        }

        return films;
    }

    // Метод для получения конкретного фильма по его идентификатору
    @Override
    public Film get(Long id) {
        String sql = "SELECT * FROM FILMS " +
                "LEFT JOIN MPARating ON FILMS.mpa_rating_id = MPARating.id " +
                "LEFT JOIN FILM_GENRE ON FILMS.id = FILM_GENRE.film_id " +
                "LEFT JOIN GENRES ON FILM_GENRE.genre_id = GENRES.id " +
                "LEFT JOIN FILM_DIRECTOR ON FILMS.id = FILM_DIRECTOR.film_id " +
                "LEFT JOIN DIRECTORS ON FILM_DIRECTOR.director_id = DIRECTORS.id " +
                "WHERE FILMS.id = ?";

        SqlRowSet resultSet = jdbcTemplate.queryForRowSet(sql, id);

        List<Genre> genres = new ArrayList<>();
        if (!getGenresForFilm(id).isEmpty()) {
            genres = getGenresForFilm(id);
        }

        List<Director> directors = new ArrayList<>();
        if (!getDirectorsForFilm(id).isEmpty()) {
            directors = getDirectorsForFilm(id);
        }

        if (resultSet.next()) {
            Film film = Film.builder()
                    .id(resultSet.getLong("id"))
                    .name(resultSet.getString("name"))
                    .description(resultSet.getString("description"))
                    .releaseDate(Objects.requireNonNull(resultSet.getDate("release_date")).toLocalDate())
                    .duration(resultSet.getInt("duration"))
                    .rating(resultSet.getInt("rating"))
                    .mpa(Mpa.builder()
                            .id(resultSet.getLong("mpa_rating_id"))
                            .name(resultSet.getString("rating_name"))
                            .build())
                    .genres(Collections.singletonList(
                            Genre.builder()
                                    .id(resultSet.getLong("id"))
                                    .name(resultSet.getString("genre_name"))
                                    .build()))
                    .directors(Collections.singletonList(
                            Director.builder()
                                    .id(resultSet.getLong("id"))
                                    .name(resultSet.getString("director_name"))
                                    .build()))
                    .build();

            film.setGenres(genres);
            film.setDirectors(directors);
            log.info("Найден фильм: {} {}", film.getId(), film.getName());
            return film;
        } else {
            log.info("Фильм с идентификатором {} не найден.", id);
            throw new DataNotFoundException("Фильм не найден.");
        }
    }

    // Метод для удаления фильма по его идентификатору
    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM FILMS WHERE id = ?";
        jdbcTemplate.update(sql, id);
        log.info("Удален объект с id=" + id);
    }

    // Метод получения списка рекомендованных фильмов пользователя
    @Override
    public List<Film> getRecommendationsFilms(Long userId) {
        // создаем список рекоммендованных фильмов
        List<Film> recommendFilms = new ArrayList<>();
        // запрос для получения пользователей по количеству пересечений
        String sqlCross = "SELECT l2.user_id \"crossed_user_id\", COUNT(*) \"num_cross\" " +
                "FROM LIKES l1 " +
                "JOIN LIKES l2 on l1.film_id = l2.film_id and l1.user_id <> l2.user_id " +
                "WHERE l1.user_id = ? " +
                "GROUP BY \"crossed_user_id\" " +
                "ORDER BY \"num_cross\" ";

        // запрос для получнеия фильмов с пересекающимся пользователем
        // но фильмов без общих лайков
        String sqlListRecFilms = "SELECT l2.film_id " +
                "FROM (SELECT film_id FROM LIKES WHERE user_id = ?) l1 " +
                "RIGHT JOIN (SELECT film_id FROM LIKES WHERE user_id = ?) l2 " +
                "ON l1.film_id = l2.film_id " +
                "WHERE l1.film_id IS NULL ";

        SqlRowSet resultSetCross = jdbcTemplate.queryForRowSet(sqlCross, userId);

        // проходим по списку пользователей из запроса пересечений
        while (resultSetCross.next()) {
            Long crossedUserId = resultSetCross.getLong("crossed_user_id");

            SqlRowSet resultSetRecFilms = jdbcTemplate.queryForRowSet(sqlListRecFilms, userId, crossedUserId);

            List<Long> recommendFilmsId = new ArrayList<>();

            // заполняем список НЕ пересекающихся id фильмов
            while (resultSetRecFilms.next()) {
                recommendFilmsId.add(resultSetRecFilms.getLong("film_id"));
            }

            // если НЕ пересекающихся фильмов нет
            // переходим к следующему пользователю
            if (recommendFilmsId.isEmpty()) {
                continue;
            }

            // формируем список фильмов по id
            recommendFilms = recommendFilmsId.stream()
                    .map(id -> get(id))
                    .collect(Collectors.toList());

            return recommendFilms;
        }

        // если вовсе нет рекомендаций
        // возвращаем 200 и пустой спикок
        log.info("Рекомендованных фильмов для пользователя {} не найдено", userId);
        return recommendFilms;
    }

    // Вспомогательный метод для извлечения параметров для SQL-запросов с id
    protected Object[] getParametersWithId(Film film) {
        return new Object[]{
                film.getName(),
                film.getDescription(),
                film.getReleaseDate() != null ? film.getReleaseDate().toString() : null,
                film.getDuration(),
                film.getRating(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId()};
    }

    // Вспомогательный метод для извлечения параметров для SQL-запросов
    private static Map<String, Object> getParams(Film film) {

        Map<String, Object> params = Map.of(
                "name", film.getName(),
                "description", film.getDescription(),
                "release_date", film.getReleaseDate().toString(),
                "duration", film.getDuration(),
                "rating", film.getRating(),
                "mpa_rating_id", film.getMpa().getId(),
                "genres", film.getGenres(),
                "directors", film.getDirectors()

        );
        return params;
    }

    // Вспомогательный метод для создания объекта Film из ResultSet
    public static Film createFilm(ResultSet rs, int rowNum) throws SQLException {
        Mpa mpa = createMpa(rs, rowNum);

        return Film.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date").toLocalDate())
                .duration(rs.getInt("duration"))
                .rating(rs.getInt("rating"))
                .mpa(mpa)
                .build();
    }

    // Метод для получения информации о MPA рейтинге по его идентификатору
    @Override
    public Mpa getMpaRating(Mpa mpa) {
        String sqlQuery = "SELECT * FROM MPARating WHERE id = ?";
        List<Mpa> mpas = jdbcTemplate.query(sqlQuery, MpaDbStorage::createMpa, mpa.getId());
        if (mpas.size() != 1) {
            throw new DataNotFoundException("При получении MPA по id список не равен 1");
        }
        return mpas.get(0);
    }

    // Метод для получения информации о GENRE по идентификатору фильма
    private List<Genre> getGenresForFilm(Long filmId) {
        String genresSql = "SELECT g.id as genre_id, g.genre_name " +
                "FROM FILM_GENRE fg " +
                "JOIN GENRES g ON fg.genre_id = g.id " +
                "WHERE fg.film_id = ?";
        try {
            return jdbcTemplate.query(genresSql, FilmDbStorage::createGenre, filmId);
        } catch (DataNotFoundException e) {
            // Если жанров нет, возвращаем пустой список
            return Collections.emptyList();
        }
    }

    // Вспомогательный метод для получения информации о MPA рейтинге по его идентификатору
    private Mpa getMpaRatingById(long mpaRatingId) {
        String sqlQuery = "SELECT * FROM MPARating WHERE id = ?";
        return jdbcTemplate.queryForObject(sqlQuery, MpaDbStorage::createMpa, mpaRatingId);
    }

    // Вспомогательный метод для создания объекта Genre из ResultSet
    public static Genre createGenre(ResultSet rs, int rowNum) throws SQLException {
        return Genre.builder()
                .id(rs.getLong("genre_id"))
                .name(rs.getString("genre_name"))
                .build();
    }

    // Вспомогательный метод для создания объекта Mpa из ResultSet
    public static Mpa createMpa(ResultSet rs, int rowNum) throws SQLException {
        return Mpa.builder()
                .id(rs.getLong("mpa_rating_id"))
                .name(rs.getString("mpa_rating_name"))
                .build();
    }

    // Вспомогательный метод для создания объекта Director из ResultSet
    static Director createDirector(ResultSet rs, int rowNum) throws SQLException {
        return Director.builder()
                .id(rs.getLong("director_id"))
                .name(rs.getString("director_name"))
                .build();
    }

    // Метод для добавления информации о жанрах в таблицу FILM_GENRE
    private void addGenresForFilm(Long filmId, List<Genre> genres) {

        // Добавляем информацию о новых жанрах в таблицу FILM_GENRE
        if (genres != null && !genres.isEmpty()) {
            for (Genre genre : genres) {
                jdbcTemplate.update("INSERT INTO FILM_GENRE (film_id, genre_id) VALUES (?, ?)",
                        filmId, genre.getId());
            }
        }
    }

    // Метод для добавления информации о жанрах в таблицу FILM_DIRECTOR
    private void addDirectorForFilm(Long filmId, List<Director> directors) {

        // Добавляем информацию о новых жанрах в таблицу FILM_DIRECTOR
        if (directors != null && !directors.isEmpty()) {
            for (Director director : directors) {
                jdbcTemplate.update("INSERT INTO FILM_DIRECTOR (film_id, director_id) VALUES (?, ?)",
                        filmId, director.getId());
            }
        }
    }

    // Метод для получения информации о DIRECTORS по идентификатору фильма
    private List<Director> getDirectorsForFilm(Long filmId) {
        String directorsSql = "SELECT d.id as director_id, d.director_name " +
                "FROM FILM_DIRECTOR fd " +
                "JOIN DIRECTORS d ON fd.director_id = d.id " +
                "WHERE fd.film_id = ?";
        try {
            return jdbcTemplate.query(directorsSql, DirectorDbStorage::createDirector, filmId);
        } catch (DataNotFoundException e) {
            // Если режиссеров нет, возвращаем пустой список
            return Collections.emptyList();
        }
    }


    // Метод для получения лайков по идентификатору фильма
    private int getLikesForFilm(Long filmId) {
        String sql = "SELECT COUNT(*) FROM LIKES WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId);
        if (count == null) {
            return 0;
        }
        return count;
    }

    //Метод для получения списка самых популярных фильмов указанного жанра за нужный год.
    @Override
    public List<Film> getPopularWithYearForYear(int limit, long genreId, int year) {
        String popWithYearFilmsSql = "SELECT f.*, m.rating_name AS mpa_rating_name " +
                "FROM FILMS f " +
                "LEFT JOIN MPARating m ON f.mpa_rating_id = m.id " +
                "WHERE EXTRACT(YEAR FROM f.release_date) = ? " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(popWithYearFilmsSql, FilmDbStorage::createFilm, year, limit);

        // Добавляем жанры и режиссеров к каждому фильму
        for (Film film : films) {
            List<Genre> genres = getGenresForFilm(film.getId());
            List<Director> directors = getDirectorsForFilm(film.getId());

            film.setGenres(genres);
            film.setDirectors(directors);

        }
        return films;
    }

    // Метод для получения списка самых популярных фильмов указанного жанра
    @Override
    public List<Film> getPopularWithGenre(int limit, Long genreId) {
        String popWithGenreFilmsSql = "SELECT f.*, m.rating_name AS mpa_rating_name " +
                "FROM FILMS f " +
                "LEFT JOIN MPARating m ON f.mpa_rating_id = m.id " +
                "LEFT JOIN FILM_GENRE fg ON f.id = fg.film_id " +
                "WHERE fg.genre_id = ? " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(popWithGenreFilmsSql, FilmDbStorage::createFilm, genreId, limit);

        // Добавляем жанры и режиссеров к каждому фильму
        for (Film film : films) {
            List<Genre> genres = getGenresForFilm(film.getId());
            List<Director> directors = getDirectorsForFilm(film.getId());

            film.setDirectors(directors);
            film.setGenres(genres);
        }
        return films;
    }

    // Метод для получения списка самых популярных фильмов за нужный год.
    @Override
    public List<Film> getPopularWithYear(int limit, Integer year) {
        String popWithYearFilmsSql = "SELECT f.*, m.rating_name AS mpa_rating_name " +
                "FROM FILMS f " +
                "LEFT JOIN MPARating m ON f.mpa_rating_id = m.id " +
                "WHERE EXTRACT(YEAR FROM f.release_date) = ?" +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(popWithYearFilmsSql, FilmDbStorage::createFilm, year, limit);

        // Добавляем жанры и режиссеров к каждому фильму
        for (Film film : films) {
            List<Genre> genres = getGenresForFilm(film.getId());
            List<Director> directors = getDirectorsForFilm(film.getId());

            film.setGenres(genres);
            film.setDirectors(directors);
        }
        return films;
    }

    // Метод для поиска фильма по запросу
    @Override
    public List<Film> searchFilmsByQuery(String query, String by) {
        String sql = "SELECT f.* FROM FILMS f LEFT JOIN FILM_DIRECTOR fd ON f.ID = fd.FILM_ID " +
                "LEFT JOIN DIRECTORS d ON fd.DIRECTOR_ID = d.ID ";

        if (by.equals("title")) {
            return jdbcTemplate.query(sql + "WHERE LOWER(f.NAME) LIKE ?", new FilmMapper(), query);
        }
        if (by.equals("director")) {
            return jdbcTemplate.query(sql + "WHERE LOWER(d.DIRECTOR_NAME) LIKE ?", new FilmMapper(), query);
        }
        if (by.equals("title,director") || by.equals("director,title")) {
            return jdbcTemplate.query(sql + "WHERE LOWER(f.NAME) LIKE ? OR LOWER(d.DIRECTOR_NAME) LIKE ?",
                    new FilmMapper(), query, query);
        }
        return new ArrayList<>();
    }

    //Вспомогательный класс для извлечения данных
    private class FilmMapper implements RowMapper<Film> {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {

            Film film = new Film();
            film.setId(rs.getLong("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));
            film.setRating(rs.getInt("rating"));
            film.setMpa(getMpaRatingById(rs.getLong("MPA_RATING_ID")));
            film.setGenres(getGenresForFilm(rs.getLong("id")));
            film.setDirectors(directorDbStorage.getDirectorsForFilm(rs.getLong("id")));
            film.setLikes((long) likesStorage.getLikesCountForFilm(rs.getLong("id")));
            return film;
        }
    }
    // Метод получения общих фильмов пользователей
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        List<Long> usersFilms = likesStorage.getAllFilmLikes(userId);
        List<Long> friendsFilms = likesStorage.getAllFilmLikes(friendId);

        if (usersFilms.isEmpty() || friendsFilms.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> commonFilmsId = new HashSet<>(usersFilms);
        commonFilmsId.addAll(friendsFilms);

        List<Film> films = new ArrayList<>();
        for (Long filmId : new HashSet<>(commonFilmsId)) {
            films.add(get(filmId));
        }
        return films.stream().sorted(Comparator.comparingLong(Film::getLikes).reversed())
                .collect(Collectors.toList());
    }

}

