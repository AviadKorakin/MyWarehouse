package com.mywarehouse.mywarehouse.Enums;

public enum LogType {
    ALL("All"), // New type added
    ITEM_CREATION("Item creation"),
    ITEM_MODIFICATION("Item modification"),
    ORDER_CREATION("Order creation"),
    ORDER_CANCELLATION("Order cancellation"),
    WAREHOUSE_CREATION("Warehouse creation"),
    OUT_OF_STOCK("Out of stock");

    private final String displayName;

    LogType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}