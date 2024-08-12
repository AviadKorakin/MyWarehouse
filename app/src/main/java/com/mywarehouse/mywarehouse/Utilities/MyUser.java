package com.mywarehouse.mywarehouse.Utilities;

import com.mywarehouse.mywarehouse.Models.User;

public class MyUser {

    private static MyUser instance;
    private User user;

    private MyUser() {
        // private constructor to prevent instantiation
    }

    public static MyUser getInstance() {
        if (instance == null) {
            instance = new MyUser();
        }
        return instance;
    }
    public void setUser(User user)
    {
        this.user=user;
    }

    public User getUser() {
        return user;
    }

    public static void setInstance(MyUser instance) {
        MyUser.instance = instance;
    }
}
