package com.cbtunnel.plus.utils;

public class Model {
    private String mName;
    private int mPostition;

    public Model(String mName, int mPostition) {
        this.mName = mName;
        this.mPostition = mPostition;
    }

    public String getName() {
        return this.mName;
    }


    public int getPostition() {
        return this.mPostition;
    }
}