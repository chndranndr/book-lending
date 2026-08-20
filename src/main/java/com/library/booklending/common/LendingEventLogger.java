package com.library.booklending.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LendingEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(LendingEventLogger.class);

    public void loanBorrowed(long memberId, long bookId, long loanId) {
        LOGGER.info("loan borrowed memberId={} bookId={} loanId={}", memberId, bookId, loanId);
    }

    public void loanReturned(long loanId, long bookId) {
        LOGGER.info("loan returned loanId={} bookId={}", loanId, bookId);
    }

    public void loanRejected(long memberId, long bookId, BusinessErrorCode reason) {
        LOGGER.info("loan rejected memberId={} bookId={} reason={}", memberId, bookId, reason);
    }
}
