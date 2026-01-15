package com.pulse.service.bookmark;

import com.pulse.dto.bookmark.BookmarkCreateRequest;
import com.pulse.dto.bookmark.BookmarkReorderRequest;
import com.pulse.dto.bookmark.BookmarkResponse;
import com.pulse.dto.bookmark.BookmarkUpdateRequest;
import com.pulse.entity.bookmark.Bookmark;
import com.pulse.entity.user.User;
import com.pulse.exception.bookmark.BookmarkAccessDeniedException;
import com.pulse.exception.bookmark.BookmarkNotFoundException;
import com.pulse.exception.user.UserNotFoundException;
import com.pulse.repository.bookmark.BookmarkRepository;
import com.pulse.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkService.class);

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, UserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookmarkResponse createBookmark(Long userId, BookmarkCreateRequest request) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found: userId={}", userId);
            throw new UserNotFoundException("User not found");
        }

        Integer maxDisplayOrder = bookmarkRepository.findMaxDisplayOrderByUserId(userId);
        Integer newDisplayOrder = maxDisplayOrder + 1;

        Bookmark bookmark = Bookmark.of(
            request.getName(),
            request.getDepartureStationId(),
            request.getArrivalStationId(),
            newDisplayOrder,
            user
        );

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return BookmarkResponse.of(savedBookmark);
    }

    @Transactional
    public BookmarkResponse updateBookmark(Long userId, Long bookmarkId, BookmarkUpdateRequest request) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId).orElse(null);
        if (bookmark == null) {
            throw new BookmarkNotFoundException("Bookmark not found with id: " + bookmarkId);
        }

        if (!bookmark.isOwnedBy(userId)) {
            throw new BookmarkAccessDeniedException("You do not have permission to access this bookmark");
        }

        if (request.getName() == null && request.getDepartureStationId() == null && request.getArrivalStationId() == null) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        bookmark.update(
            request.getName(),
            request.getDepartureStationId(),
            request.getArrivalStationId()
        );

        return BookmarkResponse.of(bookmark);
    }

    @Transactional
    public void reorderBookmarks(Long userId, BookmarkReorderRequest request) {
        List<Long> bookmarkIds = new ArrayList<>();
        for (BookmarkReorderRequest.ReorderItem item : request.getItems()) {
            bookmarkIds.add(item.getBookmarkId());
        }

        List<Bookmark> bookmarks = bookmarkRepository.findByIdsAndUserId(bookmarkIds, userId);

        if (bookmarks.size() != bookmarkIds.size()) {
            throw new BookmarkNotFoundException("Some bookmarks not found or access denied");
        }

        for (BookmarkReorderRequest.ReorderItem item : request.getItems()) {
            for (Bookmark bookmark : bookmarks) {
                if (bookmark.getId().equals(item.getBookmarkId())) {
                    bookmark.updateDisplayOrder(item.getNewDisplayOrder());
                    break;
                }
            }
        }
    }

    @Transactional
    public void deleteBookmark(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId).orElse(null);
        if (bookmark == null) {
            throw new BookmarkNotFoundException("Bookmark not found with id: " + bookmarkId);
        }

        if (!bookmark.isOwnedBy(userId)) {
            throw new BookmarkAccessDeniedException("You do not have permission to access this bookmark");
        }

        bookmarkRepository.delete(bookmark);
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarks(Long userId) {
        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        List<BookmarkResponse> responses = new ArrayList<>();
        for (Bookmark bookmark : bookmarks) {
            responses.add(BookmarkResponse.of(bookmark));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public BookmarkResponse getBookmark(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId).orElse(null);
        if (bookmark == null) {
            throw new BookmarkNotFoundException("Bookmark not found with id: " + bookmarkId);
        }

        if (!bookmark.isOwnedBy(userId)) {
            throw new BookmarkAccessDeniedException("You do not have permission to access this bookmark");
        }

        return BookmarkResponse.of(bookmark);
    }

}
