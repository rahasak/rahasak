package com.score.rahasak.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SenzBuffer {

    private final List<Byte> buffer;

    public SenzBuffer() {
        buffer = Collections.synchronizedList(new LinkedList<Byte>());
    }

    public void put(byte[] data) {
        for (byte i : data)
            buffer.add(i);
    }

    public byte[] getAll() {
        byte[] data = new byte[buffer.size()];
        int index = 0;
        for (int i = 0; i < buffer.size(); i++) {
            // add to data
            data[index] = buffer.get(i);
            index++;
        }

        return data;
    }

    public byte[] get(int from, int to) {
        byte[] data = new byte[to - from];
        int index = 0;
        for (int i = from; i < to; i++) {
            // add to data
            data[index] = buffer.get(from);
            index++;

            // remove element
            buffer.remove(from);
        }

        return data;
    }

    public void delete(int from, int to) {
        for (int i = to - 1; i >= from; i--) {
            buffer.remove(i);
        }
    }

    public int size() {
        return buffer.size();
    }

    public static void main(String args[]) {
        SenzBuffer buffer = new SenzBuffer();
        String e = "eranga";
        String r = "ranga";
        byte[] e1 = e.getBytes();
        byte[] b1 = r.getBytes();

        buffer.put(e1);
        buffer.print(e1);
        buffer.print(buffer.getAll());

        buffer.put(b1);
        buffer.print(b1);
        buffer.print(buffer.getAll());

        byte[] a = buffer.get(0, 8);
        buffer.print(a);
        //buffer.delete(0, 8);
        buffer.print(buffer.getAll());

    }

    private void print(byte[] buf) {
        for (byte b : buf) {
            System.out.print(b);
            System.out.print(", ");
        }

        System.out.println();
    }
}
