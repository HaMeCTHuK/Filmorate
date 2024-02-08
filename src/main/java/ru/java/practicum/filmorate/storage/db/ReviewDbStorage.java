package ru.java.practicum.filmorate.storage.db; // add-reviews - file 5

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.java.practicum.filmorate.exception.DataNotFoundException;
import ru.java.practicum.filmorate.model.Film;
import ru.java.practicum.filmorate.model.Review;
import ru.java.practicum.filmorate.model.User;
import ru.java.practicum.filmorate.storage.FilmStorage;
import ru.java.practicum.filmorate.storage.ReviewStorage;
import ru.java.practicum.filmorate.storage.UserStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final String QUERY_TO_SET_USEFUL = "UPDATE REVIEW SET USEFUL = ((SELECT COUNT(REVIEW_ID) " +
            "FROM REVIEW_LIKE WHERE REVIEW_ID = ?) - (SELECT COUNT(REVIEW_ID) FROM REVIEW_DISLIKE " +
            "WHERE REVIEW_ID = ?)) WHERE REVIEW_ID = ?;";

    public ReviewDbStorage(JdbcTemplate jdbcTemplate, @Qualifier("filmDbStorage") FilmStorage filmStorage,
                           @Qualifier("userDbStorage") UserStorage userStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    @Override
    //Метод для создания нового отзыва в базе данных.
    public Review create(Review review) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        User user = userStorage.get(review.getUserId());
        Film film = filmStorage.get(review.getFilmId());
        String sqlQuery = "INSERT INTO REVIEW (CONTENT, IS_POSITIVE, USER_ID, FILM_ID, USEFUL) VALUES " +
                "(?, ?, ?, ?, ?);";
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con
                    .prepareStatement(sqlQuery, new String[]{"review_id"});
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setLong(3, review.getUserId());
            ps.setLong(4, review.getFilmId());
            ps.setLong(5, review.getUseful());
            return ps;
        }, keyHolder);
        review.setReviewId((Objects.requireNonNull(keyHolder.getKey()).longValue()));
        setRightUseful(review.getReviewId());
        return get(review.getReviewId());
    }

    @Override
    // Метод для обновления отзыва в базе данных.
    public Review update(Review review) {
        String sqlQuery = "UPDATE REVIEW SET CONTENT = ?, IS_POSITIVE = ? WHERE REVIEW_ID = ?;";
        jdbcTemplate.update(sqlQuery, review.getContent(), review.getIsPositive(), review.getReviewId());
        return get(review.getReviewId());
    }

    @Override
    // Метод для удаление отзыва в базе данных.
    public void delete(Long id) {
        String sqlQuery = "DELETE FROM REVIEW WHERE REVIEW_ID = ?;";
        jdbcTemplate.update(sqlQuery, id);
    }

    @Override
    // Метод для получения всех отзывов.
    public List<Review> getAll() {
        String query = "SELECT * FROM REVIEW";
        return jdbcTemplate.query(query, this::mapRowToReviewFromResultSet);
    }

    @Override
    //  Метод для получения отзыва о фильме из базы данных по id.
    public Review get(Long id) {
        String sqlQuery = "SELECT * FROM REVIEW WHERE REVIEW_ID = ?;";
        SqlRowSet reviewRows = jdbcTemplate.queryForRowSet(sqlQuery, id);
        if (reviewRows.next()) {
            return mapRowToReviewFromSqlRowSet(reviewRows, 0);
        }
        throw new DataNotFoundException("Отзыв не найден");
    }

    @Override
    // Метод для получения списка отзывов о фильме из базы данных.
    public List<Review> getReviewsOfFilm(long filmId, int count) {
        String queryWithFilmId = "SELECT * FROM REVIEW WHERE FILM_ID = ? ORDER BY USEFUL DESC LIMIT ?";
        String queryWithoutFilmId = "SELECT * FROM REVIEW ORDER BY USEFUL DESC LIMIT ?";
        if (filmId == 0) {
            return jdbcTemplate.query(queryWithoutFilmId, this::mapRowToReviewFromResultSet, count);
        }
        return jdbcTemplate.query(queryWithFilmId, this::mapRowToReviewFromResultSet, filmId, count);
    }

    @Override
    // Метод для добавления лайка к отзыву в базе данных.
    public void addLike(long reviewId, long userId) {
        Review review = get(reviewId);
        User user = userStorage.get(userId);
        String queryToAddLike = "INSERT INTO REVIEW_LIKE (REVIEW_ID, USER_ID) VALUES (?, ?);";
        jdbcTemplate.update(queryToAddLike, reviewId, userId);
        setRightUseful(reviewId);
    }

    @Override
    // Метод для добавления дизлайка к отзыву в базе данных.
    public void addDislike(long reviewId, long userId) {
        Review review = get(reviewId);
        User user = userStorage.get(userId);
        String queryToAddDislike = "INSERT INTO REVIEW_DISLIKE (REVIEW_ID, USER_ID) VALUES (?, ?);";
        jdbcTemplate.update(queryToAddDislike, reviewId, userId);
        setRightUseful(reviewId);
    }

    @Override
    // Метод для удаления лайка отзыва в базе данных.
    public void deleteLike(long reviewId, long userId) {
        Review review = get(reviewId);
        User user = userStorage.get(userId);
        String queryToDeleteLike = "DELETE FROM REVIEW_LIKE WHERE REVIEW_ID = ? AND USER_ID = ?";
        jdbcTemplate.update(queryToDeleteLike, reviewId, userId);
        setRightUseful(reviewId);
    }

    @Override
    // Метод для удаления дизлайка отзыва в базе данных.
    public void deleteDislike(long reviewId, long userId) {
        Review review = get(reviewId);
        User user = userStorage.get(userId);
        String queryToDeleteDislike = "DELETE FROM REVIEW_DISLIKE WHERE REVIEW_ID = ? AND USER_ID = ?";
        jdbcTemplate.update(queryToDeleteDislike, reviewId, userId);
        setRightUseful(reviewId);
    }

    // Вспомогательный метод для работы с данными ResultSet
    private Review mapRowToReviewFromResultSet(ResultSet rs, int rowNum) throws SQLException {
        return mapRowToReview(rs.getLong("REVIEW_ID"),
                rs.getString("CONTENT"),
                rs.getBoolean("IS_POSITIVE"),
                rs.getLong("USER_ID"),
                rs.getLong("FILM_ID"),
                rs.getLong("USEFUL"));
    }

    // Вспомогательный метод для работы с данными SqlRowSet
    private Review mapRowToReviewFromSqlRowSet(SqlRowSet rs, int rowNum) {
        return mapRowToReview(rs.getLong("REVIEW_ID"),
                rs.getString("CONTENT"),
                rs.getBoolean("IS_POSITIVE"),
                rs.getLong("USER_ID"),
                rs.getLong("FILM_ID"),
                rs.getLong("USEFUL"));
    }

    private Review mapRowToReview(long reviewId, String content, boolean isPositive, long userId, long filmId, long useful) {
        return Review.builder()
                .reviewId(reviewId)
                .content(content)
                .isPositive(isPositive)
                .userId(userId)
                .filmId(filmId)
                .useful(useful)
                .build();
    }

    //Приватный метод для коррекции значения поля USEFUL в отзыве в базе данных.
    private void setRightUseful(long reviewId) {
        jdbcTemplate.update(QUERY_TO_SET_USEFUL, reviewId, reviewId, reviewId);
    }
}
