package ru.practicum.comment.dto;

import lombok.experimental.UtilityClass;
import ru.practicum.comment.Comment;

import java.time.format.DateTimeFormatter;

import static ru.practicum.event.dto.EventMapper.toEventShortDto;
import static ru.practicum.user.dto.UserMapper.toUserShortDto;

@UtilityClass
public class CommentMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Comment toComment(CommentDto commentDto) {
        return Comment.builder()
                .text(commentDto.getText())
                .build();
    }

    public static CommentResponseDto toCommentResponseDto(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .event(toEventShortDto(comment.getEvent()))
                .author(toUserShortDto(comment.getAuthor()))
                .text(comment.getText())
                .state(comment.getState().toString())
                .createdOn(comment.getCreatedOn().format(FORMATTER))
                .updatedOn(comment.getUpdatedOn() != null ? comment.getUpdatedOn().format(FORMATTER) : null)
                .build();
    }

}
