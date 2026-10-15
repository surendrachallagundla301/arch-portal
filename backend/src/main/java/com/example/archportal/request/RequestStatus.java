package com.example.archportal.request;

public enum RequestStatus {
    SUBMITTED,
    APPROVED,
    NEEDS_CHANGES,
    REJECTED;

    /** Approved and rejected requests are closed; no further decisions can be recorded. */
    public boolean isClosed() {
        return this == APPROVED || this == REJECTED;
    }
}
