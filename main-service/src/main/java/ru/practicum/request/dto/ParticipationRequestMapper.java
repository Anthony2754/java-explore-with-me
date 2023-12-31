package ru.practicum.request.dto;

import lombok.experimental.UtilityClass;
import ru.practicum.request.ParticipationRequest;

import java.time.format.DateTimeFormatter;

@UtilityClass
public class ParticipationRequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().toString())
                .created(request.getCreated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }
}
