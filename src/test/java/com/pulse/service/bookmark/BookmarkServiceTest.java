package com.pulse.service.bookmark;

import com.pulse.dto.bookmark.BookmarkCreateRequest;
import com.pulse.dto.bookmark.BookmarkReorderRequest;
import com.pulse.dto.bookmark.BookmarkResponse;
import com.pulse.dto.bookmark.BookmarkUpdateRequest;
import com.pulse.entity.bookmark.Bookmark;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.entity.user.ProviderType;
import com.pulse.entity.user.User;
import com.pulse.exception.bookmark.BookmarkAccessDeniedException;
import com.pulse.exception.bookmark.BookmarkNotFoundException;
import com.pulse.exception.user.UserNotFoundException;
import com.pulse.repository.bookmark.BookmarkRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkService")
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubwayStationRepository subwayStationRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    private void setUserId(User user, Long id) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setBookmarkId(Bookmark bookmark, Long id) {
        try {
            Field idField = Bookmark.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(bookmark, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("북마크 생성 성공")
    void createBookmark_Success() {
        // Given
        Long userId = 1L;
        BookmarkCreateRequest request = new BookmarkCreateRequest("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0));

        User user = User.of("test-user", ProviderType.KAKAO, "kakao123");
        Bookmark bookmark = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, user);

        SubwayLine line2 = SubwayLine.of("2호선", "#00A84D");
        SubwayLine line7 = SubwayLine.of("7호선", "#747F00");
        SubwayStation station1000 = SubwayStation.of("1000", "강남역", line2, 37.498095, 127.027610);
        SubwayStation station2000 = SubwayStation.of("2000", "홍대입구역", line7, 37.557192, 126.925381);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookmarkRepository.findMaxDisplayOrderByUserId(userId)).thenReturn(0);
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(bookmark);
        when(subwayStationRepository.findById("1000")).thenReturn(Optional.of(station1000));
        when(subwayStationRepository.findById("2000")).thenReturn(Optional.of(station2000));

        // When
        BookmarkResponse response = bookmarkService.createBookmark(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("집-회사");
        assertThat(response.departureStationId()).isEqualTo("1000");
        assertThat(response.departureStationName()).isEqualTo("강남역");
        assertThat(response.arrivalStationId()).isEqualTo("2000");
        assertThat(response.arrivalStationName()).isEqualTo("홍대입구역");

        verify(userRepository, times(1)).findById(userId);
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 생성 실패 - 사용자 없음")
    void createBookmark_UserNotFound() {
        // Given
        Long userId = 999L;
        BookmarkCreateRequest request = new BookmarkCreateRequest("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookmarkService.createBookmark(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("북마크 목록 조회 성공")
    void getBookmarks_Success() {
        // Given
        Long userId = 1L;

        User user = User.of("test-user", ProviderType.KAKAO, "kakao123");
        Bookmark bookmark1 = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, user);
        Bookmark bookmark2 = Bookmark.of("회사-집", "2000", "1000", LocalTime.of(18, 0), LocalTime.of(21, 0), 2, user);

        SubwayLine line2 = SubwayLine.of("2호선", "#00A84D");
        SubwayLine line7 = SubwayLine.of("7호선", "#747F00");
        SubwayStation station1000 = SubwayStation.of("1000", "강남역", line2, 37.498095, 127.027610);
        SubwayStation station2000 = SubwayStation.of("2000", "홍대입구역", line7, 37.557192, 126.925381);

        when(bookmarkRepository.findByUserIdOrderByDisplayOrderAsc(userId))
                .thenReturn(List.of(bookmark1, bookmark2));
        when(subwayStationRepository.findById("1000")).thenReturn(Optional.of(station1000));
        when(subwayStationRepository.findById("2000")).thenReturn(Optional.of(station2000));

        // When
        List<BookmarkResponse> responses = bookmarkService.getBookmarks(userId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().name()).isEqualTo("집-회사");
        assertThat(responses.get(0).departureStationName()).isEqualTo("강남역");
        assertThat(responses.get(0).arrivalStationName()).isEqualTo("홍대입구역");
        assertThat(responses.get(1).name()).isEqualTo("회사-집");
        assertThat(responses.get(1).departureStationName()).isEqualTo("홍대입구역");
        assertThat(responses.get(1).arrivalStationName()).isEqualTo("강남역");

        verify(bookmarkRepository, times(1)).findByUserIdOrderByDisplayOrderAsc(userId);
    }

    @Test
    @DisplayName("북마크 수정 성공")
    void updateBookmark_Success() {
        // Given
        Long userId = 1L;
        Long bookmarkId = 10L;
        BookmarkUpdateRequest request = new BookmarkUpdateRequest("새이름", null, null, null, null);

        User user = User.of("test-user", ProviderType.KAKAO, "kakao123");
        setUserId(user, userId);
        Bookmark bookmark = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, user);

        SubwayLine line2 = SubwayLine.of("2호선", "#00A84D");
        SubwayLine line7 = SubwayLine.of("7호선", "#747F00");
        SubwayStation station1000 = SubwayStation.of("1000", "강남역", line2, 37.498095, 127.027610);
        SubwayStation station2000 = SubwayStation.of("2000", "홍대입구역", line7, 37.557192, 126.925381);

        when(bookmarkRepository.findById(bookmarkId)).thenReturn(Optional.of(bookmark));
        when(subwayStationRepository.findById("1000")).thenReturn(Optional.of(station1000));
        when(subwayStationRepository.findById("2000")).thenReturn(Optional.of(station2000));

        // When
        BookmarkResponse response = bookmarkService.updateBookmark(userId, bookmarkId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.departureStationName()).isEqualTo("강남역");
        assertThat(response.arrivalStationName()).isEqualTo("홍대입구역");

        verify(bookmarkRepository, times(1)).findById(bookmarkId);
    }

    @Test
    @DisplayName("북마크 수정 실패 - 권한 없음")
    void updateBookmark_AccessDenied() {
        // Given
        Long userId = 1L;
        Long bookmarkId = 10L;
        BookmarkUpdateRequest request = new BookmarkUpdateRequest("새이름", null, null, null, null);

        User otherUser = User.of("other-user", ProviderType.KAKAO, "other123");
        setUserId(otherUser, 999L);
        Bookmark bookmark = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, otherUser);

        when(bookmarkRepository.findById(bookmarkId)).thenReturn(Optional.of(bookmark));

        // When & Then
        assertThatThrownBy(() -> bookmarkService.updateBookmark(userId, bookmarkId, request))
                .isInstanceOf(BookmarkAccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("북마크 삭제 성공")
    void deleteBookmark_Success() {
        // Given
        Long userId = 1L;
        Long bookmarkId = 10L;

        User user = User.of("test-user", ProviderType.KAKAO, "kakao123");
        setUserId(user, userId);
        Bookmark bookmark = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, user);

        when(bookmarkRepository.findById(bookmarkId)).thenReturn(Optional.of(bookmark));

        // When
        bookmarkService.deleteBookmark(userId, bookmarkId);

        // Then
        verify(bookmarkRepository, times(1)).findById(bookmarkId);
        verify(bookmarkRepository, times(1)).delete(bookmark);
    }

    @Test
    @DisplayName("북마크 삭제 실패 - 북마크 없음")
    void deleteBookmark_NotFound() {
        // Given
        Long userId = 1L;
        Long bookmarkId = 999L;

        when(bookmarkRepository.findById(bookmarkId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookmarkService.deleteBookmark(userId, bookmarkId))
                .isInstanceOf(BookmarkNotFoundException.class)
                .hasMessageContaining("Bookmark not found");

        verify(bookmarkRepository, never()).delete(any());
    }

    @Test
    @DisplayName("북마크 삭제 실패 - 권한 없음")
    void deleteBookmark_AccessDenied() {
        // Given
        Long userId = 1L;
        Long bookmarkId = 10L;

        User otherUser = User.of("other-user", ProviderType.KAKAO, "other123");
        setUserId(otherUser, 999L);
        Bookmark bookmark = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, otherUser);

        when(bookmarkRepository.findById(bookmarkId)).thenReturn(Optional.of(bookmark));

        // When & Then
        assertThatThrownBy(() -> bookmarkService.deleteBookmark(userId, bookmarkId))
                .isInstanceOf(BookmarkAccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(bookmarkRepository, never()).delete(any());
    }

    @Test
    @DisplayName("북마크 순서 변경 성공")
    void reorderBookmarks_Success() {
        // Given
        Long userId = 1L;

        User user = User.of("test-user", ProviderType.KAKAO, "kakao123");
        setUserId(user, userId);

        Bookmark bookmark1 = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, user);
        setBookmarkId(bookmark1, 1L);

        Bookmark bookmark2 = Bookmark.of("회사-집", "2000", "1000", LocalTime.of(18, 0), LocalTime.of(21, 0), 2, user);
        setBookmarkId(bookmark2, 2L);

        BookmarkReorderRequest.ReorderItem item1 = new BookmarkReorderRequest.ReorderItem(1L, 2);
        BookmarkReorderRequest.ReorderItem item2 = new BookmarkReorderRequest.ReorderItem(2L, 1);

        BookmarkReorderRequest request = new BookmarkReorderRequest(List.of(item1, item2));

        when(bookmarkRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(bookmark1, bookmark2));

        // When
        bookmarkService.reorderBookmarks(userId, request);

        // Then
        verify(bookmarkRepository, times(1)).findAllById(any());
    }

    @Test
    @DisplayName("북마크 순서 변경 실패 - 북마크 없음")
    void reorderBookmarks_NotFound() {
        // Given
        Long userId = 1L;

        BookmarkReorderRequest.ReorderItem item = new BookmarkReorderRequest.ReorderItem(999L, 1);
        BookmarkReorderRequest request = new BookmarkReorderRequest(List.of(item));

        when(bookmarkRepository.findAllById(List.of(999L))).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> bookmarkService.reorderBookmarks(userId, request))
                .isInstanceOf(BookmarkNotFoundException.class)
                .hasMessageContaining("Some bookmarks not found");
    }

    @Test
    @DisplayName("북마크 순서 변경 실패 - 권한 없음")
    void reorderBookmarks_AccessDenied() {
        // Given
        Long userId = 1L;

        User otherUser = User.of("other-user", ProviderType.KAKAO, "other123");
        setUserId(otherUser, 999L);
        Bookmark bookmark = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, otherUser);
        setBookmarkId(bookmark, 1L);

        BookmarkReorderRequest.ReorderItem item = new BookmarkReorderRequest.ReorderItem(1L, 2);
        BookmarkReorderRequest request = new BookmarkReorderRequest(List.of(item));

        when(bookmarkRepository.findAllById(List.of(1L))).thenReturn(List.of(bookmark));

        // When & Then
        assertThatThrownBy(() -> bookmarkService.reorderBookmarks(userId, request))
                .isInstanceOf(BookmarkAccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("북마크 단건 조회 성공")
    void getBookmark_Success() {
        // Given
        Long userId = 1L;
        Long bookmarkId = 10L;

        User user = User.of("test-user", ProviderType.KAKAO, "kakao123");
        setUserId(user, userId);
        Bookmark bookmark = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, user);

        SubwayLine line2 = SubwayLine.of("2호선", "#00A84D");
        SubwayLine line7 = SubwayLine.of("7호선", "#747F00");
        SubwayStation station1000 = SubwayStation.of("1000", "강남역", line2, 37.498095, 127.027610);
        SubwayStation station2000 = SubwayStation.of("2000", "홍대입구역", line7, 37.557192, 126.925381);

        when(bookmarkRepository.findById(bookmarkId)).thenReturn(Optional.of(bookmark));
        when(subwayStationRepository.findById("1000")).thenReturn(Optional.of(station1000));
        when(subwayStationRepository.findById("2000")).thenReturn(Optional.of(station2000));

        // When
        BookmarkResponse response = bookmarkService.getBookmark(userId, bookmarkId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("집-회사");
        assertThat(response.departureStationId()).isEqualTo("1000");
        assertThat(response.departureStationName()).isEqualTo("강남역");
        assertThat(response.arrivalStationId()).isEqualTo("2000");
        assertThat(response.arrivalStationName()).isEqualTo("홍대입구역");

        verify(bookmarkRepository, times(1)).findById(bookmarkId);
    }

    @Test
    @DisplayName("북마크 단건 조회 실패 - 북마크 없음")
    void getBookmark_NotFound() {
        // Given
        Long userId = 1L;
        Long bookmarkId = 999L;

        when(bookmarkRepository.findById(bookmarkId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookmarkService.getBookmark(userId, bookmarkId))
                .isInstanceOf(BookmarkNotFoundException.class)
                .hasMessageContaining("Bookmark not found");
    }

    @Test
    @DisplayName("북마크 단건 조회 실패 - 권한 없음")
    void getBookmark_AccessDenied() {
        // Given
        Long userId = 1L;
        Long bookmarkId = 10L;

        User otherUser = User.of("other-user", ProviderType.KAKAO, "other123");
        setUserId(otherUser, 999L);
        Bookmark bookmark = Bookmark.of("집-회사", "1000", "2000", LocalTime.of(9, 0), LocalTime.of(18, 0), 1, otherUser);

        when(bookmarkRepository.findById(bookmarkId)).thenReturn(Optional.of(bookmark));

        // When & Then
        assertThatThrownBy(() -> bookmarkService.getBookmark(userId, bookmarkId))
                .isInstanceOf(BookmarkAccessDeniedException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("북마크 전체 삭제 성공")
    void deleteAllBookmarks_Success() {
        // Given
        Long userId = 1L;

        User user = User.of("test-user", ProviderType.KAKAO, "kakao123");
        setUserId(user, userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(bookmarkRepository).deleteByUserId(userId);

        // When
        bookmarkService.deleteAllBookmarks(userId);

        // Then
        verify(userRepository, times(1)).findById(userId);
        verify(bookmarkRepository, times(1)).deleteByUserId(userId);
    }

    @Test
    @DisplayName("북마크 전체 삭제 실패 - 사용자 없음")
    void deleteAllBookmarks_UserNotFound() {
        // Given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookmarkService.deleteAllBookmarks(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(bookmarkRepository, never()).deleteByUserId(any());
    }
}
