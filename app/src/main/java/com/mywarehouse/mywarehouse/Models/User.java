package com.mywarehouse.mywarehouse.Models;

import java.util.List;

public class User {
    String userId;
    String name;
    String email;
    String birthday;
    String phone;
    String role;
    List<String> orders;
    List<String> pickups;

    public User() {
    }

    public User(String userId, String name, String email, String birthday, String phone, String role, List<String> orders, List<String> pickups) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.birthday = birthday;
        this.phone = phone;
        this.role = role;
        this.orders = orders;
        this.pickups = pickups;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getOrders() {
        return orders;
    }

    public void setOrders(List<String> orders) {
        this.orders = orders;
    }

    public List<String> getPickups() {
        return pickups;
    }

    public void setPickups(List<String> pickups) {
        this.pickups = pickups;
    }
}
