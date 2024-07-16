package com.mywarehouse.mywarehouse.Interfaces;

import com.mywarehouse.mywarehouse.Models.Order;

@FunctionalInterface
public interface OrderCallback {
    void onOrderItemClicked(Order order);
}
