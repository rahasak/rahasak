package com.score.rahasak;


import android.util.Log;

public class Rahas {
    public native int addz(int a);

    public void add(int a) {
            Log.d("TAG", "not addzzzz --- " + addz(a));
    }

    static {
        System.loadLibrary("raha");
    }
}
