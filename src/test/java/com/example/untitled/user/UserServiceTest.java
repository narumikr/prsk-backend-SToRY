package com.example.untitled.user;

import com.example.untitled.common.exception.DuplicationResourceException;
import com.example.untitled.common.exception.UnauthorizedException;
import com.example.untitled.user.dto.UserRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    /**
     * createUser : 正常系 - ユーザーが正常に作成される
     */
    @Test
    public void createUser_Success() {
        UserRequest request = new UserRequest();
        request.setUserName("testuser");
        request.setPassword("testpassword");

        User createdUser = new User();
        createdUser.setId(1L);
        createdUser.setUserName("testuser");
        createdUser.setPassword("testpassword");

        when(userRepository.findByUserNameAndIsDeleted("testuser", false)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(createdUser);

        User result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUserName());
        assertEquals("testpassword", result.getPassword());

        verify(userRepository, times(1)).findByUserNameAndIsDeleted("testuser", false);
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * createUser : 異常系 - ユーザー名が重複しており、例外をスローする
     */
    @Test
    public void createUser_DuplicateUserName() {
        UserRequest request = new UserRequest();
        request.setUserName("testuser");
        request.setPassword("testpassword");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUserName("testuser");
        existingUser.setPassword("existingpassword");

        when(userRepository.findByUserNameAndIsDeleted("testuser", false)).thenReturn(Optional.of(existingUser));

        DuplicationResourceException exception = assertThrows(
                DuplicationResourceException.class,
                () -> userService.createUser(request)
        );

        assertNotNull(exception.getDetails());
        assertEquals(1, exception.getDetails().size());
        assertEquals("userName", exception.getDetails().get(0).getField());
        assertTrue(exception.getDetails().get(0).getMessage().contains("testuser"));

        verify(userRepository, times(1)).findByUserNameAndIsDeleted("testuser", false);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * getAllUsers : 正常系 - ユーザー一覧を取得（ASC順）
     */
    @Test
    public void getAllUsersSuccess_ASC() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUserName("User A");

        User user2 = new User();
        user2.setId(2L);
        user2.setUserName("User B");

        Page<User> userPage = new PageImpl<>(
                Arrays.asList(user1, user2),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "userName")),
                2
        );

        when(userRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(userPage);

        Page<User> result = userService.getAllUsers(0, 20, "userName", "ASC");

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("User A", result.getContent().get(0).getUserName());
        assertEquals("User B", result.getContent().get(1).getUserName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository, times(1)).findByIsDeleted(eq(false), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(20, capturedPageable.getPageSize());
        assertEquals(Sort.Direction.ASC, capturedPageable.getSort().getOrderFor("userName").getDirection());
    }

    /**
     * getAllUsers : 正常系 - ユーザー一覧を取得（DESC順）
     */
    @Test
    public void getAllUsersSuccess_DESC() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUserName("User B");

        User user2 = new User();
        user2.setId(2L);
        user2.setUserName("User A");

        Page<User> userPage = new PageImpl<>(
                Arrays.asList(user1, user2),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "userName")),
                2
        );

        when(userRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(userPage);

        Page<User> result = userService.getAllUsers(0, 20, "userName", "DESC");

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("User B", result.getContent().get(0).getUserName());
        assertEquals("User A", result.getContent().get(1).getUserName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository, times(1)).findByIsDeleted(eq(false), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(Sort.Direction.DESC, capturedPageable.getSort().getOrderFor("userName").getDirection());
    }

    /**
     * getAllUsers : 正常系 - 空のリストを返す
     */
    @Test
    public void getAllUsersSuccess_EmptyList() {
        Page<User> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "userName")),
                0
        );

        when(userRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(emptyPage);

        Page<User> result = userService.getAllUsers(0, 20, "userName", "ASC");

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(userRepository, times(1)).findByIsDeleted(eq(false), any(Pageable.class));
    }

    /**
     * updateUser : 正常系 - 新しいuserNameに変更
     */
    @Test
    public void updateUserSuccess_NewUserName() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUserName("originaluser");
        existingUser.setPassword("testpassword");

        UserRequest request = new UserRequest();
        request.setUserName("newuser");
        request.setPassword("testpassword");

        when(userRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUserNameAndIsDeleted("newuser", false)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(1L, request);

        assertNotNull(result);
        assertEquals("newuser", result.getUserName());

        verify(userRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(userRepository, times(1)).findByUserNameAndIsDeleted("newuser", false);
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * updateUser : 正常系 - 同じuserNameで更新（自分自身のIDが一致するため重複チェック通過）
     */
    @Test
    public void updateUserSuccess_SameUserName() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUserName("testuser");
        existingUser.setPassword("testpassword");

        UserRequest request = new UserRequest();
        request.setUserName("testuser");
        request.setPassword("testpassword");

        when(userRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUserNameAndIsDeleted("testuser", false)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(1L, request);

        assertNotNull(result);
        assertEquals("testuser", result.getUserName());

        verify(userRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(userRepository, times(1)).findByUserNameAndIsDeleted("testuser", false);
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * updateUser : 異常系 - ユーザーが見つからない
     */
    @Test
    public void updateUserError_NotFound() {
        UserRequest request = new UserRequest();
        request.setUserName("newuser");
        request.setPassword("testpassword");

        when(userRepository.findByIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.updateUser(999L, request)
        );

        assertEquals("User not found with id: 999", exception.getMessage());

        verify(userRepository, times(1)).findByIdAndIsDeleted(999L, false);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * updateUser : 異常系 - パスワードが一致しない
     */
    @Test
    public void updateUserError_WrongPassword() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUserName("testuser");
        existingUser.setPassword("correctpassword");

        UserRequest request = new UserRequest();
        request.setUserName("testuser");
        request.setPassword("wrongpassword");

        when(userRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingUser));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> userService.updateUser(1L, request)
        );

        assertNotNull(exception.getDetails());
        assertEquals("password", exception.getDetails().get(0).getField());

        verify(userRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * updateUser : 異常系 - userNameが他のユーザーと重複
     */
    @Test
    public void updateUserError_DuplicateUserName() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUserName("user1");
        existingUser.setPassword("testpassword");

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUserName("existinguser2");

        UserRequest request = new UserRequest();
        request.setUserName("existinguser2");
        request.setPassword("testpassword");

        when(userRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUserNameAndIsDeleted("existinguser2", false))
                .thenReturn(Optional.of(anotherUser));

        DuplicationResourceException exception = assertThrows(
                DuplicationResourceException.class,
                () -> userService.updateUser(1L, request)
        );

        assertNotNull(exception.getDetails());
        assertEquals("userName", exception.getDetails().get(0).getField());

        verify(userRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(userRepository, times(1)).findByUserNameAndIsDeleted("existinguser2", false);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * deleteUser : 正常系 - ユーザーを論理削除
     */
    @Test
    public void deleteUserSuccess() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUserName("testuser");
        existingUser.setDeleted(false);

        when(userRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteUser(1L);

        assertTrue(existingUser.isDeleted());

        verify(userRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(userRepository, times(1)).save(existingUser);
    }

    /**
     * deleteUser : 異常系 - ユーザーが見つからない
     */
    @Test
    public void deleteUserError_NotFound() {
        when(userRepository.findByIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.deleteUser(999L)
        );

        assertEquals("User not found for id: 999", exception.getMessage());

        verify(userRepository, times(1)).findByIdAndIsDeleted(999L, false);
        verify(userRepository, never()).save(any(User.class));
    }
}
