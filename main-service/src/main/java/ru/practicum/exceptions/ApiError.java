package ru.practicum.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ApiError {

    private String status;
    private String reason;
    private String message;
    private String timestamp;
}