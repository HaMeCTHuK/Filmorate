package ru.java.practicum.filmorate.exception; // add-reviews - file 2, old one deleted - ReviewDoesNotExist

public class DataNotFoundException extends RuntimeException {
    public DataNotFoundException(String message) {
        super(message);
    }
}
