package com.tse.core_application.model;

public enum StatType {
    DELAYED(0),
    WATCHLIST(1),
    ONTRACK(2),
    NOTSTARTED(3),
    LATE_COMPLETION(4),
    COMPLETED(5);

    private final int order;

    StatType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
