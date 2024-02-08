package ru.java.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.java.practicum.filmorate.event.Events;
import ru.java.practicum.filmorate.event.LikeEvents;
import ru.java.practicum.filmorate.model.Review;
import ru.java.practicum.filmorate.storage.EventsStorage;
import ru.java.practicum.filmorate.storage.ReviewStorage;


import java.util.List;

@Slf4j
@Service
public class ReviewService extends AbstractService<Review> {

    private final ReviewStorage reviewStorage;
    private final Events event;

    @Autowired
    public ReviewService(ReviewStorage reviewStorage, EventsStorage eventsStorage) {
        this.reviewStorage = reviewStorage;  //Для вызова уникальных методов Review
        this.abstractStorage = reviewStorage; //Для вызова общих CRUD методов
        this.event = new LikeEvents(eventsStorage);

    }

    @Override
    public void validate(Review review) {
        if (review.getContent().length() > 200) {
            throw new IllegalArgumentException("Максимальная длина отзыва — 200 символов");
        }
        if (review.getContent().isEmpty()) {
            throw new IllegalArgumentException("Отзыв не может быть пустым");
        }

    }

    @Override
    public void validateParameter(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Некорректные параметры поля, проверь null или отрицательное значение");
        }
        if (getData(id) == null) {
            throw new IllegalArgumentException("Отзыва с указанным ID не существует");
        }
    }

    @Override
    public void validateParameters(Long id, Long otherId) {
        throw new UnsupportedOperationException("Метод не используется");
    }

    public List<Review> getReviewsOfFilm(long filmId, int count) {
        log.info("Получаем отзыв о фильме");
        return reviewStorage.getReviewsOfFilm(filmId, count);
    }

    public void addLike(long reviewId, long userId) {
        log.info("Добавляем лайк от пользователя к отзыву");
        reviewStorage.addLike(reviewId, userId);
        event.add(userId, reviewId);
    }

    public void addDislike(long reviewId, long userId) {
        log.info("Добавляем дизлайк от пользователя к отзыву");
        reviewStorage.addDislike(reviewId, userId);
        event.add(userId, reviewId);
    }

    public void deleteLike(long reviewId, long userId) {
        log.info("Удаляем лайк от пользователя к отзыву");
        reviewStorage.deleteLike(reviewId, userId);
        event.remove(userId, reviewId);

    }

    public void deleteDislike(long reviewId, long userId) {
        log.info("Удаляем дизлайк от пользователя к отзыву");
        reviewStorage.deleteDislike(reviewId, userId);
        event.remove(userId, reviewId);
    }
}
