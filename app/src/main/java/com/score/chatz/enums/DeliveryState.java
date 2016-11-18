package com.score.chatz.enums;

/**
 * Created by eranga on 11/18/16.
 */
public enum DeliveryState {
    DESPATCHED,
    DELIVERED,
    PENDING;

    public int getValue(DeliveryState type) {
        return type.ordinal();
    }

    public DeliveryState getType(int value) {
        return DeliveryState.valueOf(Integer.toString(value));
    }
}
