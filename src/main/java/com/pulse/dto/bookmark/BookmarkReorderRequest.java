package com.pulse.dto.bookmark;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BookmarkReorderRequest(
    @NotEmpty(message = "Reorder items are required")
    @Valid
    List<ReorderItem> items
) {
    public record ReorderItem(
        @NotNull(message = "Bookmark ID is required")
        Long bookmarkId,

        @NotNull(message = "New display order is required")
        @Min(value = 0, message = "Display order must be greater than or equal to 0")
        Integer newDisplayOrder
    ) {}
}
