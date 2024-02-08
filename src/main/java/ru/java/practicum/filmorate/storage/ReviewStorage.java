package ru.java.practicum.filmorate.storage; // add-reviews - file 6

import ru.java.practicum.filmorate.model.Review;

import java.util.List;

public interface ReviewStorage extends AbstractStorage<Review>{

    List<Review> getReviewsOfFilm(long filmId, int count);

    void addLike(long reviewId, long userId);

    void addDislike(long reviewId, long userId);

    void deleteLike(long reviewId, long userId);

    void deleteDislike(long reviewId, long userId);
}
