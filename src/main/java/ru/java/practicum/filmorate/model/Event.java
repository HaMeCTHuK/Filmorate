package ru.java.practicum.filmorate.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.java.practicum.filmorate.event.EventOperation;
import ru.java.practicum.filmorate.event.EventType;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class Event extends BaseUnit {
    private Long timestamp;
    private Long userId;
    private EventType eventType;
    private EventOperation operation;
    private Long entityId;
}
