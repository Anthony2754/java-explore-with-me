package ru.practicum.exceptions;

import javax.persistence.EntityNotFoundException;

public class CategoryNotFoundException extends EntityNotFoundException {
    public CategoryNotFoundException(long id) {
        super(String.format("Категория с id=%d не найдена", id));
    }

}
