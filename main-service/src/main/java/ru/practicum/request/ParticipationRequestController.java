package ru.practicum.request;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.request.dto.EventRequestStatusUpdateResultDto;
import ru.practicum.request.dto.ParticipationRequestDto;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ParticipationRequestController {

    private final ParticipationRequestService participationRequestService;

    @PostMapping("/users/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequestForParticipation(
            @PathVariable @Valid @Positive Long userId,
            @RequestParam @Valid @Positive Long eventId) {
       return participationRequestService.createRequestForParticipation(userId, eventId);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequestForParticipation(
            @PathVariable @Valid @Positive Long userId,
            @PathVariable @Valid @Positive Long requestId) {
        return participationRequestService.cancelRequestForParticipation(userId, requestId);
    }

    @GetMapping("/users/{userId}/requests")
    public List<ParticipationRequestDto> getRequestForParticipation(
            @PathVariable @Valid @Positive Long userId) {
        return participationRequestService.getRequestsForParticipation(userId);
    }

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getParticipationRequestsForUserEvent(
            @PathVariable @Valid @Positive Long userId,
            @PathVariable @Valid @Positive Long eventId) {
        return participationRequestService.getParticipationRequestsForUserEvent(userId, eventId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResultDto changeRequestForParticipationStatus(
            @PathVariable @Valid @Positive Long userId,
            @PathVariable @Valid @Positive Long eventId,
            @RequestBody EventRequestStatusUpdateRequestDto eventRequestStatusUpdateRequest) {
        return participationRequestService.changeRequestsForParticipationStatus(userId, eventId, eventRequestStatusUpdateRequest);
    }
}
