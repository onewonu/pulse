package com.pulse.exception.bookmark;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class BookmarkNotFoundException extends BaseException {

    public BookmarkNotFoundException(String message) {
        super(ErrorCode.BOOKMARK_NOT_FOUND, message);
    }
}
