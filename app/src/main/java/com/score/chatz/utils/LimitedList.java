package com.score.chatz.utils;

import java.util.ArrayList;

/**
 * Created by eranga on 9/28/16.
 */
public class LimitedList<K> extends ArrayList<K> {

    private static final int DEFAULT_MAX_SIZE = 7;

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
            System.out.println("Exceed max");
            remove(0);
            //removeRange(0, size() - maxSize + 1);
        }
        return r;
    }

    public K getYongest() {
        return get(size() - 1);
    }

    public K getOldest() {
        return get(0);
    }
}
