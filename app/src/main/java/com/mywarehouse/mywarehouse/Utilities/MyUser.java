package com.mywarehouse.mywarehouse.Utilities;

public class MyUser {

    private static MyUser instance;
    private String name;
    private String documentId;

    private MyUser() {
        // private constructor to prevent instantiation
    }

    public static MyUser getInstance() {
        if (instance == null) {
            instance = new MyUser();
        }
        return instance;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
