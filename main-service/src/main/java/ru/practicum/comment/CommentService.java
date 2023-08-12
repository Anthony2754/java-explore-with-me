package ru.practicum.comment;

import ru.practicum.comment.dto.CommentResponseDto;
import ru.practicum.comment.dto.CommentDto;

import java.util.List;

public interface CommentService {

    CommentResponseDto createComment(Long userId, Long eventId, CommentDto commentDto);

    List<CommentResponseDto> getCommentsByEvent(Long eventId, int from, int size);

    CommentResponseDto getCommentById(Long commentId);

    CommentResponseDto updateComment(Long userId, Long commentId, CommentDto commentDto);

    void deleteComment(Long userId, Long commentId);

    CommentResponseDto updateStatusCommentByAdmin(Long commentId, boolean isConfirm);
}
