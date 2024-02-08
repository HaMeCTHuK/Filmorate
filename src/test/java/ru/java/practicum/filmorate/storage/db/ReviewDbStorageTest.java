package ru.java.practicum.filmorate.storage.db;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import ru.java.practicum.filmorate.model.Film;
import ru.java.practicum.filmorate.model.Review;
import ru.java.practicum.filmorate.model.User;
import ru.java.practicum.filmorate.service.FilmService;
import ru.java.practicum.filmorate.service.MpaService;
import ru.java.practicum.filmorate.service.ReviewService;
import ru.java.practicum.filmorate.service.UserService;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ReviewDbStorageTest {

    private final ReviewService reviewService;
    private final FilmService filmService;
    private final UserService userService;
    private final MpaService mpaService;  // Добавлено
    private static Validator validator;

    static {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.usingContext().getValidator();
    }

    @Test
    public void shouldCreateReview() {
        User user = User.builder()
                .login("Iris")
                .name("Melissa")
                .email("catcat@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Film film = Film.builder()
                .name("Форрест Гамп")
                .description("Жизнь как коробка конфет")
                .duration(192)
                .releaseDate(LocalDate.of(1981, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        Review review = Review.builder()
                .content("Лучший фильм!")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);

        assertTrue(reviewService.getReviewsOfFilm(film.getId(), 1).contains(review));
    }

    @Test
    public void shouldNotPassContentValidation() {
        User user = User.builder()
                .login("Ivory")
                .name("Melissa")
                .email("Ivory@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Film film = Film.builder()
                .name("Мумия")
                .description("Фэнтези про Египет")
                .duration(192)
                .releaseDate(LocalDate.of(1999, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        Review review = Review.builder()
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();

        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertEquals(1, violations.size());
    }

    @Test
    public void shouldNotPassIsPositiveValidation() {
        User user = User.builder()
                .login("Rina")
                .name("Melissa")
                .email("Rina@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Film film = Film.builder()
                .name("Черный лебедь")
                .description("Фильм про балет")
                .duration(192)
                .releaseDate(LocalDate.of(2010, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        Review review = Review.builder()
                .content("Лучший фильм!")
                .userId(user.getId())
                .filmId(film.getId())
                .build();

        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertEquals(1, violations.size());
    }

    @Test
    public void shouldNotPassUserIdValidation() {
        User user = User.builder()
                .login("Lime")
                .name("Melissa")
                .email("Lime@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Film film = Film.builder()
                .name("2012")
                .description("Фильм про апокалипсис")
                .duration(192)
                .releaseDate(LocalDate.of(2009, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        Review review = Review.builder()
                .content("Лучший фильм!")
                .isPositive(true)
                .filmId(film.getId())
                .build();

        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertEquals(1, violations.size());
    }

    @Test
    public void shouldNotPassFilmIdValidation() {
        User user = User.builder()
                .login("Lemon")
                .name("Melissa")
                .email("Lemon@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Film film = Film.builder()
                .name("Дневник памяти")
                .description("Мелодрама о любви")
                .duration(192)
                .releaseDate(LocalDate.of(2004, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        Review review = Review.builder()
                .content("Лучший фильм!")
                .isPositive(true)
                .userId(user.getId())
                .build();

        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertEquals(1, violations.size());
    }

    @Test
    public void shouldUpdateReview() {
        User user = User.builder()
                .login("Melon")
                .name("Melissa")
                .email("Melon@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Film film = Film.builder()
                .name("Дневник памяти 2")
                .description("Мелодрама о любви")
                .duration(192)
                .releaseDate(LocalDate.of(2004, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        Review review = Review.builder()
                .content("Не очень")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);

        Review reviewUpdate = Review.builder()
                .reviewId(review.getReviewId())
                .content("Мне понравился")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.update(reviewUpdate);

        assertEquals(reviewUpdate, reviewService.getData(review.getReviewId()));
    }

    @Test
    public void shouldDeleteReview() {
        Film film = Film.builder()
                .name("Век Адалин")
                .description("Бессмертие от удара молнии")
                .duration(192)
                .releaseDate(LocalDate.of(2010, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        User user = User.builder()
                .login("Melody")
                .name("Melissa")
                .email("Melody@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Review review = Review.builder()
                .content("Не очень")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);
        reviewService.delete(review.getReviewId());

        assertFalse(reviewService.getReviewsOfFilm(film.getId(), 1).contains(review));
    }

    @Test
    public void shouldFindReviewById() {
        Film film = Film.builder()
                .name("Век Адалин 2")
                .description("Бессмертие от удара молнии")
                .duration(192)
                .releaseDate(LocalDate.of(2010, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        User user = User.builder()
                .login("Peony")
                .name("Melissa")
                .email("Peony@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Review review = Review.builder()
                .content("Не очень")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);

        assertEquals(review, reviewService.getData(review.getReviewId()));
    }

    @Test
    public void shouldGetReviewsOfFilm() {
        Film film = Film.builder()
                .name("Век Адалин 2")
                .description("Бессмертие от удара молнии")
                .duration(192)
                .releaseDate(LocalDate.of(2010, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        User user = User.builder()
                .login("Chestnut")
                .name("Melissa")
                .email("Chestnut@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Review review = Review.builder()
                .content("Не очень")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);

        Review review2 = Review.builder()
                .reviewId(review.getReviewId())
                .content("Мне понравился")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review2);

        assertEquals(List.of(review, review2), reviewService.getReviewsOfFilm(film.getId(), 2));
    }

    @Test
    public void shouldAddLike() {
        Film film = Film.builder()
                .name("Век Адалин 3")
                .description("Бессмертие от удара молнии")
                .duration(192)
                .releaseDate(LocalDate.of(2010, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        User user = User.builder()
                .login("Nat")
                .name("Melissa")
                .email("Nat@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Review review = Review.builder()
                .content("Неплохо")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);
        reviewService.addLike(review.getReviewId(), user.getId());

        assertEquals(1, reviewService.getData(review.getReviewId()).getUseful());
    }

    @Test
    public void shouldAddDislike() {
        Film film = Film.builder()
                .name("Век Адалин 4")
                .description("Бессмертие от удара молнии")
                .duration(192)
                .releaseDate(LocalDate.of(2010, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        User user = User.builder()
                .login("Cotton")
                .name("Melissa")
                .email("Cotton@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Review review = Review.builder()
                .content("Не очень")
                .isPositive(false)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);
        reviewService.addDislike(review.getReviewId(), user.getId());

        assertEquals(-1, reviewService.getData(review.getReviewId()).getUseful());
    }

    @Test
    public void shouldDeleteLike() {
        Film film = Film.builder()
                .name("Век Адалин 5")
                .description("Бессмертие от удара молнии")
                .duration(192)
                .releaseDate(LocalDate.of(2010, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        User user = User.builder()
                .login("Wool")
                .name("Melissa")
                .email("Wool@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Review review = Review.builder()
                .content("Неплохо")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);
        reviewService.addLike(review.getReviewId(), user.getId());
        reviewService.deleteLike(review.getReviewId(), user.getId());

        assertEquals(0, reviewService.getData(review.getReviewId()).getUseful());
    }

    @Test
    public void shouldDeleteDislike() {
        Film film = Film.builder()
                .name("Век Адалин 6")
                .description("Бессмертие от удара молнии")
                .duration(192)
                .releaseDate(LocalDate.of(2010, 12, 6))
                .mpa(mpaService.createMpa(1, "PG"))  // Исправлено
                .build();
        filmService.create(film);

        User user = User.builder()
                .login("Leaf")
                .name("Melissa")
                .email("Leaf@mail.ru")
                .birthday(LocalDate.of(2000, 8, 15))
                .build();
        userService.create(user);

        Review review = Review.builder()
                .content("Не очень")
                .isPositive(false)
                .userId(user.getId())
                .filmId(film.getId())
                .build();
        reviewService.create(review);
        reviewService.addDislike(review.getReviewId(), user.getId());
        reviewService.deleteDislike(review.getReviewId(), user.getId());

        assertEquals(0, reviewService.getData(review.getReviewId()).getUseful());
    }

}
