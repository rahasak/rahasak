package com.score.chatz.pojo;

/**
 * Created by Lakmal on 8/29/16.
 */
public class BitmapTaskParams {
    String data;
    int width;
    int height;

    public BitmapTaskParams(String data, int width, int height) {
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public String getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}