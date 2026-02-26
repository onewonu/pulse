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
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkService.class);

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final SubwayStationRepository subwayStationRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, UserRepository userRepository, SubwayStationRepository subwayStationRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
        this.subwayStationRepository = subwayStationRepository;
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
            request.name(),
            request.departureStationId(),
            request.arrivalStationId(),
            request.startTime(),
            request.endTime(),
            newDisplayOrder,
            user
        );

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        StationInfo departureInfo = getStationInfo(savedBookmark.getDepartureStationId());
        StationInfo arrivalInfo = getStationInfo(savedBookmark.getArrivalStationId());
        return BookmarkResponse.of(
                savedBookmark,
                departureInfo != null ? departureInfo.stationName() : null,
                arrivalInfo != null ? arrivalInfo.stationName() : null,
                departureInfo != null ? departureInfo.lineName() : null,
                departureInfo != null ? departureInfo.lineColor() : null,
                arrivalInfo != null ? arrivalInfo.lineName() : null,
                arrivalInfo != null ? arrivalInfo.lineColor() : null
        );
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

        if (request.name() == null && request.departureStationId() == null && request.arrivalStationId() == null && request.startTime() == null && request.endTime() == null) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        bookmark.update(
            request.name(),
            request.departureStationId(),
            request.arrivalStationId(),
            request.startTime(),
            request.endTime()
        );

        StationInfo departureInfo = getStationInfo(bookmark.getDepartureStationId());
        StationInfo arrivalInfo = getStationInfo(bookmark.getArrivalStationId());
        return BookmarkResponse.of(
                bookmark,
                departureInfo != null ? departureInfo.stationName() : null,
                arrivalInfo != null ? arrivalInfo.stationName() : null,
                departureInfo != null ? departureInfo.lineName() : null,
                departureInfo != null ? departureInfo.lineColor() : null,
                arrivalInfo != null ? arrivalInfo.lineName() : null,
                arrivalInfo != null ? arrivalInfo.lineColor() : null
        );
    }

    @Transactional
    public void reorderBookmarks(Long userId, BookmarkReorderRequest request) {
        List<Long> bookmarkIds = request.items().stream()
                .map(BookmarkReorderRequest.ReorderItem::bookmarkId)
                .toList();

        List<Bookmark> allBookmarks = bookmarkRepository.findAllById(bookmarkIds);

        if (allBookmarks.size() != bookmarkIds.size()) {
            throw new BookmarkNotFoundException("Some bookmarks not found");
        }

        boolean allOwned = allBookmarks.stream().allMatch(bookmark -> bookmark.isOwnedBy(userId));
        if (!allOwned) {
            throw new BookmarkAccessDeniedException("You do not have permission to access this bookmark");
        }

        Map<Long, Bookmark> bookmarkMap = allBookmarks.stream()
                .collect(Collectors.toMap(Bookmark::getId, Function.identity()));

        request.items().forEach(item -> {
            Bookmark bookmark = bookmarkMap.get(item.bookmarkId());
            if (bookmark != null) {
                bookmark.updateDisplayOrder(item.newDisplayOrder());
            }
        });
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

    @Transactional
    public void deleteAllBookmarks(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found: userId={}", userId);
            throw new UserNotFoundException("User not found");
        }

        bookmarkRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarks(Long userId) {
        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        return bookmarks.stream()
                .map(bookmark -> {
                    StationInfo departureInfo = getStationInfo(bookmark.getDepartureStationId());
                    StationInfo arrivalInfo = getStationInfo(bookmark.getArrivalStationId());
                    return BookmarkResponse.of(
                            bookmark,
                            departureInfo != null ? departureInfo.stationName() : null,
                            arrivalInfo != null ? arrivalInfo.stationName() : null,
                            departureInfo != null ? departureInfo.lineName() : null,
                            departureInfo != null ? departureInfo.lineColor() : null,
                            arrivalInfo != null ? arrivalInfo.lineName() : null,
                            arrivalInfo != null ? arrivalInfo.lineColor() : null
                    );
                })
                .toList();
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

        StationInfo departureInfo = getStationInfo(bookmark.getDepartureStationId());
        StationInfo arrivalInfo = getStationInfo(bookmark.getArrivalStationId());

        return BookmarkResponse.of(
                bookmark,
                departureInfo != null ? departureInfo.stationName() : null,
                arrivalInfo != null ? arrivalInfo.stationName() : null,
                departureInfo != null ? departureInfo.lineName() : null,
                departureInfo != null ? departureInfo.lineColor() : null,
                arrivalInfo != null ? arrivalInfo.lineName() : null,
                arrivalInfo != null ? arrivalInfo.lineColor() : null
        );
    }

    private StationInfo getStationInfo(Integer stationId) {
        if (stationId == null) {
            return null;
        }

        return subwayStationRepository.findById(String.valueOf(stationId))
                .map(station -> new StationInfo(
                        station.getStationName(),
                        station.getSubwayLine().getLineName(),
                        station.getSubwayLine().getColor()
                ))
                .orElse(null);
    }

    private record StationInfo(
            String stationName,
            String lineName,
            String lineColor
    ) {}

}
