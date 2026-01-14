package com.pulse.exception.bookmark;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class BookmarkAccessDeniedException extends BaseException {

    public BookmarkAccessDeniedException(String message) {
        super(ErrorCode.BOOKMARK_ACCESS_DENIED, message);
    }
}
