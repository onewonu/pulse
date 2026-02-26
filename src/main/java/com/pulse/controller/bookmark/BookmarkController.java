package com.pulse.controller.bookmark;

import com.pulse.dto.bookmark.BookmarkCreateRequest;
import com.pulse.dto.bookmark.BookmarkReorderRequest;
import com.pulse.dto.bookmark.BookmarkResponse;
import com.pulse.dto.bookmark.BookmarkUpdateRequest;
import com.pulse.service.bookmark.BookmarkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping
    public ResponseEntity<BookmarkResponse> createBookmark(@Valid @RequestBody BookmarkCreateRequest request) {
        Long userId = getCurrentUserId();
        BookmarkResponse response = bookmarkService.createBookmark(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BookmarkResponse> updateBookmark(
            @PathVariable Long id,
            @Valid @RequestBody BookmarkUpdateRequest request
    ) {
        Long userId = getCurrentUserId();
        BookmarkResponse response = bookmarkService.updateBookmark(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/reorder")
    public ResponseEntity<String> reorderBookmarks(@Valid @RequestBody BookmarkReorderRequest request) {
        Long userId = getCurrentUserId();
        bookmarkService.reorderBookmarks(userId, request);
        return ResponseEntity.ok("Bookmarks reordered successfully");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteAllBookmarks() {
        Long userId = getCurrentUserId();
        bookmarkService.deleteAllBookmarks(userId);
        return ResponseEntity.ok("All bookmarks deleted successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBookmark(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        bookmarkService.deleteBookmark(userId, id);
        return ResponseEntity.ok("Bookmark deleted successfully");
    }

    @GetMapping
    public ResponseEntity<List<BookmarkResponse>> getBookmarks() {
        Long userId = getCurrentUserId();
        List<BookmarkResponse> responses = bookmarkService.getBookmarks(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookmarkResponse> getBookmark(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        BookmarkResponse response = bookmarkService.getBookmark(userId, id);
        return ResponseEntity.ok(response);
    }

    private static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
