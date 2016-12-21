package com.score.rahasak.exceptions;

/**
 * Created by Lakmal on 9/4/16.
 */
public class CameraBusyException extends Exception {
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "Camera is been used right now. Cannot launch a new instance.";
    }
}
