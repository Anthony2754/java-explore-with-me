package ru.practicum.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.CompilationMapper;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequestDto;
import ru.practicum.event.EventRepository;
import ru.practicum.exceptions.CompilationNotFoundException;
import ru.practicum.exceptions.RequestValidationException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.compilation.dto.CompilationMapper.toCompilation;
import static ru.practicum.compilation.dto.CompilationMapper.toCompilationDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {

        log.info("Получение подборки событий с параметрами: pinned = " + pinned + ", from = " + from + ", size = " + size);

        List<Compilation> compilations;

        if (pinned != null)
            compilations = compilationRepository.findByPinned(pinned, PageRequest.of(from / size, size));
        else
            compilations = compilationRepository.findAll(PageRequest.of(from / size, size)).getContent();

        return !compilations.isEmpty()
                ? compilations.stream().map(CompilationMapper::toCompilationDto).collect(Collectors.toList())
                : Collections.emptyList();
    }

    @Override
    public CompilationDto getCompilationById(Long id) {

        log.info("Получение подборки событий по id = " + id);

        return toCompilationDto(compilationRepository.findById(id)
                .orElseThrow(() -> new CompilationNotFoundException(id)));
    }

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {

        log.info("Добавление новой подборки: compilation= " + newCompilationDto);

        Compilation compilation = toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null) {
            compilation.setEvents(eventRepository.findByIdIn(newCompilationDto.getEvents()));
        }

        return toCompilationDto(compilationRepository.save(compilation));
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequestDto updateCompilationRequestDto) {
        log.info("Обновление подборки: comp_id = " + compId + ", update_compilation = " + updateCompilationRequestDto);
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new CompilationNotFoundException(compId));

        if (updateCompilationRequestDto.getTitle() != null) {
            String title = updateCompilationRequestDto.getTitle();
            if (title.isEmpty() || title.length() > 50)
                throw new RequestValidationException("Название подборки должно содержать от 1 до 50 символов");
            compilation.setTitle(updateCompilationRequestDto.getTitle());
        }

        if (updateCompilationRequestDto.getPinned() != null)
            compilation.setPinned(updateCompilationRequestDto.getPinned());

        if (updateCompilationRequestDto.getEvents() != null)
            compilation.setEvents(eventRepository.findByIdIn(updateCompilationRequestDto.getEvents()));

        return toCompilationDto(compilationRepository.save(compilation));
    }

    @Override
    public void deleteCompilation(Long compId) {

        log.info("Удаление подборки: comp_id = " + compId);

        compilationRepository.findById(compId).orElseThrow(() -> new CompilationNotFoundException(compId));
        compilationRepository.deleteById(compId);
    }
}
