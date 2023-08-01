package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user.dto.NewUserRequestDto;
import ru.practicum.user.dto.UserDto;

import java.util.List;

import static ru.practicum.user.dto.UserMapper.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Transactional
    @Override
    public List<UserDto> getUsers(List<Long> id, int from, int size) {
        log.info("Получение пользователей по id: id = {}, from = {}, size = {}", id, from, size);
        return toUserDto(userRepository.findUserByIdIn(id, PageRequest.of(from / size, size)));
    }

    @Transactional
    @Override
    public List<UserDto> getUsers(int from, int size) {
        log.info("Получение всех пользователей: from = {}, size = {}", from, size);
        return toUserDto(userRepository.findAll(PageRequest.of(from / size, size)));
    }

    @Transactional
    @Override
    public UserDto createUser(NewUserRequestDto newUserRequestDto) {
        log.info("Создание нового пользователя: " + newUserRequestDto);
        return toUserDto(userRepository.save(toUser(newUserRequestDto)));
    }

    @Transactional
    @Override
    public void deleteUser(Long id) {
        log.info("Удаление пользователя с id = " + id);
        userRepository.deleteById(id);
    }
}
