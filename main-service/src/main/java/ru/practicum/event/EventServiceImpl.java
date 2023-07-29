package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.HitDto;
import ru.practicum.StatClient;
import ru.practicum.StatDto;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.exceptions.*;
import ru.practicum.location.Location;
import ru.practicum.location.LocationRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.event.dto.EventMapper.*;
import static ru.practicum.location.dto.LocationMapper.toLocation;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final StatClient statClient;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {

        log.info("Добавление нового события: user_id = " + userId + ", event = " + newEventDto);

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new CategoryNotFoundException(newEventDto.getCategory()));

        Event event = toEvent(newEventDto);

        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(2)))
            throw new RequestValidationException("Дата начала события не может быть раньше 2 часов от настоящего времени.");

        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setInitiator(user);
        event.setState(EventState.PENDING);
        event.setLocation(locationRepository.save(toLocation(newEventDto.getLocation())));
        event.setViews(0L);

        return toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, int from, int size) {

        log.info("Получение событий, добавленных текущим пользователем: user_id = " + userId + ", from = " + from +
                ", size = " + size);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        List<Event> events = eventRepository.findByInitiatorId(userId, PageRequest.of(from / size, size));

        return events.stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {

        log.info("Получение информации о событии, добавленном пользователем: user_id = " + userId +
                ", event_id = " + eventId);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return toEventFullDto(eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId)));
    }

    @Override
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequestDto updateEventUserRequestDto) {

        log.info("Обновление информации о событии: user_id = " + userId + ", event_id = " + eventId +
                ", update_event = " + updateEventUserRequestDto);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));

        if (event.getState() != null && event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED)
            throw new ForbiddenException("Изменить возможно только отмененные или ожидающие публикации события");

        if (updateEventUserRequestDto.getEventDate() != null
                && LocalDateTime.parse(updateEventUserRequestDto.getEventDate(), formatter)
                .isBefore(LocalDateTime.now().plusHours(2)))
            throw new RequestValidationException(String.format("Дата начала события не может быть раньше 2 часов от настоящего времени. " +
                            "Новая дата начала: %s",
                    updateEventUserRequestDto.getEventDate()));

        if (updateEventUserRequestDto.getTitle() != null)
            event.setTitle(updateEventUserRequestDto.getTitle());

        if (updateEventUserRequestDto.getAnnotation() != null)
            event.setAnnotation(updateEventUserRequestDto.getAnnotation());

        if (updateEventUserRequestDto.getCategory() != null)
            event.setCategory(categoryRepository.findById(updateEventUserRequestDto.getCategory())
                    .orElseThrow(() -> new CategoryNotFoundException(updateEventUserRequestDto.getCategory())));

        if (updateEventUserRequestDto.getDescription() != null)
            event.setDescription(updateEventUserRequestDto.getDescription());

        if (updateEventUserRequestDto.getEventDate() != null)
            event.setEventDate(LocalDateTime.parse(updateEventUserRequestDto.getEventDate(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        if (updateEventUserRequestDto.getLocation() != null) {
            Location location = event.getLocation();
            location.setLat(updateEventUserRequestDto.getLocation().getLat());
            location.setLon(updateEventUserRequestDto.getLocation().getLon());
            event.setLocation(location);
            locationRepository.save(location);
        }
        if (updateEventUserRequestDto.getPaid() != null)
            event.setPaid(updateEventUserRequestDto.getPaid());

        if (updateEventUserRequestDto.getParticipantLimit() != null)
            event.setParticipantLimit(updateEventUserRequestDto.getParticipantLimit());

        if (updateEventUserRequestDto.getRequestModeration() != null)
            event.setRequestModeration(updateEventUserRequestDto.getRequestModeration());

        if (updateEventUserRequestDto.getParticipantLimit() != null)
            event.setParticipantLimit(updateEventUserRequestDto.getParticipantLimit());

        if (updateEventUserRequestDto.getStateAction() != null) {
            if (updateEventUserRequestDto.getStateAction() == StateUserAction.CANCEL_REVIEW)
                event.setState(EventState.CANCELED);

            if (updateEventUserRequestDto.getStateAction() == StateUserAction.SEND_TO_REVIEW)
                event.setState(EventState.PENDING);
        }
        return toEventFullDto(eventRepository.save(event));
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequestDto updateEventAdminRequestDto) {
        log.info("Обновление события администратором: event_id = " + eventId + ", update_event = " + updateEventAdminRequestDto);

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));

        if (updateEventAdminRequestDto.getStateAction() != null) {
            if (updateEventAdminRequestDto.getStateAction() == StateAdminAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING)
                    throw new ForbiddenException("Невозможно опубликовать событие с неправильным статусом: " + event.getState());

                if (event.getPublishedOn() != null && event.getEventDate().isAfter(event.getPublishedOn().minusHours(1)))
                    throw new RequestValidationException("Невозможно опубликовать событие так как оно начинается менее чем через час");
                event.setPublishedOn(LocalDateTime.now());
                event.setState(EventState.PUBLISHED);
            }
            if (updateEventAdminRequestDto.getStateAction() == StateAdminAction.REJECT_EVENT)
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ForbiddenException("невозможно отклонить объявление так как оно уже опубликовано");
                } else {
                    event.setState(EventState.CANCELED);
                }
        }

        if (updateEventAdminRequestDto.getEventDate() != null
                && LocalDateTime.parse(updateEventAdminRequestDto.getEventDate(), formatter)
                .isBefore(LocalDateTime.now().plusHours(2)))
            throw new RequestValidationException(String.format("Дата начала события не может быть раньше 2 часов от текущего времени %s",
                    updateEventAdminRequestDto.getEventDate()));

        if (updateEventAdminRequestDto.getTitle() != null)
            event.setTitle(updateEventAdminRequestDto.getTitle());

        if (updateEventAdminRequestDto.getAnnotation() != null)
            event.setAnnotation(updateEventAdminRequestDto.getAnnotation());

        if (updateEventAdminRequestDto.getCategory() != null)
            event.setCategory(categoryRepository.findById(updateEventAdminRequestDto.getCategory())
                    .orElseThrow(() -> new CategoryNotFoundException(updateEventAdminRequestDto.getCategory())));

        if (updateEventAdminRequestDto.getDescription() != null)
            event.setDescription(updateEventAdminRequestDto.getDescription());

        if (updateEventAdminRequestDto.getEventDate() != null)
            event.setEventDate(LocalDateTime.parse(updateEventAdminRequestDto.getEventDate(), formatter));

        if (updateEventAdminRequestDto.getLocation() != null) {
            Location location = event.getLocation();
            location.setLat(updateEventAdminRequestDto.getLocation().getLat());
            location.setLon(updateEventAdminRequestDto.getLocation().getLon());
            event.setLocation(location);
            locationRepository.save(location);
        }
        if (updateEventAdminRequestDto.getPaid() != null)
            event.setPaid(updateEventAdminRequestDto.getPaid());

        if (updateEventAdminRequestDto.getParticipantLimit() != null)
            event.setParticipantLimit(updateEventAdminRequestDto.getParticipantLimit());

        if (updateEventAdminRequestDto.getRequestModeration() != null)
            event.setRequestModeration(updateEventAdminRequestDto.getRequestModeration());

        if (updateEventAdminRequestDto.getParticipantLimit() != null)
            event.setParticipantLimit(updateEventAdminRequestDto.getParticipantLimit());

        return toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               String rangeStart, String rangeEnd, int from, int size) {

        log.info("Поиск событий по параметрам: user_ids = " + users + ", states = " + states +
                ", categories = " + categories + ", rangeStart = " + rangeStart + ", rangeEnd = " + rangeEnd);

        validateEventStates(states);

        List<Event> events = eventRepository.findEvents(users,
                states, categories,
                rangeStart != null ? LocalDateTime.parse(rangeStart, formatter) : null,
                rangeEnd != null ? LocalDateTime.parse(rangeEnd, formatter) : null,
                PageRequest.of(from / size, size));

        return events
                .stream()
                .map(EventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getPublishedEvents(String text, List<Long> categories, Boolean paid, String rangeStart,
                                                  String rangeEnd, boolean onlyAvailable, String sort, int from, int size,
                                                  HttpServletRequest request) {
        log.info("Поиск событий по параметрам: text = " + text + ", categories = " + categories +
                ", paid = " + paid + ", rangeStart = " + rangeStart + ", rangeEnd = " + rangeEnd +
                ", onlyAvailable = " + onlyAvailable + ", sort = " + sort + ", from = " + from +
                ", size = " + size);

        log.info("Ip: {}", request.getRemoteAddr());
        log.info("Endpoint: {}", request.getRequestURI());

        statClient.addHit(HitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(formatter))
                .build());

        if (rangeStart != null && rangeEnd != null &&
                LocalDateTime.parse(rangeStart, formatter).isAfter(LocalDateTime.parse(rangeEnd, formatter)))
            throw new RequestValidationException("Дата начала события должна быть раньше даты окончания");

        List<Event> events = eventRepository.findPublishedEvents(
                    text,
                    categories,
                    paid,
                    rangeStart != null ? LocalDateTime.parse(rangeStart, formatter) : LocalDateTime.now(),
                    rangeEnd != null ? LocalDateTime.parse(rangeEnd, formatter) : null,
                    PageRequest.of(from / size, size));

        List<EventShortDto> eventShortDto = Collections.emptyList();
        if (events != null) {
            eventShortDto = events.stream().map(EventMapper::toEventShortDto).collect(Collectors.toList());

            if (onlyAvailable) {
                eventShortDto = events.stream().filter(event ->
                        countConfirmedRequests(event.getRequests()) < event.getParticipantLimit()
                ).map(EventMapper::toEventShortDto).collect(Collectors.toList());
            }

            if (sort != null) {
                switch (EventSort.valueOf(sort)) {
                    case EVENT_DATE:
                        eventShortDto.sort(Comparator.comparing(EventShortDto::getEventDate));
                        break;

                    case VIEWS:
                        eventShortDto.sort(Comparator.comparing(EventShortDto::getViews));
                        break;
                    default: throw new RequestValidationException("Не верные параметры сортировки");
                }
            }
        }
        return eventShortDto;
    }

    @Override
    public EventFullDto getPublishedEventById(Long eventId, HttpServletRequest request) {

        log.info("Получение информации об опубликованном событии по id: event_id = " + eventId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        Integer countHits = getCountHits(request);

        statClient.addHit(HitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(formatter))
                .build());

        Integer newCountHits = getCountHits(request);

        if (newCountHits != null && newCountHits > countHits) {
            event.setViews(event.getViews() + 1);
            eventRepository.save(event);
        }
        return toEventFullDto(event);
    }

    private void validateEventStates(List<String> states) {
        if (states != null)
            for (String state : states)
                try {
                    EventState.valueOf(state);
                } catch (IllegalArgumentException e) {
                    throw new RequestValidationException("Неправильный статус");
                }
    }

    private Integer getCountHits(HttpServletRequest request) {

        log.info("Ip: {}", request.getRemoteAddr());
        log.info("Endpoint: {}", request.getRequestURI());

        ResponseEntity<StatDto[]> response = statClient.getStats(
                LocalDateTime.now().minusYears(100).format(formatter),
                LocalDateTime.now().format(formatter),
                new String[] {request.getRequestURI()},
                true);
        Optional<StatDto> statDto;
        int hits = 0;
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            statDto = Arrays.stream(response.getBody()).findFirst();
            if (statDto.isPresent())
                hits = statDto.get().getHits();
        }
        return hits;
    }
}
