package ru.java.practicum.filmorate.event;

import ru.java.practicum.filmorate.model.Event;

import java.util.List;

// общий интерфейс для классов событий
public interface Events {
    void add(Long userId, Long entityId);
    void remove(Long userId, Long entityId);
    List<Event> getFeed(Long userId);
}
