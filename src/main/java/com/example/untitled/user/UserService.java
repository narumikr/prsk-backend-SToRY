package com.example.untitled.user;

import com.example.untitled.common.dto.ErrorDetails;
import com.example.untitled.common.exception.DuplicationResourceException;
import com.example.untitled.common.exception.UnauthorizedException;
import com.example.untitled.user.dto.UserRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return userRepository.findByIsDeleted(false, pageable);
    }

    public User createUser(UserRequest reqDto) {
        userRepository.findByUserNameAndIsDeleted(reqDto.getUserName(), false)
                .ifPresent(user -> {
                    throw new DuplicationResourceException(
                            "Conflict detected",
                            List.of(new ErrorDetails(
                                    "userName",
                                    "User name already exist: " + reqDto.getUserName()
                            ))
                    );
                });

        User user = new User();
        user.setUserName(reqDto.getUserName());
        user.setPassword(reqDto.getPassword());

        return userRepository.save(user);
    }

    public User updateUser(Long id, UserRequest reqDto) {
        // IDで既存ユーザーを検索（削除されていないもの）
        User existingUser = userRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + id
                ));

        // パスワードが一致するか確認
        if (!existingUser.getPassword().equals(reqDto.getPassword())) {
            throw new UnauthorizedException(
                    "Authentication failed",
                    List.of(new ErrorDetails(
                            "password",
                            "Invalid password"
                    ))
            );
        }

        // 他のユーザーと同じuserNameに更新しようとしていないか確認
        userRepository.findByUserNameAndIsDeleted(reqDto.getUserName(), false)
                .ifPresent(user -> {
                    if (!user.getId().equals(id)) {
                        throw new DuplicationResourceException(
                                "Conflict detected",
                                List.of(new ErrorDetails(
                                        "userName",
                                        "User name already exist: " + reqDto.getUserName()
                                ))
                        );
                    }
                });

        // ユーザー情報を更新
        existingUser.setUserName(reqDto.getUserName());
        existingUser.setPassword(reqDto.getPassword());

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new EntityNotFoundException("User not found for id: " + id));

        user.setDeleted(true);
        userRepository.save(user);
    }
}
