package ru.practicum.model;

import lombok.experimental.UtilityClass;
import ru.practicum.StatDto;

@UtilityClass
public class StatMapper {

    public static StatDto toStatDto(Stat stat) {
        return StatDto.builder()
                .app(stat.getApp())
                .uri(stat.getUri())
                .hits(stat.getHits().intValue())
                .build();
    }
}
