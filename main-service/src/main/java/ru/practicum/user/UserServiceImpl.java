package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.user.dto.NewUserRequestDto;
import ru.practicum.user.dto.UserDto;

import java.util.List;

import static ru.practicum.user.dto.UserMapper.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public List<UserDto> getUsers(List<Long> id, int from, int size) {
        log.info("Получение пользователей по id: id = " + id + ", from = " + from + ", size = " + size);
        return toUserDto(userRepository.findUserByIdIn(id, PageRequest.of(from / size, size)));
    }

    public List<UserDto> getUsers(int from, int size) {
        log.info("Получение всех пользователей: from = " + from + ", size = " + size);
        return toUserDto(userRepository.findAll(PageRequest.of(from / size, size)));
    }

    public UserDto createUser(NewUserRequestDto newUserRequestDto) {
        log.info("Создание нового пользователя: " + newUserRequestDto);
        return toUserDto(userRepository.save(toUser(newUserRequestDto)));
    }

    public void deleteUser(Long id) {
        log.info("Удаление пользователя с id = " + id);
        userRepository.deleteById(id);
    }
}
