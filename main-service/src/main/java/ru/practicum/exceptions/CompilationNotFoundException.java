package ru.practicum.exceptions;

import javax.persistence.EntityNotFoundException;

public class CompilationNotFoundException extends EntityNotFoundException {
    public CompilationNotFoundException(Long id) {
        super(String.format("Подборка с id=%d не найдена", id));
    }
}
