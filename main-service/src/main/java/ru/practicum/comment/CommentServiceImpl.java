package ru.practicum.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentMapper;
import ru.practicum.comment.dto.CommentResponseDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.event.EventState;
import ru.practicum.exceptions.CommentNotFoundException;
import ru.practicum.exceptions.EventNotFoundException;
import ru.practicum.exceptions.ForbiddenException;
import ru.practicum.exceptions.UserNotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.comment.dto.CommentMapper.toComment;
import static ru.practicum.comment.dto.CommentMapper.toCommentResponseDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentResponseDto createComment(Long userId, Long eventId, CommentDto commentDto) {

        log.info("Создание комментария пользователем= {}, под событием= {}: {}", userId, eventId, commentDto);

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        Comment comment = toComment(commentDto);
        comment.setEvent(event);
        comment.setAuthor(user);
        comment.setState(CommentState.PENDING);
        comment.setCreatedOn(LocalDateTime.now());

        return toCommentResponseDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public List<CommentResponseDto> getCommentsByEvent(Long eventId, int from, int size) {

        log.info("получения комментария по событию= {}", eventId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        List<Comment> comments = commentRepository.findByEvent(event, PageRequest.of(from / size, size));

        return comments.stream().map(CommentMapper::toCommentResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponseDto getCommentById(Long commentId) {

        log.info("Получение комментария по id= {}", commentId);

        return toCommentResponseDto(commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId)));
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(Long userId, Long commentId, CommentDto commentDto) {

        log.info("Обновлен комментарий с id= {} пользователем= {}: {}", commentId, userId, commentDto);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Невозможно обновить комментарий другого пользователя");
        }

        if (comment.getState() == CommentState.CONFIRMED) {
            throw new ForbiddenException("Невозможно обновить подтвержденный комментарий");
        }

        comment.setText(commentDto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
        comment.setState(CommentState.PENDING);

        return toCommentResponseDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {

        log.info("Удален коммент с id = {} пользователем = {}", commentId, userId);

        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Невозможно удалить комментарий другого пользователя");
        }

        if (comment.getState() == CommentState.CONFIRMED) {
            throw new ForbiddenException("Невозможно удалить подтвержденный комментарий");
        }

        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public CommentResponseDto updateStatusCommentByAdmin(Long commentId, boolean isConfirm) {

        log.info("Обновлен статус комментария с id = {}. Новый статус: {}", commentId, isConfirm);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (isConfirm) {
            comment.setState(CommentState.CONFIRMED);
        } else {
            comment.setState(CommentState.REJECTED);
        }

        comment.setUpdatedOn(LocalDateTime.now());

        return toCommentResponseDto(commentRepository.save(comment));
    }
}
