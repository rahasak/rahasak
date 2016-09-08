package com.score.chatz.exceptions;

/**
 * Created by Lakmal on 9/4/16.
 */
public class InvalidIntentType extends Exception {
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "No such intent registered in app. Please refer enum class.";
    }
}
