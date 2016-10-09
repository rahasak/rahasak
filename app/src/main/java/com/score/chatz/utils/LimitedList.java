package com.score.chatz.utils;

import java.util.ArrayList;

/**
 * Created by eranga on 9/28/16.
 */
public class LimitedList<K> extends ArrayList<K> {

    private static final int DEFAULT_MAX_SIZE = 49;

    private int maxSize;

    public LimitedList(int size) {
        if (size < DEFAULT_MAX_SIZE) {
            this.maxSize = DEFAULT_MAX_SIZE;
        } else {
            this.maxSize = size;
        }
    }

    public boolean add(K k) {
        boolean r = super.add(k);
        if (size() > maxSize) {
            remove(0);
        }
        return r;
    }

    public K getYongest() {
        if (size() > 0)
            return get(size() - 1);
        else return null;
    }

    public K getOldest() {
        return get(0);
    }
}
