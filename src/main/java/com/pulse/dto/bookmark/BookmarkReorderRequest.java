package com.pulse.dto.bookmark;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BookmarkReorderRequest {

    @NotEmpty(message = "Reorder items are required")
    @Valid
    private List<ReorderItem> items;

    public static class ReorderItem {
        @NotNull(message = "Bookmark ID is required")
        private Long bookmarkId;

        @NotNull(message = "New display order is required")
        @Min(value = 0, message = "Display order must be greater than or equal to 0")
        private Integer newDisplayOrder;

        public Long getBookmarkId() {
            return bookmarkId;
        }

        public Integer getNewDisplayOrder() {
            return newDisplayOrder;
        }

        public void setBookmarkId(Long bookmarkId) {
            this.bookmarkId = bookmarkId;
        }

        public void setNewDisplayOrder(Integer newDisplayOrder) {
            this.newDisplayOrder = newDisplayOrder;
        }
    }

    public List<ReorderItem> getItems() {
        return items;
    }

    public void setItems(List<ReorderItem> items) {
        this.items = items;
    }
}
