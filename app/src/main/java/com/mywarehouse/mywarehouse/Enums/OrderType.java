package com.mywarehouse.mywarehouse.Enums;

public enum OrderType {
    REGISTERED("Order has being registered in our system"),
    IN_PROGRESS("Order is in pickup process"),// New type added
    PICKED_UP("Order is picked up and waiting for delivery"),
    TRANSACTIONS_NEEDED("Collecting the items into the warehouse will take few business days."),
    COMPLETED("Order completed.");

    private final String displayName;

    OrderType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
