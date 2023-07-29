package ru.practicum.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.event.EventState;
import ru.practicum.exceptions.EventNotFoundException;
import ru.practicum.exceptions.ForbiddenException;
import ru.practicum.exceptions.UserNotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequestDto;
import ru.practicum.request.dto.EventRequestStatusUpdateResultDto;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.ParticipationRequestMapper;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.practicum.request.dto.ParticipationRequestMapper.toParticipationRequestDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository participationRequestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public ParticipationRequestDto createRequestForParticipation(Long userId, Long eventId) {

        log.info("Создание запроса от пользователя на участие в событии: user_id = " + userId + ", event_id = " + eventId);

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));

        ParticipationRequest existParticipationRequest = participationRequestRepository.findByRequesterIdAndEventId(userId, eventId);

        if (existParticipationRequest != null) {
            log.info("Пользователь с id = " + userId + " не может добавить повторный запрос с id = " + eventId);
            throw new ForbiddenException("Попытка создания повторного запроса на участие");
        }

        if (event.getInitiator().getId().equals(userId)) {
            log.info("Пользователь с id = " + userId + " не может добавить запрос на участие в своём событии с id = " + eventId);
            throw new ForbiddenException("Попытка добавления запроса на участие в собственном событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            log.info("Пользователь с id = " + userId + " не может участвовать в неопубликованном событии с id = " + eventId);
            throw new ForbiddenException("Попытка добавления запроса на неопубликованное событие");
        }

        if (event.getParticipantLimit() != 0 && participationRequestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            log.info("Пользователь с id = " + userId + " не может участвовать в событии с id = " + eventId +
                    ", так как достигнуто максимальное количество запросов на участие");
            throw new ForbiddenException("Попытка добавления запроса на участие в событии с максимальным количеством запросов");
        }

        ParticipationRequestStatus status = ParticipationRequestStatus.PENDING;

        if (!event.isRequestModeration() || event.getParticipantLimit() == 0)
            status = ParticipationRequestStatus.CONFIRMED;

        ParticipationRequest newParticipationRequest = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        return toParticipationRequestDto(participationRequestRepository.save(newParticipationRequest));
    }

    @Override
    public ParticipationRequestDto cancelRequestForParticipation(Long userId, Long requestId) {

        log.info("Отмена своего запроса на участие в событии: user_id = " + userId + ", request_id = " + requestId);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        ParticipationRequest requestToUpdate = participationRequestRepository.getReferenceById(requestId);
        requestToUpdate.setStatus(ParticipationRequestStatus.CANCELED);

        return toParticipationRequestDto(participationRequestRepository.save(requestToUpdate));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsForParticipation(Long userId) {

        log.info("Получение информации о заявках пользователя на участие в событиях: user_id = " + userId);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        List<Optional<ParticipationRequest>> requests = participationRequestRepository.findByRequesterId(userId);

        return requests.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ParticipationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> getParticipationRequestsForUserEvent(Long userId, Long eventId) {

        log.info("Получение информации о запросах на участие в событии пользователя: user_id = " + userId +
                ", event_id = " + eventId);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        List<Event> userEvents = eventRepository.findByIdAndInitiatorId(eventId, userId);
        List<Optional<ParticipationRequest>> requests = participationRequestRepository.findByEventIn(userEvents);

        return requests.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ParticipationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResultDto changeRequestsForParticipationStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequestDto eventRequestStatusUpdateRequestDto) {

        log.info("Изменение статуса заявок на участие в событии пользователем: " +
                "user_id = " + userId + ", event_id = " + eventId + ", новый статус = " + eventRequestStatusUpdateRequestDto);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        List<ParticipationRequest> requests = participationRequestRepository.findAllById(eventRequestStatusUpdateRequestDto.getRequestIds());

        EventRequestStatusUpdateResultDto eventRequestStatusUpdateResultDto = EventRequestStatusUpdateResultDto.builder()
                .confirmedRequests(new ArrayList<>())
                .rejectedRequests(new ArrayList<>())
                .build();

        if (!requests.isEmpty()) {

            if (ParticipationRequestStatus.valueOf(eventRequestStatusUpdateRequestDto.getStatus()) == ParticipationRequestStatus.CONFIRMED) {
                int limitParticipants = event.getParticipantLimit();

                if (limitParticipants == 0 || !event.isRequestModeration())
                    throw new ForbiddenException("Запрос отклонен: лимит участников равен 0 или не пройдена модерация");

                Integer countParticipants = participationRequestRepository.countByEventIdAndStatus(event.getId(), ParticipationRequestStatus.CONFIRMED);

                if (countParticipants == limitParticipants)
                    throw new ForbiddenException("Достигнуто максимальное количество заявок на участие");

                for (ParticipationRequest request : requests) {

                    if (request.getStatus() != ParticipationRequestStatus.PENDING)
                        throw new ForbiddenException("Статус запроса должен быть PENDING");

                    if (countParticipants < limitParticipants) {

                        request.setStatus(ParticipationRequestStatus.CONFIRMED);
                        eventRequestStatusUpdateResultDto.getConfirmedRequests().add(toParticipationRequestDto(request));
                        countParticipants++;
                    } else {
                        request.setStatus(ParticipationRequestStatus.REJECTED);
                        eventRequestStatusUpdateResultDto.getRejectedRequests().add(toParticipationRequestDto(request));
                    }
                }
                participationRequestRepository.saveAll(requests);

                if (countParticipants == limitParticipants) {
                    participationRequestRepository.updateRequestStatusByEventIdAndStatus(event,
                            ParticipationRequestStatus.PENDING, ParticipationRequestStatus.REJECTED);
                }

            } else if (ParticipationRequestStatus.valueOf(eventRequestStatusUpdateRequestDto.getStatus()) == ParticipationRequestStatus.REJECTED) {

                for (ParticipationRequest request : requests) {

                    if (request.getStatus() != ParticipationRequestStatus.PENDING)
                        throw new ForbiddenException("Status of request doesn't PENDING");

                    request.setStatus(ParticipationRequestStatus.REJECTED);
                    eventRequestStatusUpdateResultDto.getRejectedRequests().add(toParticipationRequestDto(request));
                }
                participationRequestRepository.saveAll(requests);
            }
        }
        return eventRequestStatusUpdateResultDto;
    }

}
