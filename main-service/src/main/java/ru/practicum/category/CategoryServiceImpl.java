package ru.practicum.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryMapper;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.exceptions.CategoryNotFoundException;
import ru.practicum.exceptions.ForbiddenException;

import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.category.dto.CategoryMapper.toCategory;
import static ru.practicum.category.dto.CategoryMapper.toCategoryDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    public List<CategoryDto> getCategories(int from, int size) {

        log.info("Получение списка категорий: from = " + from + ", size = " + size);

        return categoryRepository.findAll(PageRequest.of(from / size, size))
                .stream()
                .map(CategoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(long catId) {

        log.info("Получение категории по id: cat_id = " + catId);

        return toCategoryDto(categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId)));
    }

    @Override
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {

        log.info("Добавление новой категории: " + newCategoryDto);

        return toCategoryDto(categoryRepository.save(toCategory(newCategoryDto)));
    }

    @Override
    public CategoryDto updateCategory(long catId, NewCategoryDto newCategoryDto) {

        log.info("Изменение категории: cat_id = " + catId + ", название категории = " + newCategoryDto);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId));
        Category updatedCategory = toCategory(newCategoryDto);
        updatedCategory.setId(category.getId());
        return toCategoryDto(categoryRepository.save(updatedCategory));
    }

    @Override
    public void deleteCategory(long catId) {

        log.info("Удаление категории: cat_id = " + catId);

        categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId));
        Event event = eventRepository.findFirstByCategoryId(catId);

        if (event != null)
            throw new ForbiddenException("Категория не удалилась");
        categoryRepository.deleteById(catId);
    }

}
