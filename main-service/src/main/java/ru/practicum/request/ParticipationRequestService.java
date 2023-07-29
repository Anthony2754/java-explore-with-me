package ru.practicum.request;

import ru.practicum.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.request.dto.EventRequestStatusUpdateResultDto;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    ParticipationRequestDto createRequestForParticipation(Long userId, Long eventId);
    ParticipationRequestDto cancelRequestForParticipation(Long userId, Long requestId);
    List<ParticipationRequestDto> getRequestsForParticipation(Long userId);
    List<ParticipationRequestDto> getParticipationRequestsForUserEvent(Long userId, Long eventId);
    EventRequestStatusUpdateResultDto changeRequestsForParticipationStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequestDto eventRequestStatusUpdateRequest);
}
