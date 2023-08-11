package ru.practicum.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentResponseDto;
import ru.practicum.comment.dto.CommentDto;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/user/{userId}/events/{eventId}/comment/")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto createComment(
            @PathVariable @Valid @Positive Long userId,
            @PathVariable @Valid @Positive Long eventId,
            @RequestBody @Validated CommentDto commentDto) {
        return commentService.createComment(userId, eventId, commentDto);
    }

    @GetMapping("/events/{eventId}/comments")
    public List<CommentResponseDto> getCommentsByEvent(
            @PathVariable @Valid @Positive Long eventId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        return commentService.getCommentsByEvent(eventId, from, size);
    }

    @GetMapping("/comment/{commentId}")
    public CommentResponseDto getCommentById(
            @PathVariable @Valid @Positive Long commentId) {
        return commentService.getCommentById(commentId);
    }

    @PatchMapping("/user/{userId}/comment/{commentId}")
    public CommentResponseDto updateComment(
            @PathVariable @Valid @Positive Long userId,
            @PathVariable @Valid @Positive Long commentId,
            @RequestBody @Validated CommentDto commentDto) {
        return commentService.updateComment(userId, commentId, commentDto);
    }

    @DeleteMapping("/user/{userId}/comment/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable @Valid @Positive Long userId,
            @PathVariable @Valid @Positive Long commentId) {
        commentService.deleteComment(userId, commentId);
    }

    @PatchMapping("/admin/comment/{commentId}")
    public CommentResponseDto updateStatusCommentByAdmin(
            @PathVariable @Valid @Positive Long commentId,
            @RequestParam boolean isConfirm) {
        return commentService.updateStatusCommentByAdmin(commentId, isConfirm);
    }
}
