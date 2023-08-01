package ru.practicum.exceptions;

import javax.persistence.EntityNotFoundException;

public class EventNotFoundException extends EntityNotFoundException {
    public EventNotFoundException(long id) {
        super(String.format("Событие с id=%d не найдено", id));
    }

}
